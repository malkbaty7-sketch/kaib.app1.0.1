import React, { useState, useRef, useCallback } from "react";
import * as pdfjsLib from "pdfjs-dist";
import mammoth from "mammoth";
import { Document, Packer, Paragraph, TextRun } from "docx";
import { GoogleGenAI } from "@google/genai";

// إعداد worker الخاص بـ pdfjs-dist
declare const process: { env: Record<string, string | undefined> };

if (typeof window !== "undefined" && (pdfjsLib as any).GlobalWorkerOptions) {
  (pdfjsLib as any).GlobalWorkerOptions.workerSrc =
    `https://cdn.jsdelivr.net/npm/pdfjs-dist@${(pdfjsLib as any).version || "3.11.174"}/build/pdf.worker.min.mjs`;
}

/* ------------------------------ نماذج البيانات (Models & Types) ------------------------------ */

export interface LinguisticInsights {
  wordCount: number;
  characterCount: number;
  paragraphCount: number;
  tashkeelCount: number;
  tashkeelDensity: number; // نسبة التشكيل المئوية
  rtlRatio: number; // نسبة الحروف العربية/RTL
  detectedLanguage: string;
  hasDiacritics: boolean;
  preservedLines: number;
}

export interface ConversionFile {
  id: string;
  file: File;
  name: string;
  size: number;
  targetFormat: "docx" | "pdf" | "html" | "txt";
  mode: "auto" | "ai" | "backend";
  keepLines: boolean;
  status: "idle" | "processing" | "completed" | "error";
  progress: number;
  statusMessage?: string;
  errorMessage?: string;
  previewText?: string;
  downloadUrl?: string;
  resultBlob?: Blob;
  insights?: LinguisticInsights;
}

/* ------------------------------ أدوات معالجة اللغة العربية ------------------------------ */

const ARABIC_REGEX = /[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]/g;
const PRESENTATION_FORMS = /[\uFB50-\uFDFF\uFE70-\uFEFF]/;
const TASHKEEL_REGEX = /[\u0617-\u061A\u064B-\u0652\u0670\u06D6-\u06ED]/g;

// تحويل أشكال العرض التقديمية إلى حروف أساسية مع صيانة التشكيل
export const fixAr = (s: string): string =>
  PRESENTATION_FORMS.test(s) ? s.normalize("NFKC") : s;

export const isRtl = (s: string): boolean => {
  const arabicMatches = (s.match(ARABIC_REGEX) || []).length;
  const letterMatches = (s.match(/\p{L}/gu) || []).length || 1;
  return arabicMatches / letterMatches > 0.4;
};

export const escapeHtml = (s: string): string =>
  s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

// استخراج التحليلات اللغوية والتشكيلية للنص
export const analyzeArabicText = (text: string, paras: string[]): LinguisticInsights => {
  const words = text.trim() ? text.trim().split(/\s+/) : [];
  const chars = text.replace(/\s+/g, "");
  const tashkeel = (text.match(TASHKEEL_REGEX) || []).length;
  const arabicChars = (text.match(ARABIC_REGEX) || []).length;
  const totalLetters = (text.match(/\p{L}/gu) || []).length || 1;

  const tashkeelDensity = words.length > 0 ? Math.round((tashkeel / words.length) * 100) : 0;
  const rtlRatio = Math.round((arabicChars / totalLetters) * 100);

  return {
    wordCount: words.length,
    characterCount: chars.length,
    paragraphCount: paras.length,
    tashkeelCount: tashkeel,
    tashkeelDensity,
    rtlRatio,
    detectedLanguage: rtlRatio > 50 ? "العربية (RTL)" : "نص أجنبي / لاتيني (LTR)",
    hasDiacritics: tashkeel > 0,
    preservedLines: text.split("\n").length,
  };
};

/* ------------------------------ محرك استخراج النصوص (Extraction Engine) ------------------------------ */

export async function pdfText(file: File, keepLines: boolean): Promise<string[]> {
  const arrayBuffer = await file.arrayBuffer();
  const pdf = await (pdfjsLib as any).getDocument({ data: arrayBuffer }).promise;
  const out: string[] = [];

  for (let p = 1; p <= pdf.numPages; p++) {
    const page = await pdf.getPage(p);
    const content = await page.getTextContent();
    const items = (content.items as any[]).filter(
      (i) => typeof i.str === "string" && i.str.trim() !== ""
    );

    const lines: { y: number; h: number; items: any[] }[] = [];
    for (const it of items) {
      const y = it.transform[5];
      const h = it.height || 10;
      const line = lines.find((l) => Math.abs(l.y - y) < Math.max(2, h * 0.5));
      if (line) line.items.push(it);
      else lines.push({ y, h, items: [it] });
    }

    lines.sort((a, b) => b.y - a.y);
    let buf: string[] = [];
    let prevY: number | null = null;

    for (const l of lines) {
      const joined = l.items.map((i) => i.str).join("");
      const rtl = isRtl(joined);
      l.items.sort((a, b) =>
        rtl ? b.transform[4] - a.transform[4] : a.transform[4] - b.transform[4]
      );
      const text = fixAr(l.items.map((i) => i.str).join(" ").replace(/\s+/g, " ").trim());

      if (keepLines) {
        out.push(text);
      } else {
        if (prevY !== null && prevY - l.y > l.h * 1.7 && buf.length) {
          out.push(buf.join(" "));
          buf = [];
        }
        buf.push(text);
        prevY = l.y;
      }
    }
    if (buf.length) out.push(buf.join(" "));
  }
  return out.filter(Boolean);
}

export const toBase64 = (file: File): Promise<string> =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result).split(",")[1]);
    reader.onerror = () => reject(new Error("تعذر قراءة الملف كـ Base64"));
    reader.readAsDataURL(file);
  });

const PROMPT_TASHKEEL =
  "انسخ كل النص الموجود في هذا المستند كما هو بالضبط. حافظ على كل التشكيل العربي " +
  "(الفتحة والضمة والكسرة والشدة والسكون والتنوين) وعلامات الترقيم والأرقام وترتيب الأسطر والفقرات. " +
  "لا تترجم ولا تصحح ولا تلخص ولا تضف أي تعليق. افصل الفقرات بسطر فارغ. أخرج النص فقط.";

export async function geminiExtract(file: File): Promise<string> {
  const apiKey =
    process.env.GEMINI_API_KEY ||
    (typeof window !== "undefined" && (window as any).GEMINI_API_KEY) ||
    "";
  if (!apiKey) {
    throw new Error("مفتاح GEMINI_API_KEY غير متوفر. يرجى تزويده في المتغيرات البيئية.");
  }
  const ai = new GoogleGenAI({ apiKey });
  const data = await toBase64(file);
  const mimeType =
    file.type || (file.name.toLowerCase().endsWith(".pdf") ? "application/pdf" : "image/png");

  const res = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        role: "user",
        parts: [{ inlineData: { mimeType, data } }, { text: PROMPT_TASHKEEL }],
      },
    ],
  });
  return res.text ?? "";
}

/* ------------------------------ بناء المستندات وتنزيلها ------------------------------ */

export const htmlFromParas = (paras: string[]): string =>
  paras.map((p) => `<p>${escapeHtml(p)}</p>`).join("\n");

export const wrapHtml = (body: string, title: string): string =>
  `<!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8"><title>${escapeHtml(
    title
  )}</title>
<link href="https://fonts.googleapis.com/css2?family=Amiri:wght@400;700&display=swap" rel="stylesheet">
<style>
body{font-family:'Amiri','Traditional Arabic','Noto Naskh Arabic',serif;font-size:18px;line-height:2;max-width:850px;margin:30px auto;padding:0 24px;color:#1e293b;background:#fff}
p,li,td,th,h1,h2,h3,h4,h5,h6{unicode-bidi:plaintext;text-align:start}
p{margin:0 0 .7em} table{border-collapse:collapse;width:100%;margin:1.2em 0} td,th{border:1px solid #cbd5e1;padding:8px}
img{max-width:100%;border-radius:8px}
@media print{body{margin:0;max-width:none;padding:10mm}}
</style></head><body>${body}</body></html>`;

export async function makeDocx(paras: string[]): Promise<Blob> {
  const children = paras.map((t) => {
    const rtl = isRtl(t);
    return new Paragraph({
      bidirectional: rtl,
      spacing: { after: 120, line: 360 },
      children: [
        new TextRun({
          text: t,
          rightToLeft: rtl,
          font: { ascii: "Arial", hAnsi: "Arial", cs: "Arial" },
          size: 28,
          sizeComplexScript: 28,
        }),
      ],
    });
  });
  return Packer.toBlob(new Document({ sections: [{ children }] }));
}

export function downloadBlob(blob: Blob, name: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  setTimeout(() => {
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }, 2000);
}

export function printHtml(html: string) {
  const iframe = document.createElement("iframe");
  iframe.style.cssText = "position:fixed;right:0;bottom:0;width:0;height:0;border:0";
  document.body.appendChild(iframe);
  iframe.onload = () =>
    setTimeout(() => {
      iframe.contentWindow?.focus();
      iframe.contentWindow?.print();
      setTimeout(() => iframe.remove(), 60000);
    }, 800);
  iframe.srcdoc = html;
}

/* ------------------------------ واجهة المستخدم الرئيسية (App Component) ------------------------------ */

export default function App() {
  // طابور المعالجة بالدفعات (Batch Queue)
  const [queue, setQueue] = useState<ConversionFile[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const [selectedFileForInsights, setSelectedFileForInsights] = useState<ConversionFile | null>(null);
  const [globalTarget, setGlobalTarget] = useState<"docx" | "pdf" | "html" | "txt">("docx");
  const [globalMode, setGlobalMode] = useState<"auto" | "ai" | "backend">("auto");
  const [globalKeepLines, setGlobalKeepLines] = useState<boolean>(true);
  const [isProcessingQueue, setIsProcessingQueue] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  // تنسيق حجم الملف
  const formatSize = (bytes: number) => {
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
    return (bytes / (1024 * 1024)).toFixed(1) + " MB";
  };

  // إضافة ملفات جديدة إلى الطابور
  const addFilesToQueue = useCallback(
    (files: FileList | File[]) => {
      const newItems: ConversionFile[] = Array.from(files).map((file) => ({
        id: Math.random().toString(36).substring(2, 9) + Date.now().toString(36),
        file,
        name: file.name,
        size: file.size,
        targetFormat: globalTarget,
        mode: globalMode,
        keepLines: globalKeepLines,
        status: "idle",
        progress: 0,
      }));
      setQueue((prev) => [...prev, ...newItems]);
    },
    [globalTarget, globalMode, globalKeepLines]
  );

  // إزالة ملف من الطابور (removeFile)
  const removeFile = (id: string) => {
    setQueue((prev) => prev.filter((item) => item.id !== id));
    if (selectedFileForInsights?.id === id) {
      setSelectedFileForInsights(null);
    }
  };

  // معالجة ملف فردي (processFile)
  const processFile = async (id: string) => {
    const item = queue.find((f) => f.id === id);
    if (!item || item.status === "processing") return;

    // تحديث حالة البداية
    setQueue((prev) =>
      prev.map((f) =>
        f.id === id
          ? { ...f, status: "processing", progress: 15, statusMessage: "جاري تحليل الملف..." }
          : f
      )
    );

    try {
      const file = item.file;
      const baseName = file.name.replace(/\.[^.]+$/, "");
      const ext = file.name.split(".").pop()?.toLowerCase() || "";
      let paras: string[] = [];
      let srcHtml: string | null = null;

      const splitParas = (text: string) =>
        text
          .split(item.keepLines ? /\n+/ : /\n\s*\n/)
          .map((s) => fixAr(s.trim()))
          .filter(Boolean);

      // 1. محاولة استخدام الـ Backend أولاً إذا كان الوضع "backend"
      if (item.mode === "backend") {
        setQueue((prev) =>
          prev.map((f) =>
            f.id === id ? { ...f, progress: 40, statusMessage: "جاري الإرسال إلى /api/process-pdf..." } : f
          )
        );

        const formData = new FormData();
        formData.append("file", file);
        formData.append("targetFormat", item.targetFormat);
        formData.append("keepLines", String(item.keepLines));

        const res = await fetch("/api/process-pdf", {
          method: "POST",
          body: formData,
        });

        if (res.ok) {
          const data = await res.json();
          paras = data.paragraphs || splitParas(data.text || "");
        } else {
          throw new Error("فشل طلب /api/process-pdf في الواجهة الخلفية");
        }
      } else {
        // 2. المعالجة الذكية المحلية (Offline/Client-Side Engine)
        if (ext === "docx") {
          const buf = await file.arrayBuffer();
          srcHtml = (await mammoth.convertToHtml({ arrayBuffer: buf })).value;
          paras = splitParas((await mammoth.extractRawText({ arrayBuffer: buf })).value);
        } else if (ext === "txt") {
          paras = splitParas(await file.text());
        } else if (ext === "pdf") {
          if (item.mode === "auto") {
            setQueue((prev) =>
              prev.map((f) =>
                f.id === id
                  ? { ...f, progress: 35, statusMessage: "استخراج مباشر وترتيب RTL..." }
                  : f
              )
            );
            paras = await pdfText(file, item.keepLines);
          }
          if (item.mode === "ai" || paras.join("").length < 25) {
            setQueue((prev) =>
              prev.map((f) =>
                f.id === id
                  ? { ...f, progress: 50, statusMessage: "استخراج ذكي مع التشكيل (Gemini OCR)..." }
                  : f
              )
            );
            paras = splitParas(await geminiExtract(file));
          }
        } else if (file.type.startsWith("image/")) {
          setQueue((prev) =>
            prev.map((f) =>
              f.id === id
                ? { ...f, progress: 50, statusMessage: "قراءة الصورة والتشكيل عبر Gemini..." }
                : f
            )
          );
          paras = splitParas(await geminiExtract(file));
        } else {
          throw new Error("صيغة الملف غير مدعومة. الصيغ المدعومة: PDF, DOCX, TXT, صور.");
        }
      }

      if (!paras.length) {
        throw new Error("لم يتم العثور على أي نصوص قابلة للقراءة داخل هذا المستند.");
      }

      // بناء التحليلات اللغوية (Linguistic Insights)
      const fullText = paras.join("\n");
      const insights = analyzeArabicText(fullText, paras);

      // بناء الملف الناتج بناءً على الصيغة المطلوبة
      setQueue((prev) =>
        prev.map((f) =>
          f.id === id ? { ...f, progress: 80, statusMessage: `إنشاء ملف ${item.targetFormat.toUpperCase()}...` } : f
        )
      );

      let resultBlob: Blob;
      let downloadName = `${baseName}.${item.targetFormat}`;

      if (item.targetFormat === "docx") {
        resultBlob = await makeDocx(paras);
      } else if (item.targetFormat === "txt") {
        resultBlob = new Blob(["\ufeff" + paras.join("\n\n")], {
          type: "text/plain;charset=utf-8",
        });
      } else if (item.targetFormat === "html") {
        const fullHtml = wrapHtml(srcHtml ?? htmlFromParas(paras), baseName);
        resultBlob = new Blob([fullHtml], { type: "text/html;charset=utf-8" });
      } else {
        // PDF
        const fullHtml = wrapHtml(srcHtml ?? htmlFromParas(paras), baseName);
        resultBlob = new Blob([fullHtml], { type: "text/html;charset=utf-8" });
        downloadName = `${baseName}.html`;
      }

      const downloadUrl = URL.createObjectURL(resultBlob);

      // تحديث الحالة عند الاكتمال
      setQueue((prev) =>
        prev.map((f) =>
          f.id === id
            ? {
                ...f,
                status: "completed",
                progress: 100,
                statusMessage: "تم التحويل بنجاح ✔",
                previewText: fullText,
                resultBlob,
                downloadUrl,
                insights,
              }
            : f
        )
      );
    } catch (err: any) {
      setQueue((prev) =>
        prev.map((f) =>
          f.id === id
            ? {
                ...f,
                status: "error",
                progress: 0,
                errorMessage: err?.message || "حدث خطأ غير متوقع أثناء المعالجة",
                statusMessage: "تعذر التحويل ❌",
              }
            : f
        )
      );
    }
  };

  // معالجة كافة الملفات في الطابور
  const processAll = async () => {
    setIsProcessingQueue(true);
    const pendingItems = queue.filter((f) => f.status === "idle" || f.status === "error");
    for (const item of pendingItems) {
      await processFile(item.id);
    }
    setIsProcessingQueue(false);
  };

  // تنزيل ملف فردي
  const downloadSingle = (item: ConversionFile) => {
    if (item.resultBlob) {
      const ext = item.targetFormat === "pdf" ? "html" : item.targetFormat;
      const baseName = item.name.replace(/\.[^.]+$/, "");
      downloadBlob(item.resultBlob, `${baseName}.${ext}`);
    }
  };

  // أحداث السحب والإفلات (Drag & Drop)
  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      addFilesToQueue(e.dataTransfer.files);
    }
  };

  return (
    <div dir="rtl" className="min-h-screen bg-slate-50 text-slate-800 font-sans p-4 sm:p-8">
      <div className="max-w-5xl mx-auto space-y-8">
        {/* Header */}
        <header className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-blue-600 flex items-center justify-center text-white shadow-md shadow-blue-500/20 text-xl font-bold">
                ك
              </div>
              <div>
                <h1 className="text-2xl font-black text-slate-900 tracking-tight">محوّل المستندات العربي المتقدم</h1>
                <p className="text-sm text-slate-500">
                  تحويل ملفات PDF إلى Word (DOCX) ومستندات أخرى مع الحفاظ الدقيق على التشكيل واتجاه RTL
                </p>
              </div>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3 text-xs">
            <span className="px-3 py-1.5 rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-200 font-semibold flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              محرك تشكيل فائق الدقة (NFKC + OCR)
            </span>
            <span className="px-3 py-1.5 rounded-lg bg-blue-50 text-blue-700 border border-blue-200 font-semibold">
              يدعم Batch Processing
            </span>
          </div>
        </header>

        {/* Global Configuration Bar */}
        <div className="bg-white rounded-xl p-4 shadow-sm border border-slate-200 flex flex-wrap items-center justify-between gap-4 text-sm">
          <div className="flex flex-wrap items-center gap-6">
            <div className="flex items-center gap-2">
              <span className="font-semibold text-slate-600">صيغة الإخراج الافتراضية:</span>
              <select
                className="bg-slate-50 border border-slate-300 rounded-lg px-3 py-1.5 font-medium text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={globalTarget}
                onChange={(e) => setGlobalTarget(e.target.value as any)}
              >
                <option value="docx">Word (DOCX)</option>
                <option value="pdf">PDF (جاهز للطباعة)</option>
                <option value="html">HTML (خط أميري نقي)</option>
                <option value="txt">نص عادي (TXT مع التشكيل)</option>
              </select>
            </div>

            <div className="flex items-center gap-2">
              <span className="font-semibold text-slate-600">محرك المعالجة:</span>
              <select
                className="bg-slate-50 border border-slate-300 rounded-lg px-3 py-1.5 font-medium text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={globalMode}
                onChange={(e) => setGlobalMode(e.target.value as any)}
              >
                <option value="auto">تلقائي ذكي (مستخرج محلي + OCR عند الحاجة)</option>
                <option value="ai">ذكاء اصطناعي دائماً (Gemini OCR للتشكيل الدقيق)</option>
                <option value="backend">خادم الـ Backend (/api/process-pdf)</option>
              </select>
            </div>

            <label className="flex items-center gap-2 cursor-pointer select-none">
              <input
                type="checkbox"
                className="w-4 h-4 rounded text-blue-600 focus:ring-blue-500 border-slate-300"
                checked={globalKeepLines}
                onChange={(e) => setGlobalKeepLines(e.target.checked)}
              />
              <span className="text-slate-700 font-medium">الحفاظ على فواصل الأسطر الأصلية</span>
            </label>
          </div>

          {queue.length > 0 && (
            <button
              onClick={() => setQueue([])}
              className="text-red-500 hover:text-red-700 font-medium text-xs transition"
            >
              مسح الطابور بالكامل
            </button>
          )}
        </div>

        {/* Drag and Drop Zone */}
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          className={`relative border-2 border-dashed rounded-2xl p-10 text-center cursor-pointer transition-all duration-200 flex flex-col items-center justify-center gap-4 ${
            isDragging
              ? "border-blue-500 bg-blue-50/70 scale-[1.01] shadow-lg shadow-blue-500/10"
              : "border-slate-300 hover:border-blue-400 bg-white hover:bg-slate-50/60 shadow-sm"
          }`}
        >
          <input
            ref={fileInputRef}
            type="file"
            multiple
            accept=".pdf,.docx,.txt,image/*"
            className="hidden"
            onChange={(e) => {
              if (e.target.files) addFilesToQueue(e.target.files);
            }}
          />

          <div
            className={`w-16 h-16 rounded-2xl flex items-center justify-center transition-colors ${
              isDragging ? "bg-blue-600 text-white" : "bg-blue-100 text-blue-600"
            }`}
          >
            <svg
              className="w-8 h-8"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
              />
            </svg>
          </div>

          <div className="space-y-1">
            <p className="text-lg font-bold text-slate-800">
              اسحب وأفلت ملفاتك هنا، أو <span className="text-blue-600 underline">تصفح من جهازك</span>
            </p>
            <p className="text-xs text-slate-500">
              يدعم ملفات PDF، ومستندات Word (DOCX)، وملفات TXT، والصور الممسوحة ضوئياً (JPG, PNG)
            </p>
          </div>

          <div className="flex flex-wrap items-center justify-center gap-2 pt-2 text-xs font-semibold text-slate-600">
            <span className="px-2.5 py-1 bg-red-100 text-red-700 rounded-md">PDF</span>
            <span className="px-2.5 py-1 bg-blue-100 text-blue-700 rounded-md">DOCX</span>
            <span className="px-2.5 py-1 bg-emerald-100 text-emerald-700 rounded-md">TXT</span>
            <span className="px-2.5 py-1 bg-purple-100 text-purple-700 rounded-md">صور وOCR</span>
          </div>
        </div>

        {/* Batch Queue Section */}
        {queue.length > 0 && (
          <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 space-y-5">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-black text-slate-800">طابور الملفات (Batch Queue)</h2>
                <span className="px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-700 text-xs font-bold">
                  {queue.length} ملف
                </span>
              </div>

              <div className="flex items-center gap-3">
                <button
                  disabled={isProcessingQueue}
                  onClick={processAll}
                  className="px-5 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white text-sm font-bold shadow-md shadow-blue-500/20 transition flex items-center gap-2"
                >
                  {isProcessingQueue ? (
                    <>
                      <svg className="animate-spin w-4 h-4 text-white" viewBox="0 0 24 24" fill="none">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                      </svg>
                      جاري معالجة الطابور...
                    </>
                  ) : (
                    <>
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      تحويل الكل دفعة واحدة
                    </>
                  )}
                </button>
              </div>
            </div>

            {/* List of files in queue */}
            <div className="divide-y divide-slate-100">
              {queue.map((item) => (
                <div key={item.id} className="py-4 first:pt-0 last:pb-0 flex flex-col md:flex-row md:items-center justify-between gap-4">
                  <div className="flex items-start gap-3 flex-1 min-w-0">
                    <div className="w-10 h-10 rounded-xl bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-700 font-bold text-xs shrink-0">
                      {item.name.split(".").pop()?.toUpperCase() || "FILE"}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="font-bold text-slate-800 text-sm truncate">{item.name}</p>
                      <div className="flex flex-wrap items-center gap-3 text-xs text-slate-500 mt-1">
                        <span>{formatSize(item.size)}</span>
                        <span>•</span>
                        <span className="font-medium text-slate-700">الهدف: {item.targetFormat.toUpperCase()}</span>
                        {item.statusMessage && (
                          <>
                            <span>•</span>
                            <span className={item.status === "error" ? "text-red-500 font-medium" : "text-blue-600 font-medium"}>
                              {item.statusMessage}
                            </span>
                          </>
                        )}
                      </div>

                      {/* Progress Bar */}
                      {item.status === "processing" && (
                        <div className="w-full bg-slate-100 rounded-full h-1.5 mt-2 overflow-hidden">
                          <div
                            className="bg-blue-600 h-full rounded-full transition-all duration-300"
                            style={{ width: `${item.progress}%` }}
                          />
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Actions & Insights Trigger */}
                  <div className="flex items-center gap-2 shrink-0">
                    {item.status === "completed" && item.insights && (
                      <button
                        onClick={() => setSelectedFileForInsights(item)}
                        className="px-3 py-1.5 rounded-lg bg-indigo-50 hover:bg-indigo-100 text-indigo-700 border border-indigo-200 text-xs font-bold transition flex items-center gap-1.5"
                      >
                        <svg className="w-4 h-4 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                        </svg>
                        التحليل اللغوي
                      </button>
                    )}

                    {item.status === "completed" && (
                      <button
                        onClick={() => downloadSingle(item)}
                        className="px-3.5 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow-sm transition flex items-center gap-1"
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                        </svg>
                        تنزيل
                      </button>
                    )}

                    {item.status === "idle" && (
                      <button
                        onClick={() => processFile(item.id)}
                        className="px-3.5 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold transition"
                      >
                        تحويل
                      </button>
                    )}

                    <button
                      onClick={() => removeFile(item.id)}
                      className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-red-500 transition"
                      title="حذف من الطابور"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Linguistic Insights Modal / Drawer */}
        {selectedFileForInsights && selectedFileForInsights.insights && (
          <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 max-w-2xl w-full p-6 space-y-6 max-h-[90vh] flex flex-col animate-in fade-in zoom-in-95 duration-150">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-indigo-50 border border-indigo-200 flex items-center justify-center text-indigo-600 font-bold">
                    📊
                  </div>
                  <div>
                    <h3 className="text-lg font-black text-slate-800">تفاصيل التحليل اللغوي (Linguistic Insights)</h3>
                    <p className="text-xs text-slate-500">{selectedFileForInsights.name}</p>
                  </div>
                </div>
                <button
                  onClick={() => setSelectedFileForInsights(null)}
                  className="p-2 rounded-xl hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>

              {/* Insights Grid */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <div className="bg-slate-50 rounded-xl p-3 border border-slate-200 text-center">
                  <p className="text-xs text-slate-500 font-medium">عدد الكلمات</p>
                  <p className="text-lg font-black text-slate-800 mt-0.5">
                    {selectedFileForInsights.insights.wordCount.toLocaleString()}
                  </p>
                </div>
                <div className="bg-slate-50 rounded-xl p-3 border border-slate-200 text-center">
                  <p className="text-xs text-slate-500 font-medium">عدد الحروف</p>
                  <p className="text-lg font-black text-slate-800 mt-0.5">
                    {selectedFileForInsights.insights.characterCount.toLocaleString()}
                  </p>
                </div>
                <div className="bg-emerald-50 rounded-xl p-3 border border-emerald-200 text-center">
                  <p className="text-xs text-emerald-700 font-medium">علامات التشكيل</p>
                  <p className="text-lg font-black text-emerald-800 mt-0.5">
                    {selectedFileForInsights.insights.tashkeelCount.toLocaleString()}
                  </p>
                </div>
                <div className="bg-blue-50 rounded-xl p-3 border border-blue-200 text-center">
                  <p className="text-xs text-blue-700 font-medium">كثافة التشكيل</p>
                  <p className="text-lg font-black text-blue-800 mt-0.5">
                    {selectedFileForInsights.insights.tashkeelDensity}%
                  </p>
                </div>
              </div>

              <div className="bg-slate-50 rounded-xl p-4 border border-slate-200 space-y-2 text-xs">
                <div className="flex justify-between">
                  <span className="text-slate-500 font-medium">نسبة الأحرف العربية (RTL):</span>
                  <span className="font-bold text-slate-800">{selectedFileForInsights.insights.rtlRatio}%</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500 font-medium">تحديد اللغة والاتجاه:</span>
                  <span className="font-bold text-slate-800">{selectedFileForInsights.insights.detectedLanguage}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500 font-medium">عدد الفقرات المستخرجة:</span>
                  <span className="font-bold text-slate-800">{selectedFileForInsights.insights.paragraphCount} فقرة</span>
                </div>
              </div>

              {/* Text Preview Box */}
              <div className="space-y-2 flex-1 min-h-0 flex flex-col">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-700">معاينة النص المستخرج والتشكيل:</span>
                  <button
                    onClick={() => {
                      if (selectedFileForInsights.previewText) {
                        navigator.clipboard.writeText(selectedFileForInsights.previewText);
                      }
                    }}
                    className="text-xs text-blue-600 hover:underline font-semibold"
                  >
                    نسخ النص
                  </button>
                </div>
                <textarea
                  readOnly
                  dir="rtl"
                  className="w-full flex-1 min-h-[140px] p-3 rounded-xl bg-slate-50 border border-slate-200 font-mono text-sm leading-relaxed text-slate-800 focus:outline-none resize-none"
                  value={selectedFileForInsights.previewText || ""}
                />
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  onClick={() => downloadSingle(selectedFileForInsights)}
                  className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold shadow-md shadow-blue-500/20 transition"
                >
                  تنزيل المستند المحول
                </button>
                <button
                  onClick={() => setSelectedFileForInsights(null)}
                  className="px-4 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-sm font-semibold transition"
                >
                  إغلاق
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
