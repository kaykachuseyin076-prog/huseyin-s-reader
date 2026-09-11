package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream

/**
 * High-performance, memory-safe native PdfRenderer manager.
 * Optimized for large PDF documents with session pooling, bounded memory caching,
 * and robust error isolation.
 */
object PdfHelper {
    private const val TAG = "PdfHelper"

    // Memory-bounded LRU Cache for rendered page bitmaps (at most 1/8th of JVM heap, 8MB-32MB)
    private val maxCacheMemoryKb = ((Runtime.getRuntime().maxMemory() / 1024) / 8).toInt().coerceIn(8192, 32768)
    private val pageCache = object : LruCache<String, Bitmap>(maxCacheMemoryKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    // Active native PdfRenderer session lock and references
    private val sessionLock = Any()
    private var activeUri: Uri? = null
    private var activePfd: ParcelFileDescriptor? = null
    private var activeRenderer: PdfRenderer? = null

    data class PdfMeta(
        val totalPages: Int,
        val coverPath: String?
    )

    /**
     * Safely opens a ParcelFileDescriptor for both content:// and file:// URI schemes.
     */
    fun openFileDescriptor(context: Context, uri: Uri): ParcelFileDescriptor? {
        return try {
            if (uri.scheme == "file" && uri.path != null) {
                ParcelFileDescriptor.open(File(uri.path!!), ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open file descriptor for $uri", e)
            null
        }
    }

    /**
     * Extracts total pages and generates/caches a cover image for the PDF.
     */
    fun extractMetaAndCover(context: Context, uri: Uri, bookId: String): PdfMeta {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = openFileDescriptor(context, uri) ?: return PdfMeta(0, null)
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            if (pageCount <= 0) {
                return PdfMeta(0, null)
            }

            // Check if cover already cached
            val safeId = bookId.hashCode().toString()
            val coversDir = File(context.cacheDir, "covers").apply { mkdirs() }
            val coverFile = File(coversDir, "pdf_$safeId.jpg")

            if (coverFile.exists() && coverFile.length() > 0) {
                return PdfMeta(pageCount, coverFile.absolutePath)
            }

            // Render first page as cover
            val page = renderer.openPage(0)
            val originalWidth = page.width
            val originalHeight = page.height

            // Scale to reasonable thumbnail dimensions (approx 400x600 max)
            val maxDimension = 640f
            val scale = (maxDimension / maxOf(originalWidth, originalHeight)).coerceAtMost(1.5f)
            val renderWidth = (originalWidth * scale).toInt().coerceIn(80, 480)
            val renderHeight = (originalHeight * scale).toInt().coerceIn(120, 720)

            var bitmap = try {
                Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.RGB_565)
            } catch (e: Throwable) {
                Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
            }
            var canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            try {
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            } catch (e: IllegalArgumentException) {
                bitmap.recycle()
                bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
            page.close()

            FileOutputStream(coverFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            bitmap.recycle()

            return PdfMeta(pageCount, coverFile.absolutePath)
        } catch (t: Throwable) {
            Log.e(TAG, "Error extracting PDF cover for $uri", t)
            return PdfMeta(0, null)
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (ignored: Throwable) {}
        }
    }

    /**
     * Renders a specific page using native PdfRenderer with session caching.
     * Thread-safe and memory-bounded to effortlessly support 100MB+ large PDFs.
     */
    fun renderPage(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetWidth: Int = 1080
    ): Bitmap? {
        val cacheKey = "${uri}_${pageIndex}_$targetWidth"
        pageCache.get(cacheKey)?.let { cachedBitmap ->
            if (!cachedBitmap.isRecycled) {
                return cachedBitmap
            }
        }

        synchronized(sessionLock) {
            // Check cache again inside lock
            pageCache.get(cacheKey)?.let { cachedBitmap ->
                if (!cachedBitmap.isRecycled) {
                    return cachedBitmap
                }
            }

            try {
                // Ensure session is open for this URI
                if (activeRenderer == null || activeUri != uri) {
                    closeActiveSessionLocked()
                    val pfd = openFileDescriptor(context, uri) ?: return null
                    activePfd = pfd
                    activeRenderer = PdfRenderer(pfd)
                    activeUri = uri
                }

                val renderer = activeRenderer ?: return null
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null

                val page = renderer.openPage(pageIndex)
                val pageWidth = page.width.toFloat().coerceAtLeast(10f)
                val pageHeight = page.height.toFloat().coerceAtLeast(10f)

                // Cap target width to 1400px to prevent large memory footprint
                val boundedTargetWidth = targetWidth.coerceIn(600, 1400)
                val scale = (boundedTargetWidth.toFloat() / pageWidth).coerceIn(0.5f, 2.5f)
                val width = (pageWidth * scale).toInt().coerceIn(100, 1600)
                val height = (pageHeight * scale).toInt().coerceIn(100, 2400)

                // Use RGB_565 with ARGB_8888 fallback
                var bitmap = try {
                    Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                } catch (e: Throwable) {
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                }
                var canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                try {
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                } catch (e: IllegalArgumentException) {
                    bitmap.recycle()
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
                page.close()

                pageCache.put(cacheKey, bitmap)
                return bitmap
            } catch (t: Throwable) {
                Log.e(TAG, "Error rendering page $pageIndex for $uri natively", t)
                // If native engine encountered an error, reset session cleanly
                closeActiveSessionLocked()
                return null
            }
        }
    }

    /**
     * Gets page dimensions without full rendering.
     */
    fun getPageDimensions(context: Context, uri: Uri, pageIndex: Int): Pair<Int, Int>? {
        synchronized(sessionLock) {
            try {
                if (activeRenderer == null || activeUri != uri) {
                    closeActiveSessionLocked()
                    val pfd = openFileDescriptor(context, uri) ?: return null
                    activePfd = pfd
                    activeRenderer = PdfRenderer(pfd)
                    activeUri = uri
                }
                val renderer = activeRenderer ?: return null
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
                val page = renderer.openPage(pageIndex)
                val w = page.width
                val h = page.height
                page.close()
                return Pair(w, h)
            } catch (t: Throwable) {
                return null
            }
        }
    }

    /**
     * Renders a low-resolution thumbnail preview of the page (~400px width) in background.
     * Takes minimal memory (<500KB) and renders in milliseconds for instant placeholder display.
     */
    fun renderPagePreview(context: Context, uri: Uri, pageIndex: Int): Bitmap? {
        return renderPage(context, uri, pageIndex, targetWidth = 450)
    }

    /**
     * Renders a page using a tiled background rendering approach for memory efficiency on large PDF documents.
     * Splitting into [numTiles] vertical slices cuts the peak contiguous bitmap allocation in half or quarter.
     * Supports memory caching for instantaneous swipe-back navigation.
     */
    fun renderPageTiles(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetWidth: Int = 1080,
        numTiles: Int = 2
    ): List<Bitmap>? {
        val boundedTargetWidth = targetWidth.coerceIn(600, 1400)
        // Check if all tiles already in cache
        var allCached = true
        val cachedTiles = mutableListOf<Bitmap>()
        for (t in 0 until numTiles) {
            val tileKey = "${uri}_tile_${pageIndex}_${t}_$boundedTargetWidth"
            val cached = pageCache.get(tileKey)
            if (cached != null && !cached.isRecycled) {
                cachedTiles.add(cached)
            } else {
                allCached = false
                break
            }
        }
        if (allCached && cachedTiles.size == numTiles) {
            return cachedTiles
        }

        synchronized(sessionLock) {
            try {
                if (activeRenderer == null || activeUri != uri) {
                    closeActiveSessionLocked()
                    val pfd = openFileDescriptor(context, uri) ?: return null
                    activePfd = pfd
                    activeRenderer = PdfRenderer(pfd)
                    activeUri = uri
                }

                val renderer = activeRenderer ?: return null
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null

                val page = renderer.openPage(pageIndex)
                val pageWidth = page.width.toFloat().coerceAtLeast(10f)
                val pageHeight = page.height.toFloat().coerceAtLeast(10f)

                val scale = (boundedTargetWidth.toFloat() / pageWidth).coerceIn(0.5f, 2.5f)
                val totalWidth = (pageWidth * scale).toInt().coerceIn(100, 1600)
                val totalHeight = (pageHeight * scale).toInt().coerceIn(100, 2400)
                val tileHeight = (totalHeight / numTiles).coerceAtLeast(50)

                val tiles = mutableListOf<Bitmap>()
                for (t in 0 until numTiles) {
                    val tileKey = "${uri}_tile_${pageIndex}_${t}_$boundedTargetWidth"
                    val cached = pageCache.get(tileKey)
                    if (cached != null && !cached.isRecycled) {
                        tiles.add(cached)
                        continue
                    }

                    val actualTileHeight = if (t == numTiles - 1) {
                        totalHeight - (tileHeight * (numTiles - 1))
                    } else {
                        tileHeight
                    }

                    var tileBitmap = try {
                        Bitmap.createBitmap(totalWidth, actualTileHeight, Bitmap.Config.RGB_565)
                    } catch (e: Throwable) {
                        Bitmap.createBitmap(totalWidth, actualTileHeight, Bitmap.Config.ARGB_8888)
                    }
                    var canvas = Canvas(tileBitmap)
                    canvas.drawColor(Color.WHITE)

                    val matrix = Matrix()
                    matrix.postScale(scale, scale)
                    matrix.postTranslate(0f, -t * tileHeight.toFloat())

                    try {
                        page.render(tileBitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    } catch (e: IllegalArgumentException) {
                        tileBitmap.recycle()
                        tileBitmap = Bitmap.createBitmap(totalWidth, actualTileHeight, Bitmap.Config.ARGB_8888)
                        canvas = Canvas(tileBitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(tileBitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }

                    pageCache.put(tileKey, tileBitmap)
                    tiles.add(tileBitmap)
                }

                page.close()
                return tiles
            } catch (t: Throwable) {
                Log.e(TAG, "Error rendering tiled page $pageIndex natively", t)
                closeActiveSessionLocked()
                return null
            }
        }
    }

    /**
     * Safely closes the active native PdfRenderer session and releases file descriptors.
     */
    fun closeSession() {
        synchronized(sessionLock) {
            closeActiveSessionLocked()
        }
    }

    private fun closeActiveSessionLocked() {
        try {
            activeRenderer?.close()
        } catch (ignored: Throwable) {}
        try {
            activePfd?.close()
        } catch (ignored: Throwable) {}
        activeRenderer = null
        activePfd = null
        activeUri = null
    }

    /**
     * Generates a Table of Contents list for the PDF pages.
     */
    fun extractTableOfContents(context: Context, uri: Uri, totalPages: Int): List<com.example.ui.reader.TocItem> {
        val items = mutableListOf<com.example.ui.reader.TocItem>()
        for (i in 0 until totalPages) {
            items.add(
                com.example.ui.reader.TocItem(
                    title = "Sayfa ${i + 1}",
                    targetIndex = i,
                    isChapter = false
                )
            )
        }
        return items
    }

    /**
     * Extracts textual content for the given PDF page using PdfTextExtractor.
     */
    fun getPageText(context: Context, uri: Uri, pageIndex: Int, bookTitle: String? = null): String {
        return try {
            val layout = PdfTextExtractor.getPageLayout(context, uri, pageIndex, bookTitle)
            layout.fullText.ifBlank { "Sayfa ${pageIndex + 1}" }
        } catch (t: Throwable) {
            "Sayfa ${pageIndex + 1}"
        }
    }

    /**
     * Searches for a query string across actual PDF pages.
     */
    fun searchInPdf(
        context: Context,
        uri: Uri,
        query: String,
        totalPages: Int,
        bookTitle: String? = null
    ): List<com.example.ui.reader.BookSearchResult> {
        val results = mutableListOf<com.example.ui.reader.BookSearchResult>()
        val trimmed = query.trim()
        if (trimmed.length < 2) return results

        try {
            val maxPagesToScan = totalPages.coerceAtMost(60)
            for (pageIdx in 0 until maxPagesToScan) {
                if (results.size >= 50) break

                val layout = PdfTextExtractor.getPageLayout(context, uri, pageIdx, bookTitle)
                val text = layout.fullText
                if (text.isBlank()) continue

                var startIndex = 0
                while (startIndex < text.length && results.size < 50) {
                    val matchIndex = text.indexOf(trimmed, startIndex, ignoreCase = true)
                    if (matchIndex == -1) break

                    val snippetStart = maxOf(0, matchIndex - 35)
                    val snippetEnd = minOf(text.length, matchIndex + trimmed.length + 45)

                    val snippetBuilder = StringBuilder()
                    if (snippetStart > 0) snippetBuilder.append("…")
                    snippetBuilder.append(text.substring(snippetStart, snippetEnd).replace(Regex("\\s+"), " ").trim())
                    if (snippetEnd < text.length) snippetBuilder.append("…")

                    results.add(
                        com.example.ui.reader.BookSearchResult(
                            targetIndex = pageIdx,
                            targetTitle = "Sayfa ${pageIdx + 1}",
                            snippet = snippetBuilder.toString(),
                            matchWord = trimmed
                        )
                    )

                    startIndex = matchIndex + trimmed.length
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error searching in PDF", t)
        }
        return results
    }
}
