import express, { Request, Response, NextFunction } from "express";
import { createServer as createViteServer } from "vite";
import path from "path";
import { fileURLToPath } from "url";
import {
  Document,
  Packer,
  Paragraph,
  TextRun,
  HeadingLevel,
  AlignmentType,
  Table,
  TableRow,
  TableCell,
  WidthType,
  BorderStyle,
  TableLayoutType,
} from "docx";
import { GoogleGenAI } from "@google/genai";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/* ------------------------------ موجه النظام (SYSTEM_PROMPT) ------------------------------ */

const SYSTEM_PROMPT = `You are a high-fidelity document reconstruction agent. your top priority is VISUAL FIDELITY and LINGUISTIC INSIGHT.

Output JSON Format:
{
  "structure": Array<Paragraph | Table>,
  "insights": {
    "primaryLanguage": string,
    "secondaryLanguages": string[],
    "summary": string,
    "tone": string,
    "complexity": string
  }
}

Paragraph/Table schema must follow precisely to ensure Word compatibility.
1. FONT PRESERVATION: Identify EXACT font families (e.g., 'Amiri', 'Traditional Arabic', 'Sakkal Majalla', 'Calibri', 'Arial').
2. ARABIC/RTL: Set "rtl": true and "alignment": "right". Detect specific fonts. Preserve Arabic diacritics (Tashkeel) completely.
3. Return ONLY the raw JSON object. Do not include markdown code block syntax if possible.`;

/* ------------------------------ مساعدات معالجة النصوص والـ JSON ------------------------------ */

// تنظيف استجابة الـ JSON في حال تضمنت علامات الماركداون ```json ... ```
function cleanJsonOutput(raw: string): string {
  if (!raw) return "{}";
  let cleaned = raw.trim();
  if (cleaned.startsWith("```json")) {
    cleaned = cleaned.substring(7);
  } else if (cleaned.startsWith("```")) {
    cleaned = cleaned.substring(3);
  }
  if (cleaned.endsWith("```")) {
    cleaned = cleaned.substring(0, cleaned.length - 3);
  }
  return cleaned.trim();
}

// تنظيف اسم الملف ليكون آمناً في ترويسة Content-Disposition
function sanitizeFilename(name?: string): string {
  if (!name || typeof name !== "string") return "converted";
  return name.replace(/[^\w\s\u0600-\u06FF.-]/gi, "_").trim() || "converted";
}

/* ------------------------------ تشغيل الخادم ------------------------------ */

async function startServer() {
  const app = express();
  const PORT = process.env.PORT ? parseInt(process.env.PORT, 10) : 3000;

  const apiKey = process.env.GEMINI_API_KEY || "";
  if (!apiKey) {
    console.warn("⚠️ [تحذير]: لم يتم تعيين GEMINI_API_KEY في المتغيرات البيئية. وظائف الذكاء الاصطناعي ستتطلب إدخال المفتاح.");
  }
  const genAI = new GoogleGenAI({ apiKey });

  // معالجة الأجسام الكبيرة حتى 100MB
  app.use(express.json({ limit: "100mb" }));
  app.use(express.urlencoded({ extended: true, limit: "100mb" }));

  // إعدادات CORS لدعم التطوير والربط مع مختلف الواجهات
  app.use((req: Request, res: Response, next: NextFunction) => {
    res.header("Access-Control-Allow-Origin", "*");
    res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization");
    if (req.method === "OPTIONS") {
      return res.sendStatus(200);
    }
    next();
  });

  // تسجيل الطلبات للمراقبة واستكشاف الأخطاء
  app.use((req: Request, res: Response, next: NextFunction) => {
    if (req.path.startsWith("/api")) {
      const start = Date.now();
      res.on("finish", () => {
        const duration = Date.now() - start;
        console.log(`[API] ${req.method} ${req.path} -> ${res.statusCode} (${duration}ms)`);
      });
    }
    next();
  });

  /* ------------------------------ دالة بناء الفقرة (createParagraph) ------------------------------ */

  const createParagraph = (p: any): Paragraph => {
    const isRtl = !!p.rtl;

    // تحديد محاذاة النص
    const alignment =
      p.alignment === "center"
        ? AlignmentType.CENTER
        : p.alignment === "right" || isRtl
        ? AlignmentType.RIGHT
        : p.alignment === "both" || p.alignment === "justify"
        ? AlignmentType.JUSTIFIED
        : AlignmentType.LEFT;

    // تحديد مستوى العنوان
    const headingLevel =
      p.level === 1
        ? HeadingLevel.HEADING_1
        : p.level === 2
        ? HeadingLevel.HEADING_2
        : p.level === 3
        ? HeadingLevel.HEADING_3
        : p.level === 4
        ? HeadingLevel.HEADING_4
        : undefined;

    return new Paragraph({
      heading: headingLevel,
      alignment: alignment,
      bidirectional: isRtl,
      spacing: p.spacing
        ? {
            before: typeof p.spacing.before === "number" ? Math.round(p.spacing.before * 20) : undefined,
            after: typeof p.spacing.after === "number" ? Math.round(p.spacing.after * 20) : undefined,
            line: typeof p.spacing.line === "number" ? Math.round(p.spacing.line * 240) : undefined,
          }
        : undefined,
      indent: p.indent
        ? {
            left: typeof p.indent.left === "number" ? Math.round(p.indent.left * 20) : undefined,
            firstLine: typeof p.indent.firstLine === "number" ? Math.round(p.indent.firstLine * 20) : undefined,
          }
        : undefined,
      children: (p.runs || []).map((run: any) => {
        const detectedFont = run.fontFamily?.trim();
        const fallbackFont = isRtl ? "Traditional Arabic" : "Calibri";
        const chosenFont = detectedFont || fallbackFont;

        const fontSize = typeof run.fontSize === "number" ? run.fontSize * 2 : 24;

        return new TextRun({
          text: String(run.text || ""),
          bold: !!run.bold,
          italics: !!run.italic,
          underline: run.underline ? {} : undefined,
          size: fontSize,
          font: {
            ascii: chosenFont,
            cs: chosenFont,
            hAnsi: chosenFont,
            eastAsia: chosenFont,
          },
          color: run.color ? String(run.color).replace("#", "") : undefined,
          rightToLeft: isRtl,
          sizeComplexScript: fontSize,
          boldComplexScript: !!run.bold,
          italicsComplexScript: !!run.italic,
        });
      }),
    });
  };

  /* ------------------------------ فحص صحة الخادم (Health Check) ------------------------------ */

  app.get("/api/health", (_req: Request, res: Response) => {
    res.json({
      status: "ok",
      serverTime: new Date().toISOString(),
      geminiConfigured: !!process.env.GEMINI_API_KEY,
    });
  });

  /* ------------------------------ نقطة النهاية: /api/process-pdf ------------------------------ */

  app.post("/api/process-pdf", async (req: Request, res: Response) => {
    try {
      let { base64 } = req.body;
      if (!base64 || typeof base64 !== "string") {
        return res.status(400).json({
          error: "Missing base64 data",
          message: "يرجى تقديم بيانات الملف مشفرة بتنسيق base64 صالح في جسم الطلب.",
        });
      }

      // تنظيف بادئة data URI إذا كانت موجودة (مثل data:application/pdf;base64,...)
      if (base64.includes(",")) {
        base64 = base64.split(",")[1];
      }

      const activeApiKey = process.env.GEMINI_API_KEY || "";
      if (!activeApiKey) {
        return res.status(500).json({
          error: "API Key Not Configured",
          message: "مفتاح GEMINI_API_KEY غير متوفر في بيئة الخادم. يرجى ضبط المتغير البيئي.",
        });
      }

      console.log(`[Gemini] Processing PDF (base64 size: ~${Math.round(base64.length / 1024)} KB)...`);

      // إرسال الطلب إلى نموذج gemini-2.0-flash مع خيارات JSON الدقيقة
      const response = await genAI.models.generateContent({
        model: "gemini-2.0-flash",
        contents: [
          {
            role: "user",
            parts: [
              { text: SYSTEM_PROMPT },
              { text: "Convert this PDF to its structured representation for Word document generation." },
              { inlineData: { data: base64, mimeType: "application/pdf" } },
            ],
          },
        ],
        config: {
          responseMimeType: "application/json",
          temperature: 0.1, // لضمان أقصى قدر من الدقة والالتزام بالتنسيق
        },
      });

      const rawText = response.text || "";
      const cleaned = cleanJsonOutput(rawText);

      // محاولة تحليل الـ JSON للتأكد من سلامته وتقديم استجابة غنية
      let parsedJson: any = null;
      try {
        parsedJson = JSON.parse(cleaned);
      } catch (parseErr) {
        console.warn("[Gemini] JSON parse warning, returning raw text fallback:", parseErr);
      }

      // إرجاع النتيجة متوافقة مع الكود القديم (result) ومع أي عميل حديث (data, insights)
      return res.json({
        result: rawText,
        data: parsedJson,
        structure: parsedJson?.structure || [],
        insights: parsedJson?.insights || null,
      });
    } catch (error: any) {
      console.error("[Gemini Error]:", error);
      const isRateLimit = error?.message?.includes("429") || error?.status === 429;
      const isQuota = error?.message?.includes("quota");

      return res.status(500).json({
        error: "AI processing failed",
        message: isRateLimit || isQuota
          ? "تم تجاوز الحد المسموح به لطلبات الذكاء الاصطناعي، يرجى المحاولة بعد لحظات."
          : error?.message || "حدث خطأ غير متوقع أثناء معالجة المستند عبر الذكاء الاصطناعي.",
        details: process.env.NODE_ENV !== "production" ? error?.stack : undefined,
      });
    }
  });

  /* ------------------------------ نقطة النهاية: /api/generate-docx ------------------------------ */

  app.post("/api/generate-docx", async (req: Request, res: Response) => {
    try {
      const { filename, content } = req.body;

      if (!content || !Array.isArray(content)) {
        return res.status(400).json({
          error: "Invalid content format",
          message: "يجب أن يكون حقل 'content' مصفوفة صالحة تحتوي على عناصر الفقرات والجداول.",
        });
      }

      console.log(`[DOCX] Generating document "${filename || "converted"}" with ${content.length} blocks...`);

      const doc = new Document({
        sections: [
          {
            properties: {
              page: {
                margin: {
                  top: 1440, // 1 inch
                  bottom: 1440,
                  left: 1440,
                  right: 1440,
                },
              },
            },
            children: content.map((item: any, index: number) => {
              try {
                if (item && item.type === "table") {
                  return new Table({
                    width: { size: 100, type: WidthType.PERCENTAGE },
                    layout: TableLayoutType.AUTOFIT,
                    rows: (item.rows || []).map((row: any) =>
                      new TableRow({
                        children: (row.cells || []).map((cell: any) =>
                          new TableCell({
                            width: cell.width ? { size: cell.width, type: WidthType.PERCENTAGE } : undefined,
                            children: (cell.paragraphs || []).map((p: any) => createParagraph(p)),
                          })
                        ),
                      })
                    ),
                  });
                }
                return createParagraph(item || {});
              } catch (itemErr) {
                console.warn(`[DOCX] Warning: Error building item at index ${index}, falling back to plain paragraph:`, itemErr);
                return new Paragraph({
                  children: [new TextRun({ text: item?.text || "" })],
                });
              }
            }),
          },
        ],
      });

      const buffer = await Packer.toBuffer(doc);
      const safeName = sanitizeFilename(filename);

      res.setHeader("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
      res.setHeader("Content-Disposition", `attachment; filename="${encodeURIComponent(safeName)}.docx"`);
      res.setHeader("Content-Length", buffer.length.toString());

      return res.send(buffer);
    } catch (error: any) {
      console.error("[DOCX Generation Error]:", error);
      return res.status(500).json({
        error: "Failed to generate Word document",
        message: error?.message || "تعذر إنشاء ملف الـ Word. يرجى التأكد من توافق بنية العناصر المرسلة.",
        details: process.env.NODE_ENV !== "production" ? error?.stack : undefined,
      });
    }
  });

  /* ------------------------------ إعدادات Vite للواجهة الأمامية ------------------------------ */

  if (process.env.NODE_ENV !== "production") {
    try {
      const vite = await createViteServer({
        server: { middlewareMode: true },
        appType: "spa",
      });
      app.use(vite.middlewares);
    } catch (viteErr) {
      console.warn("[Vite Middleware]: Could not load Vite middleware:", viteErr);
    }
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (_req: Request, res: Response) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  /* ------------------------------ معالجة الأخطاء الشاملة (Global Error Handler) ------------------------------ */

  app.use((err: any, _req: Request, res: Response, _next: NextFunction) => {
    console.error("[Server Unhandled Error]:", err);
    res.status(500).json({
      error: "Internal Server Error",
      message: err?.message || "حدث خطأ داخلي في الخادم.",
    });
  });

  const server = app.listen(PORT, "0.0.0.0", () => {
    console.log(`🚀 [خادم كاتب DOCX] يعمل بنجاح على: http://localhost:${PORT}`);
  });

  // إغلاق نظيف للخادم عند استلام إشارات الإنهاء
  const shutdown = () => {
    console.log("[Server] Closing HTTP server gracefully...");
    server.close(() => {
      console.log("[Server] Closed.");
      process.exit(0);
    });
  };

  process.on("SIGTERM", shutdown);
  process.on("SIGINT", shutdown);
}

startServer();
