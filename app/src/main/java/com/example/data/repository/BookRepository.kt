package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.FolderEntity
import com.example.util.BookTitleCleaner
import com.example.util.DeviceBookScanner
import com.example.util.EpubHelper
import com.example.util.PdfHelper
import com.example.util.SampleBookProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope

class BookRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val bookDao = db.bookDao()
    private val folderDao = db.folderDao()
    private val highlightDao = db.highlightDao()
    private val noteDao = db.noteDao()
    private val inkStrokeDao = db.inkStrokeDao()
    private val discussionDao = db.discussionDao()

    fun getDiscussionDao() = discussionDao

    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()
    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()

    companion object {
        private const val TAG = "BookRepository"
    }

    suspend fun getBookById(id: String): BookEntity? = withContext(Dispatchers.IO) {
        bookDao.getBookById(id)
    }

    /**
     * Imports sample starter books into the database if library is empty.
     */
    suspend fun checkAndLoadSampleBooksIfEmpty() = withContext(Dispatchers.IO) {
        val count = bookDao.getBookCount()
        if (count == 0) {
            val sampleBooks = SampleBookProvider.createSampleBooks(context)
            bookDao.insertOrUpdateAll(sampleBooks)
        }
    }

    suspend fun loadSampleBooksExplicitly() = withContext(Dispatchers.IO) {
        val sampleBooks = SampleBookProvider.createSampleBooks(context)
        bookDao.insertOrUpdateAll(sampleBooks)
    }

    /**
     * Automatically scans device storage (MediaStore, accessible filesystem, persisted SAF)
     * and synchronizes the library progressively. Avoids duplicates, preserves reading progress,
     * and removes deleted/inaccessible files.
     */
    suspend fun scanDeviceStorage(
        onProgress: (suspend (scannedCount: Int, foundCount: Int) -> Unit)? = null
    ): Int = withContext(Dispatchers.IO) {
        try {
            // Ensure sample starter books are loaded if database is empty
            val initialCount = bookDao.getBookCount()
            if (initialCount == 0) {
                checkAndLoadSampleBooksIfEmpty()
            }

            val persistedFolders = folderDao.getAllFoldersList().map { Uri.parse(it.uriString) }
            val existingBooks = bookDao.getAllBooksList()
            val existingByUri = existingBooks.associateBy { it.uriString }.toMutableMap()
            val existingByNameAndSize = existingBooks.associateBy { "${it.title.lowercase()}_${it.fileSize}" }.toMutableMap()

            val discoveredDocs = DeviceBookScanner.discoverAllDocuments(
                context = context,
                persistedFolders = persistedFolders,
                onProgressUpdate = onProgress
            )

            val validDiscoveredUris = mutableSetOf<String>()
            var newBooksAdded = 0

            for (doc in discoveredDocs) {
                val uriStr = doc.uri.toString()
                validDiscoveredUris.add(uriStr)
                if (doc.filePath != null) {
                    validDiscoveredUris.add(Uri.fromFile(java.io.File(doc.filePath)).toString())
                }

                val keyByNameSize = "${doc.fileName.lowercase()}_${doc.fileSize}"
                val existing = existingByUri[uriStr] ?: existingByNameAndSize[keyByNameSize]

                if (existing != null) {
                    // Update URI if changed (e.g. from file:// to content:// or moved)
                    if (existing.uriString != uriStr) {
                        val updated = existing.copy(uriString = uriStr)
                        bookDao.insertOrUpdate(updated)
                        existingByUri[uriStr] = updated
                    }
                } else {
                    // Newly discovered book -> process immediately and save so UI updates progressively
                    val processed = processSingleDocument(
                        uri = doc.uri,
                        fileName = doc.fileName,
                        isPdf = doc.isPdf,
                        fileSize = doc.fileSize
                    )
                    if (processed != null) {
                        bookDao.insertOrUpdate(processed)
                        existingByUri[processed.uriString] = processed
                        existingByNameAndSize["${processed.title.lowercase()}_${processed.fileSize}"] = processed
                        newBooksAdded++
                        onProgress?.invoke(discoveredDocs.size, existingByUri.size)
                    }
                }
            }

            // Cleanup deleted / inaccessible books (excluding built-in sample books)
            val staleIds = mutableListOf<String>()
            for (book in existingBooks) {
                val isSample = book.uriString.contains("sample_books") || book.id.contains("sample_books")
                if (!isSample && !validDiscoveredUris.contains(book.uriString)) {
                    val isAccessible = DeviceBookScanner.isDocumentAccessible(context, book.uriString)
                    if (!isAccessible) {
                        staleIds.add(book.id)
                    }
                }
            }

            if (staleIds.isNotEmpty()) {
                bookDao.deleteBooksByIds(staleIds)
                Log.d(TAG, "Pruned ${staleIds.size} deleted/inaccessible books from library")
            }
            
            // Queue cover generation for any books that need it
            triggerBackgroundCoverGeneration()

            newBooksAdded
        } catch (e: Exception) {
            Log.e(TAG, "Error in scanDeviceStorage", e)
            0
        }
    }

    /**
     * Persists a folder URI and recursively scans it for PDF and EPUB files.
     */
    suspend fun scanAndAddFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            // Take persistable permission
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
            } catch (e: SecurityException) {
                Log.w(TAG, "Failed to take persistable permission: ${e.message}")
            }

            val docFile = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext 0
            val folderName = docFile.name ?: "Belgeler"

            // Save folder record
            folderDao.insertFolder(
                FolderEntity(
                    uriString = treeUri.toString(),
                    displayName = folderName,
                    addedTimestamp = System.currentTimeMillis(),
                    lastScannedTimestamp = System.currentTimeMillis()
                )
            )

            // Scan directory
            val discoveredBooks = mutableListOf<BookEntity>()
            scanFolderRecursive(docFile, discoveredBooks)

            if (discoveredBooks.isNotEmpty()) {
                bookDao.insertOrUpdateAll(discoveredBooks)
            }
            triggerBackgroundCoverGeneration()

            discoveredBooks.size
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning folder: $treeUri", e)
            0
        }
    }

    /**
     * Re-scans all previously added folders.
     */
    suspend fun refreshAllFolders(): Int = withContext(Dispatchers.IO) {
        val folders = folderDao.getAllFoldersList()
        var totalAdded = 0
        for (folder in folders) {
            val treeUri = Uri.parse(folder.uriString)
            val docFile = DocumentFile.fromTreeUri(context, treeUri)
            if (docFile != null && docFile.exists()) {
                val discovered = mutableListOf<BookEntity>()
                scanFolderRecursive(docFile, discovered)
                if (discovered.isNotEmpty()) {
                    bookDao.insertOrUpdateAll(discovered)
                    totalAdded += discovered.size
                }
            }
        }
        triggerBackgroundCoverGeneration()
        totalAdded
    }

    private suspend fun scanFolderRecursive(directory: DocumentFile, outList: MutableList<BookEntity>) {
        val files = directory.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                // Scan subdirectories
                scanFolderRecursive(file, outList)
            } else if (file.isFile) {
                val name = file.name ?: continue
                val lowerName = name.lowercase()
                val isPdf = lowerName.endsWith(".pdf") || file.type == "application/pdf"
                val isEpub = lowerName.endsWith(".epub") || file.type == "application/epub+zip"

                if (isPdf || isEpub) {
                    val book = processSingleDocument(file.uri, name, isPdf, file.length())
                    if (book != null) {
                        outList.add(book)
                    }
                }
            }
        }
    }

    /**
     * Adds an individual file selected via file picker.
     */
    suspend fun addSingleDocument(uri: Uri): BookEntity? = withContext(Dispatchers.IO) {
        try {
            var finalUri = uri
            var persistSucceeded = false
            // Try taking persistable permission
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                persistSucceeded = true
            } catch (e: Exception) {
                Log.w(TAG, "Persistable permission not granted for single uri: ${e.message}")
            }

            val mimeType = try { context.contentResolver.getType(uri) } catch (e: Exception) { null }
            val rawFileName = queryFileName(uri) ?: "Bilinmeyen Kitap"
            val isPdf = rawFileName.endsWith(".pdf", ignoreCase = true) ||
                    mimeType == "application/pdf" ||
                    mimeType?.contains("pdf", ignoreCase = true) == true
            val isEpub = rawFileName.endsWith(".epub", ignoreCase = true) ||
                    mimeType == "application/epub+zip" ||
                    mimeType?.contains("epub", ignoreCase = true) == true

            if (!isPdf && !isEpub) {
                return@withContext null
            }

            val fileName = if (!rawFileName.contains(".")) {
                if (isPdf) "$rawFileName.pdf" else "$rawFileName.epub"
            } else {
                rawFileName
            }

            // If persistable permission failed and it's a content URI, copy to app's internal storage
            // so the book remains permanently accessible after temporary intent grants expire
            if (!persistSucceeded && uri.scheme == "content") {
                try {
                    val booksDir = File(context.filesDir, "imported_books").apply { mkdirs() }
                    val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    val destFile = File(booksDir, "${System.currentTimeMillis()}_$safeName")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (destFile.exists() && destFile.length() > 0) {
                        finalUri = Uri.fromFile(destFile)
                    }
                } catch (copyEx: Exception) {
                    Log.w(TAG, "Failed to copy external content to internal storage", copyEx)
                }
            }

            val fileSize = queryFileSize(finalUri)
            val book = processSingleDocument(finalUri, fileName, isPdf, fileSize, extractFullMeta = true)
            if (book != null) {
                bookDao.insertOrUpdate(book)
                // Also trigger full extraction to get cover if it wasn't done
                extractAndSaveMetadata(book)
            }
            book
        } catch (e: Exception) {
            Log.e(TAG, "Error importing file $uri", e)
            null
        }
    }

    private suspend fun processSingleDocument(
        uri: Uri,
        fileName: String,
        isPdf: Boolean,
        fileSize: Long,
        extractFullMeta: Boolean = false
    ): BookEntity? {
        val bookId = uri.toString()
        val existing = bookDao.getBookById(bookId)

        val format = if (isPdf) "PDF" else "EPUB"

        var title: String? = existing?.title
        var author: String? = existing?.author
        var coverPath: String? = existing?.coverPath
        var totalPages = existing?.totalPages ?: 0

        if (extractFullMeta || existing == null) {
            // We only do fast metadata extraction here if existing is null, 
            // but we skip the expensive cover generation in this fast pass
            if (isPdf) {
                // To keep it fast, we don't open the PDF here unless requested.
                // We'll queue it for a background update.
            } else {
                // Basic clean for EPUB
            }
        }
        
        // Clean title if not found in metadata
        val finalTitle = if (!title.isNullOrBlank()) {
            title!!.trim()
        } else {
            BookTitleCleaner.cleanFileName(fileName)
        }

        return BookEntity(
            id = bookId,
            uriString = uri.toString(),
            title = finalTitle,
            author = author,
            format = format,
            fileSize = fileSize,
            coverPath = coverPath,
            currentPage = existing?.currentPage ?: 0,
            totalPages = totalPages.coerceAtLeast(1),
            lastReadProgress = existing?.lastReadProgress ?: 0f,
            lastReadTimestamp = existing?.lastReadTimestamp ?: 0L,
            addedTimestamp = existing?.addedTimestamp ?: System.currentTimeMillis()
        )
    }

    /**
     * Extracts full metadata and cover, then updates the database.
     */
    suspend fun extractAndSaveMetadata(book: BookEntity) = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(book.uriString)
            val isPdf = book.format.equals("PDF", ignoreCase = true)
            var title = book.title
            var author = book.author
            var coverPath = book.coverPath
            var totalPages = book.totalPages

            if (isPdf) {
                val meta = PdfHelper.extractMetaAndCover(context, uri, book.id)
                if (meta.totalPages > 0) totalPages = meta.totalPages
                if (meta.coverPath != null) coverPath = meta.coverPath
            } else {
                val meta = EpubHelper.extractMetaAndCover(context, uri, book.id)
                if (!meta.title.isNullOrBlank()) title = meta.title.trim()
                if (!meta.author.isNullOrBlank()) author = meta.author.trim()
                if (meta.totalChapters > 0) totalPages = meta.totalChapters
                if (meta.coverPath != null) coverPath = meta.coverPath
            }

            val updatedBook = book.copy(
                title = title,
                author = author,
                coverPath = coverPath,
                totalPages = totalPages.coerceAtLeast(1)
            )
            bookDao.insertOrUpdate(updatedBook)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting metadata for ${book.id}", e)
        }
    }

    private var coverGenerationJob: kotlinx.coroutines.Job? = null

    private fun triggerBackgroundCoverGeneration() {
        if (coverGenerationJob?.isActive == true) return

        coverGenerationJob = kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                var needsMetadata = bookDao.getAllBooksList().filter { it.coverPath == null || it.totalPages <= 0 }
                while (needsMetadata.isNotEmpty()) {
                    for (book in needsMetadata) {
                        extractAndSaveMetadata(book)
                    }
                    // Check if more were added while we were processing
                    needsMetadata = bookDao.getAllBooksList().filter { it.coverPath == null || it.totalPages <= 0 }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in background cover generation", e)
            }
        }
    }

    suspend fun updateReadingProgress(
        bookId: String,
        currentPage: Int,
        totalPages: Int
    ) = withContext(Dispatchers.IO) {
        val progress = if (totalPages > 0) {
            ((currentPage + 1).toFloat() / totalPages.toFloat()).coerceIn(0f, 1f)
        } else 0f

        bookDao.updateReadingProgress(
            id = bookId,
            currentPage = currentPage,
            totalPages = totalPages,
            progress = progress,
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun deleteBook(bookId: String) = withContext(Dispatchers.IO) {
        bookDao.deleteBookById(bookId)
    }

    // ==========================================
    // HIGHLIGHT & NOTE OPERATIONS (AŞAMA 3)
    // ==========================================

    fun getHighlightsForBook(bookId: String): Flow<List<com.example.data.local.entity.HighlightWithNote>> {
        return highlightDao.getHighlightsWithNotesForBook(bookId)
    }

    fun getHighlightsForLocation(
        bookId: String,
        chapterOrPageIndex: Int
    ): Flow<List<com.example.data.local.entity.HighlightWithNote>> {
        return highlightDao.getHighlightsWithNotesForLocation(bookId, chapterOrPageIndex)
    }

    suspend fun getHighlightById(id: String): com.example.data.local.entity.HighlightEntity? = withContext(Dispatchers.IO) {
        highlightDao.getHighlightById(id)
    }

    suspend fun addHighlight(
        highlight: com.example.data.local.entity.HighlightEntity,
        initialNoteText: String? = null
    ): com.example.data.local.entity.HighlightEntity = withContext(Dispatchers.IO) {
        highlightDao.insertHighlight(highlight)
        if (!initialNoteText.isNullOrBlank()) {
            val note = com.example.data.local.entity.NoteEntity(
                id = java.util.UUID.randomUUID().toString(),
                highlightId = highlight.id,
                bookId = highlight.bookId,
                noteText = initialNoteText.trim()
            )
            noteDao.insertOrUpdateNote(note)
        }
        highlight
    }

    suspend fun deleteHighlight(highlightId: String) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteByHighlightId(highlightId)
        discussionDao.deleteByHighlightId(highlightId)
        highlightDao.deleteHighlight(highlightId)
    }

    suspend fun saveNote(
        highlightId: String,
        bookId: String,
        noteText: String
    ): com.example.data.local.entity.NoteEntity = withContext(Dispatchers.IO) {
        val existing = noteDao.getNoteByHighlightId(highlightId)
        val note = if (existing != null) {
            existing.copy(
                noteText = noteText.trim(),
                updatedAt = System.currentTimeMillis()
            )
        } else {
            com.example.data.local.entity.NoteEntity(
                id = java.util.UUID.randomUUID().toString(),
                highlightId = highlightId,
                bookId = bookId,
                noteText = noteText.trim()
            )
        }
        noteDao.insertOrUpdateNote(note)
        note
    }

    suspend fun deleteNoteByHighlightId(highlightId: String) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteByHighlightId(highlightId)
    }

    suspend fun getNoteByHighlightId(highlightId: String): com.example.data.local.entity.NoteEntity? = withContext(Dispatchers.IO) {
        noteDao.getNoteByHighlightId(highlightId)
    }

    // ==========================================
    // INK / HANDWRITING STROKES (AŞAMA 4)
    // ==========================================

    fun getInkStrokesForPage(bookId: String, format: String, pageOrChapter: Int): Flow<List<com.example.data.local.entity.InkStrokeEntity>> {
        return inkStrokeDao.getStrokesForPage(bookId, format, pageOrChapter)
    }

    fun getAllInkStrokesForBook(bookId: String): Flow<List<com.example.data.local.entity.InkStrokeEntity>> {
        return inkStrokeDao.getStrokesForBook(bookId)
    }

    suspend fun insertInkStroke(stroke: com.example.data.local.entity.InkStrokeEntity) = withContext(Dispatchers.IO) {
        inkStrokeDao.insertStroke(stroke)
    }

    suspend fun insertInkStrokes(strokes: List<com.example.data.local.entity.InkStrokeEntity>) = withContext(Dispatchers.IO) {
        inkStrokeDao.insertStrokes(strokes)
    }

    suspend fun deleteInkStrokeById(id: String) = withContext(Dispatchers.IO) {
        inkStrokeDao.deleteStrokeById(id)
    }

    suspend fun deleteInkStrokesByIds(ids: List<String>) = withContext(Dispatchers.IO) {
        inkStrokeDao.deleteStrokesByIds(ids)
    }

    suspend fun deleteInkStrokesForPage(bookId: String, format: String, pageOrChapter: Int) = withContext(Dispatchers.IO) {
        inkStrokeDao.deleteStrokesForPage(bookId, format, pageOrChapter)
    }

    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    name = it.getString(index)
                }
            }
        }
        return name ?: uri.lastPathSegment
    }

    private fun queryFileSize(uri: Uri): Long {
        var size: Long = 0L
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0) {
                    size = it.getLong(index)
                }
            }
        }
        return size
    }
}
