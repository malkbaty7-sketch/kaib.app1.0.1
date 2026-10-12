package com.example.util

import android.content.Context
import com.example.data.model.ChapterItem
import com.example.data.model.CustomFontEntity
import com.example.data.model.ExportAdvancedMetadata
import com.example.data.model.Project
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object EpubExporter {

    fun exportProjectToEpub(
        context: Context,
        project: Project,
        chapters: List<ChapterItem>,
        metadata: ExportAdvancedMetadata = ExportAdvancedMetadata(),
        customFonts: List<CustomFontEntity> = emptyList()
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeFileName = project.projectName.replace(Regex("""[^\w\u0600-\u06FF]"""), "_")
        val outputFile = File(exportDir, "${safeFileName}_${System.currentTimeMillis()}.epub")

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            // 1. mimetype (MUST BE uncompressed / STORED and first entry in EPUB spec)
            val mimetypeBytes = "application/epub+zip".toByteArray(StandardCharsets.US_ASCII)
            val mimetypeEntry = ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = mimetypeBytes.size.toLong()
                compressedSize = mimetypeBytes.size.toLong()
                crc = CRC32().apply { update(mimetypeBytes) }.value
            }
            zos.putNextEntry(mimetypeEntry)
            zos.write(mimetypeBytes)
            zos.closeEntry()

            // 2. META-INF/container.xml
            val containerXml = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>"""
            addDeflatedEntry(zos, "META-INF/container.xml", containerXml)

            // 3. Embedded Fonts
            val embeddedFontItems = mutableListOf<Pair<String, String>>() // (id, href)
            val fontFaceCss = StringBuilder()

            customFonts.forEachIndexed { fIdx, fontEntity ->
                val fontFile = File(fontEntity.filePath)
                if (fontFile.exists() && fontFile.canRead()) {
                    val fontZipPath = "OEBPS/fonts/${fontFile.name}"
                    addFileEntry(zos, fontZipPath, fontFile)
                    val fontId = "font_$fIdx"
                    embeddedFontItems.add(Pair(fontId, "fonts/${fontFile.name}"))

                    fontFaceCss.append("""
@font-face {
    font-family: '${fontEntity.fontName}';
    src: url('fonts/${fontFile.name}');
}
""")
                }
            }

            // 4. OEBPS/style.css with embedded font-family and Arabic RTL styling
            val primaryFontFamily = if (customFonts.isNotEmpty()) "'${customFonts.first().fontName}', 'Tajawal', sans-serif" else "'Tajawal', 'Traditional Arabic', 'Amiri', sans-serif"
            val css = """
@charset "utf-8";
$fontFaceCss
body {
    direction: rtl;
    text-align: justify;
    font-family: $primaryFontFamily;
    margin: 5%;
    line-height: 1.8;
    color: #222222;
    background-color: #FFFFFF;
}
h1 {
    text-align: center;
    color: #D2691E;
    margin-top: 1.5em;
    margin-bottom: 0.8em;
    font-size: 1.8em;
    border-bottom: 2px solid #D2691E;
    padding-bottom: 0.3em;
}
h2 {
    color: #8B4513;
    font-size: 1.4em;
    margin-top: 1.2em;
}
p {
    margin-bottom: 1em;
    text-indent: 1.5em;
}
.cover-title {
    font-size: 2.2em;
    font-weight: bold;
    color: #1A1A1A;
    margin-top: 2em;
    text-align: center;
}
.cover-meta {
    font-size: 1.1em;
    color: #666666;
    margin-top: 1em;
    text-align: center;
}
.cover-image-container {
    text-align: center;
    margin: 2em 0;
}
.cover-image {
    max-width: 90%;
    max-height: 400px;
    border-radius: 8px;
    box-shadow: 0 4px 10px rgba(0,0,0,0.15);
}
.decorative-divider {
    text-align: center;
    color: #D2691E;
    margin: 2em 0;
    font-size: 1.3em;
}
"""
            addDeflatedEntry(zos, "OEBPS/style.css", css)

            // 5. Embedded Cover Image if available
            var hasCoverImage = false
            var coverImageFileName = ""
            if (!project.coverImagePath.isNullOrBlank()) {
                val coverFile = File(project.coverImagePath)
                if (coverFile.exists() && coverFile.canRead()) {
                    coverImageFileName = coverFile.name
                    addFileEntry(zos, "OEBPS/images/$coverImageFileName", coverFile)
                    hasCoverImage = true
                }
            }

            // 6. OEBPS/cover.xhtml
            val coverImgHtml = if (hasCoverImage) """<div class="cover-image-container"><img class="cover-image" src="images/$coverImageFileName" alt="غلاف الكتاب"/></div>""" else ""
            val coverXhtml = """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" dir="rtl" lang="${metadata.language}">
<head>
    <title>${escapeXml(project.projectName)}</title>
    <link rel="stylesheet" type="text/css" href="style.css"/>
</head>
<body>
    <div class="cover-title">${escapeXml(project.projectName)}</div>
    $coverImgHtml
    <div class="decorative-divider">❖ ❖ ❖</div>
    <div class="cover-meta">الناشر: ${escapeXml(metadata.publisher)}</div>
    <div class="cover-meta">الرقم الدولي المعياري (ISBN): ${escapeXml(metadata.isbn)}</div>
    <div class="cover-meta">${escapeXml(metadata.rights)}</div>
    <div class="cover-meta">تاريخ النشر: ${escapeXml(metadata.publicationYear)}</div>
</body>
</html>"""
            addDeflatedEntry(zos, "OEBPS/cover.xhtml", coverXhtml)

            // 7. OEBPS/toc.xhtml (Table of Contents)
            val tocSb = StringBuilder()
            tocSb.append("""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" dir="rtl" lang="${metadata.language}">
<head>
    <title>فهرس المحتويات</title>
    <link rel="stylesheet" type="text/css" href="style.css"/>
</head>
<body>
    <h1>فهرس المحتويات</h1>
    <nav epub:type="toc" id="toc">
        <ol>
""")
            chapters.forEachIndexed { idx, ch ->
                tocSb.append("            <li><a href=\"chapter_${idx + 1}.xhtml\">${escapeXml(ch.title)}</a></li>\n")
            }
            tocSb.append("""        </ol>
    </nav>
    <nav epub:type="landmarks" hidden="">
        <ol>
            <li><a epub:type="cover" href="cover.xhtml">الغلاف</a></li>
            <li><a epub:type="toc" href="toc.xhtml">الفهرس</a></li>
            <li><a epub:type="bodymatter" href="chapter_1.xhtml">بداية المحتوى</a></li>
        </ol>
    </nav>
</body>
</html>""")
            addDeflatedEntry(zos, "OEBPS/toc.xhtml", tocSb.toString())

            // 8. Chapter files
            chapters.forEachIndexed { idx, ch ->
                val chNum = idx + 1
                val chSb = StringBuilder()
                chSb.append("""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" dir="rtl" lang="${metadata.language}">
<head>
    <title>${escapeXml(ch.title)}</title>
    <link rel="stylesheet" type="text/css" href="style.css"/>
</head>
<body>
    <h1>${escapeXml(ch.title)}</h1>
""")
                val paras = ch.content.split("\n\n")
                for (p in paras) {
                    if (p.isNotBlank()) {
                        chSb.append("    <p>${escapeXml(p.trim())}</p>\n")
                    }
                }
                chSb.append("""</body>
</html>""")
                addDeflatedEntry(zos, "OEBPS/chapter_$chNum.xhtml", chSb.toString())
            }

            // 9. OEBPS/toc.ncx
            val ncxSb = StringBuilder()
            ncxSb.append("""<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
  <head>
    <meta name="dtb:uid" content="urn:isbn:${metadata.isbn}"/>
    <meta name="dtb:depth" content="1"/>
    <meta name="dtb:totalPageCount" content="0"/>
    <meta name="dtb:maxPageNumber" content="0"/>
  </head>
  <docTitle><text>${escapeXml(project.projectName)}</text></docTitle>
  <navMap>
    <navPoint id="navpoint-cover" playOrder="1">
      <navLabel><text>الغلاف</text></navLabel>
      <content src="cover.xhtml"/>
    </navPoint>
    <navPoint id="navpoint-toc" playOrder="2">
      <navLabel><text>الفهرس</text></navLabel>
      <content src="toc.xhtml"/>
    </navPoint>
""")
            chapters.forEachIndexed { idx, ch ->
                val order = idx + 3
                ncxSb.append("""    <navPoint id="navpoint-${idx + 1}" playOrder="$order">
      <navLabel><text>${escapeXml(ch.title)}</text></navLabel>
      <content src="chapter_${idx + 1}.xhtml"/>
    </navPoint>
""")
            }
            ncxSb.append("""  </navMap>
</ncx>""")
            addDeflatedEntry(zos, "OEBPS/toc.ncx", ncxSb.toString())

            // 10. OEBPS/content.opf with Full Dublin Core Metadata
            val opfSb = StringBuilder()
            opfSb.append("""<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="pub-id" dir="rtl" prefix="rendition: http://www.idpf.org/vocab/rendition/#">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="pub-id">urn:isbn:${escapeXml(metadata.isbn)}</dc:identifier>
    <dc:title>${escapeXml(project.projectName)}</dc:title>
    <dc:language>${metadata.language}</dc:language>
    <dc:publisher>${escapeXml(metadata.publisher)}</dc:publisher>
    <dc:rights>${escapeXml(metadata.rights)}</dc:rights>
    <dc:creator>تطبيق كاتب - منصة النشر الاحترافية</dc:creator>
    <dc:date>${metadata.publicationYear}-01-01</dc:date>
    <meta property="dcterms:modified">${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())}</meta>
""")
            if (hasCoverImage) {
                opfSb.append("""    <meta name="cover" content="cover-img-item"/>\n""")
            }
            opfSb.append("""  </metadata>
  <manifest>
    <item id="css" href="style.css" media-type="text/css"/>
    <item id="toc_ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
    <item id="toc" href="toc.xhtml" media-type="application/xhtml+xml" properties="nav"/>
    <item id="cover" href="cover.xhtml" media-type="application/xhtml+xml"/>
""")
            if (hasCoverImage) {
                opfSb.append("""    <item id="cover-img-item" href="images/$coverImageFileName" media-type="image/jpeg" properties="cover-image"/>\n""")
            }
            embeddedFontItems.forEach { (fid, href) ->
                opfSb.append("""    <item id="$fid" href="$href" media-type="font/ttf"/>\n""")
            }
            chapters.forEachIndexed { idx, _ ->
                opfSb.append("    <item id=\"ch_${idx + 1}\" href=\"chapter_${idx + 1}.xhtml\" media-type=\"application/xhtml+xml\"/>\n")
            }
            opfSb.append("""  </manifest>
  <spine toc="toc_ncx">
    <itemref idref="cover"/>
    <itemref idref="toc"/>
""")
            chapters.forEachIndexed { idx, _ ->
                opfSb.append("    <itemref idref=\"ch_${idx + 1}\"/>\n")
            }
            opfSb.append("""  </spine>
</package>""")
            addDeflatedEntry(zos, "OEBPS/content.opf", opfSb.toString())
        }

        return outputFile
    }

    private fun addDeflatedEntry(zos: ZipOutputStream, name: String, content: String) {
        val entry = ZipEntry(name)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }

    private fun addFileEntry(zos: ZipOutputStream, zipPath: String, file: File) {
        val entry = ZipEntry(zipPath)
        zos.putNextEntry(entry)
        FileInputStream(file).use { fis ->
            fis.copyTo(zos)
        }
        zos.closeEntry()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
