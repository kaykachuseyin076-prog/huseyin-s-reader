package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

object EpubHelper {
    private const val TAG = "EpubHelper"

    data class EpubMeta(
        val title: String?,
        val author: String?,
        val coverPath: String?,
        val totalChapters: Int,
        val spineHrefs: List<String>
    )

    data class ChapterParagraph(
        val index: Int,
        val text: String,
        val isHeading: Boolean = false
    )

    data class ChapterContent(
        val title: String?,
        val htmlContent: String,
        val plainText: String,
        val paragraphs: List<ChapterParagraph> = emptyList()
    )

    /**
     * Safely opens an InputStream for both content:// and file:// URI schemes.
     */
    fun openInputStream(context: Context, uri: Uri): InputStream? {
        return try {
            if (uri.scheme == "file" && uri.path != null) {
                File(uri.path!!).inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open input stream for $uri", e)
            null
        }
    }

    /**
     * Extracts metadata (title, author, cover, spine items) from an EPUB Uri.
     */
    fun extractMetaAndCover(context: Context, uri: Uri, bookId: String): EpubMeta {
        val safeId = bookId.hashCode().toString()
        val coversDir = File(context.cacheDir, "covers").apply { mkdirs() }
        val cachedCoverFile = File(coversDir, "epub_$safeId.jpg")

        var opfPath = ""
        var opfContent: String? = null
        val entries = mutableMapOf<String, ByteArray>()

        try {
            // First pass: locate container.xml and find opf path
            openInputStream(context, uri)?.use { inputStream ->
                val zis = ZipInputStream(inputStream)
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name.equals("META-INF/container.xml", ignoreCase = true)) {
                        val content = zis.readBytes().toString(Charsets.UTF_8)
                        opfPath = parseContainerForOpf(content)
                    }
                    entry = zis.nextEntry
                }
            }

            if (opfPath.isBlank()) {
                // Fallback: look for any .opf file in the zip
                openInputStream(context, uri)?.use { inputStream ->
                    val zis = ZipInputStream(inputStream)
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (entry.name.endsWith(".opf", ignoreCase = true)) {
                            opfPath = entry.name
                            opfContent = zis.readBytes().toString(Charsets.UTF_8)
                            break
                        }
                        entry = zis.nextEntry
                    }
                }
            } else {
                // Read opf content
                openInputStream(context, uri)?.use { inputStream ->
                    val zis = ZipInputStream(inputStream)
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (entry.name.equals(opfPath, ignoreCase = true)) {
                            opfContent = zis.readBytes().toString(Charsets.UTF_8)
                            break
                        }
                        entry = zis.nextEntry
                    }
                }
            }

            if (opfContent == null) {
                return EpubMeta(null, null, null, 0, emptyList())
            }

            // Parse OPF content
            val parsedOpf = parseOpf(opfContent!!, opfPath)

            // Extract cover image if available and not yet cached
            var finalCoverPath: String? = null
            if (cachedCoverFile.exists() && cachedCoverFile.length() > 0) {
                finalCoverPath = cachedCoverFile.absolutePath
            } else if (parsedOpf.coverHref != null) {
                val coverZipPath = resolvePath(opfPath, parsedOpf.coverHref)
                openInputStream(context, uri)?.use { inputStream ->
                    val zis = ZipInputStream(inputStream)
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (entry.name.equals(coverZipPath, ignoreCase = true) ||
                            entry.name.endsWith(parsedOpf.coverHref, ignoreCase = true)
                        ) {
                            val bytes = zis.readBytes()
                            if (bytes.isNotEmpty()) {
                                FileOutputStream(cachedCoverFile).use { fos ->
                                    fos.write(bytes)
                                }
                                finalCoverPath = cachedCoverFile.absolutePath
                            }
                            break
                        }
                        entry = zis.nextEntry
                    }
                }
            }

            return EpubMeta(
                title = parsedOpf.title,
                author = parsedOpf.author,
                coverPath = finalCoverPath,
                totalChapters = parsedOpf.spineHrefs.size.coerceAtLeast(1),
                spineHrefs = parsedOpf.spineHrefs
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract EPUB metadata for $uri", e)
            return EpubMeta(null, null, null, 0, emptyList())
        }
    }

    /**
     * Reads a chapter from the EPUB.
     */
    fun readChapter(context: Context, uri: Uri, chapterIndex: Int): ChapterContent {
        try {
            val meta = extractMetaAndCover(context, uri, uri.toString())
            if (meta.spineHrefs.isEmpty() || chapterIndex < 0 || chapterIndex >= meta.spineHrefs.size) {
                return ChapterContent("Hata", "<p>Bölüm bulunamadı.</p>", "Bölüm bulunamadı.")
            }

            val targetHref = meta.spineHrefs[chapterIndex]
            var chapterHtml: String? = null

            openInputStream(context, uri)?.use { inputStream ->
                val zis = ZipInputStream(inputStream)
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(targetHref, ignoreCase = true)) {
                        chapterHtml = zis.readBytes().toString(Charsets.UTF_8)
                        break
                    }
                    entry = zis.nextEntry
                }
            }

            val rawHtml = chapterHtml ?: "<p>Bu bölüm yüklenemedi.</p>"
            val plainText = stripHtmlTags(rawHtml)
            val chapterTitle = extractHtmlTitle(rawHtml) ?: "Bölüm ${chapterIndex + 1}"
            val paragraphs = parseParagraphs(rawHtml, plainText)

            return ChapterContent(
                title = chapterTitle,
                htmlContent = rawHtml,
                plainText = plainText,
                paragraphs = paragraphs
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading EPUB chapter $chapterIndex", e)
            return ChapterContent("Hata", "<p>Hata: ${e.message}</p>", "Bölüm yüklenemedi.", emptyList())
        }
    }

    /**
     * Extracts Table of Contents from the EPUB spine and metadata.
     */
    fun extractTableOfContents(context: Context, uri: Uri): List<com.example.ui.reader.TocItem> {
        val items = mutableListOf<com.example.ui.reader.TocItem>()
        try {
            val meta = extractMetaAndCover(context, uri, uri.toString())
            if (meta.spineHrefs.isEmpty()) {
                return items
            }

            for (i in meta.spineHrefs.indices) {
                val chapter = readChapter(context, uri, i)
                val title = chapter.title?.takeIf { it.isNotBlank() && !it.equals("Hata", ignoreCase = true) }
                    ?: "Bölüm ${i + 1}"
                items.add(
                    com.example.ui.reader.TocItem(
                        title = title,
                        targetIndex = i,
                        isChapter = true
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting TOC for EPUB $uri", e)
        }
        return items
    }

    /**
     * Searches for a text string across all EPUB chapters.
     */
    fun searchInEpub(
        context: Context,
        uri: Uri,
        query: String
    ): List<com.example.ui.reader.BookSearchResult> {
        val results = mutableListOf<com.example.ui.reader.BookSearchResult>()
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < 2) return results

        try {
            val meta = extractMetaAndCover(context, uri, uri.toString())
            for (i in meta.spineHrefs.indices) {
                if (results.size >= 60) break // Limit to prevent excessive memory/time

                val chapter = readChapter(context, uri, i)
                val text = chapter.plainText
                val title = chapter.title ?: "Bölüm ${i + 1}"

                var startIndex = 0
                while (startIndex < text.length && results.size < 60) {
                    val matchIndex = text.indexOf(trimmedQuery, startIndex, ignoreCase = true)
                    if (matchIndex == -1) break

                    val snippetStart = maxOf(0, matchIndex - 35)
                    val snippetEnd = minOf(text.length, matchIndex + trimmedQuery.length + 45)

                    val snippetBuilder = StringBuilder()
                    if (snippetStart > 0) snippetBuilder.append("…")
                    snippetBuilder.append(text.substring(snippetStart, snippetEnd).replace(Regex("\\s+"), " ").trim())
                    if (snippetEnd < text.length) snippetBuilder.append("…")

                    results.add(
                        com.example.ui.reader.BookSearchResult(
                            targetIndex = i,
                            targetTitle = title,
                            snippet = snippetBuilder.toString(),
                            matchWord = trimmedQuery
                        )
                    )

                    startIndex = matchIndex + trimmedQuery.length
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching in EPUB $uri", e)
        }
        return results
    }

    private fun parseParagraphs(rawHtml: String, plainText: String): List<ChapterParagraph> {
        val paragraphs = mutableListOf<ChapterParagraph>()
        // First try extracting <p> and heading blocks
        val blockRegex = Regex("<(p|h1|h2|h3|h4|blockquote)[^>]*>([\\s\\S]*?)</\\1>", RegexOption.IGNORE_CASE)
        val matches = blockRegex.findAll(rawHtml).toList()

        if (matches.isNotEmpty()) {
            matches.forEachIndexed { index, match ->
                val tag = match.groupValues[1].lowercase()
                val inner = match.groupValues[2]
                val cleaned = stripHtmlTags(inner).trim()
                if (cleaned.isNotBlank()) {
                    paragraphs.add(
                        ChapterParagraph(
                            index = index,
                            text = cleaned,
                            isHeading = tag.startsWith("h")
                        )
                    )
                }
            }
        }

        // Fallback if no <p> tags matched
        if (paragraphs.isEmpty()) {
            val lines = plainText.split(Regex("\n\n+"))
            lines.forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.isNotBlank()) {
                    paragraphs.add(
                        ChapterParagraph(
                            index = index,
                            text = trimmed,
                            isHeading = false
                        )
                    )
                }
            }
        }

        return paragraphs
    }

    private fun parseContainerForOpf(containerXml: String): String {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(containerXml)))
            val rootfiles = doc.getElementsByTagName("rootfile")
            if (rootfiles.length > 0) {
                val element = rootfiles.item(0) as Element
                element.getAttribute("full-path")
            } else ""
        } catch (e: Exception) {
            // Regex fallback
            val match = Regex("""full-path\s*=\s*["']([^"']+)["']""").find(containerXml)
            match?.groupValues?.get(1) ?: ""
        }
    }

    private data class ParsedOpf(
        val title: String?,
        val author: String?,
        val coverHref: String?,
        val spineHrefs: List<String>
    )

    private fun parseOpf(opfXml: String, opfPath: String): ParsedOpf {
        var title: String? = null
        var author: String? = null
        var coverId: String? = null
        var coverHref: String? = null
        val manifest = mutableMapOf<String, String>() // id -> href
        val spineList = mutableListOf<String>()

        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(opfXml)))

            // Title
            val titleNodes = doc.getElementsByTagName("dc:title")
            if (titleNodes.length > 0) {
                title = titleNodes.item(0).textContent?.trim()
            }

            // Creator / Author
            val creatorNodes = doc.getElementsByTagName("dc:creator")
            if (creatorNodes.length > 0) {
                author = creatorNodes.item(0).textContent?.trim()
            }

            // Manifest items
            val itemNodes = doc.getElementsByTagName("item")
            for (i in 0 until itemNodes.length) {
                val item = itemNodes.item(i) as Element
                val id = item.getAttribute("id")
                val href = item.getAttribute("href")
                val properties = item.getAttribute("properties")
                val mediaType = item.getAttribute("media-type")

                manifest[id] = href

                if (properties.contains("cover-image", ignoreCase = true)) {
                    coverHref = href
                } else if (mediaType.startsWith("image/") && (id.contains("cover", ignoreCase = true) || href.contains("cover", ignoreCase = true))) {
                    if (coverHref == null) coverHref = href
                }
            }

            // Meta tags (looking for name="cover")
            val metaNodes = doc.getElementsByTagName("meta")
            for (i in 0 until metaNodes.length) {
                val meta = metaNodes.item(i) as Element
                if (meta.getAttribute("name").equals("cover", ignoreCase = true)) {
                    coverId = meta.getAttribute("content")
                }
            }

            if (coverHref == null && coverId != null && manifest.containsKey(coverId)) {
                coverHref = manifest[coverId]
            }

            // Spine
            val itemrefNodes = doc.getElementsByTagName("itemref")
            for (i in 0 until itemrefNodes.length) {
                val itemref = itemrefNodes.item(i) as Element
                val idref = itemref.getAttribute("idref")
                manifest[idref]?.let { href ->
                    spineList.add(resolvePath(opfPath, href))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing OPF with XML parser, using regex fallback", e)
            // Regex fallbacks
            title = Regex("""<dc:title[^>]*>([^<]+)</dc:title>""").find(opfXml)?.groupValues?.get(1)?.trim()
            author = Regex("""<dc:creator[^>]*>([^<]+)</dc:creator>""").find(opfXml)?.groupValues?.get(1)?.trim()
        }

        return ParsedOpf(title, author, coverHref, spineList)
    }

    private fun resolvePath(basePath: String, relativePath: String): String {
        val parent = if (basePath.contains("/")) basePath.substringBeforeLast('/') + "/" else ""
        val combined = parent + relativePath
        return combined.replace("/./", "/").replace("//", "/")
    }

    private fun stripHtmlTags(html: String): String {
        return html
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractHtmlTitle(html: String): String? {
        val titleMatch = Regex("""<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE).find(html)
        if (titleMatch != null) {
            val t = titleMatch.groupValues[1].trim()
            if (t.isNotBlank()) return t
        }
        val h1Match = Regex("""<h1[^>]*>([^<]+)</h1>""", RegexOption.IGNORE_CASE).find(html)
        return h1Match?.groupValues?.get(1)?.trim()
    }
}
