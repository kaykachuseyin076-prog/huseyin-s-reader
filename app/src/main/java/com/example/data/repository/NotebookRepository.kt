package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.NotebookEntity
import com.example.data.local.entity.NotebookPageEntity
import com.example.data.local.entity.NotebookShapeEntity
import com.example.data.local.entity.NotebookStrokeEntity
import com.example.data.local.entity.NotebookTextBoxEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class NotebookRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val notebookDao = database.notebookDao()

    val allNotebooks: Flow<List<NotebookEntity>> = notebookDao.getAllNotebooks()

    suspend fun getNotebook(id: String): NotebookEntity? = withContext(Dispatchers.IO) {
        notebookDao.getNotebookById(id)
    }

    fun observeNotebook(id: String): Flow<NotebookEntity?> {
        return notebookDao.observeNotebookById(id)
    }

    fun getPages(notebookId: String): Flow<List<NotebookPageEntity>> {
        return notebookDao.getPagesForNotebook(notebookId)
    }

    suspend fun getPagesList(notebookId: String): List<NotebookPageEntity> = withContext(Dispatchers.IO) {
        notebookDao.getPagesListForNotebook(notebookId)
    }

    fun getStrokes(pageId: String): Flow<List<NotebookStrokeEntity>> {
        return notebookDao.getStrokesForPage(pageId)
    }

    suspend fun getStrokesList(pageId: String): List<NotebookStrokeEntity> = withContext(Dispatchers.IO) {
        notebookDao.getStrokesListForPage(pageId)
    }

    fun getTextBoxes(pageId: String): Flow<List<NotebookTextBoxEntity>> {
        return notebookDao.getTextBoxesForPage(pageId)
    }

    fun getShapes(pageId: String): Flow<List<NotebookShapeEntity>> {
        return notebookDao.getShapesForPage(pageId)
    }

    /**
     * Creates a new digital notebook with an initial first page with the selected template.
     */
    suspend fun createNotebook(
        title: String,
        template: String,
        bookId: String? = null
    ): String = withContext(Dispatchers.IO) {
        val notebookId = UUID.randomUUID().toString()
        val pageId = UUID.randomUUID().toString()

        val notebook = NotebookEntity(
            id = notebookId,
            title = title.ifBlank { "Yeni Not Defteri" },
            defaultTemplate = template,
            bookId = bookId,
            pageCount = 1
        )
        val initialPage = NotebookPageEntity(
            id = pageId,
            notebookId = notebookId,
            pageIndex = 0,
            template = template
        )

        notebookDao.insertNotebook(notebook)
        notebookDao.insertPage(initialPage)
        notebookId
    }

    fun getNotebooksForBook(bookId: String): Flow<List<NotebookEntity>> {
        return notebookDao.getNotebooksForBook(bookId)
    }

    fun getNotebooksForBookLocation(bookId: String, pageIndex: Int): Flow<List<NotebookEntity>> {
        return notebookDao.getNotebooksForBookLocation(bookId, pageIndex)
    }

    fun observeNotebookForHighlight(highlightId: String): Flow<NotebookEntity?> {
        return notebookDao.observeNotebookForHighlight(highlightId)
    }

    suspend fun getNotebookForHighlightSync(highlightId: String): NotebookEntity? = withContext(Dispatchers.IO) {
        notebookDao.getNotebookForHighlightSync(highlightId)
    }

    suspend fun createBookLinkedNotebook(
        bookId: String,
        bookTitle: String,
        pageOrChapter: Int,
        format: String,
        chapterTitle: String? = null,
        highlightId: String? = null,
        snippet: String? = null,
        customTitle: String? = null,
        template: String = "BOOK_NOTE"
    ): String = withContext(Dispatchers.IO) {
        val notebookId = UUID.randomUUID().toString()
        val pageId = UUID.randomUUID().toString()

        val locationDesc = if (format.uppercase() == "EPUB") {
            chapterTitle ?: "Bölüm ${pageOrChapter + 1}"
        } else {
            "Sayfa ${pageOrChapter + 1}"
        }

        val finalTitle = customTitle?.ifBlank { null }
            ?: "$bookTitle — $locationDesc Notu"

        val notebook = NotebookEntity(
            id = notebookId,
            title = finalTitle,
            defaultTemplate = template,
            bookId = bookId,
            bookTitle = bookTitle,
            pdfPage = if (format.uppercase() == "PDF") pageOrChapter else null,
            chapterIndex = if (format.uppercase() == "EPUB") pageOrChapter else null,
            chapterTitle = chapterTitle,
            highlightId = highlightId,
            sourceSnippet = snippet,
            pageCount = 1
        )
        val initialPage = NotebookPageEntity(
            id = pageId,
            notebookId = notebookId,
            pageIndex = 0,
            template = template
        )

        notebookDao.insertNotebook(notebook)
        notebookDao.insertPage(initialPage)

        // If a source snippet is provided, add it as a neat text box on the first page!
        if (!snippet.isNullOrBlank()) {
            val textBoxId = UUID.randomUUID().toString()
            val textEntity = NotebookTextBoxEntity(
                id = textBoxId,
                pageId = pageId,
                notebookId = notebookId,
                text = "📌 ALINTI:\n\"$snippet\"",
                x = 24f,
                y = 24f,
                width = 320f,
                height = 120f,
                fontSizeSp = 14f,
                color = 0xFF37474FL,
                isBold = false
            )
            notebookDao.insertTextBox(textEntity)
        }

        notebookId
    }

    /**
     * Adds a new page to the notebook with the specified or default template.
     */
    suspend fun addPage(
        notebookId: String,
        template: String? = null
    ): NotebookPageEntity = withContext(Dispatchers.IO) {
        val existingPages = notebookDao.getPagesListForNotebook(notebookId)
        val newIndex = existingPages.size
        val effectiveTemplate = template
            ?: existingPages.lastOrNull()?.template
            ?: notebookDao.getNotebookById(notebookId)?.defaultTemplate
            ?: "BLANK"

        val newPage = NotebookPageEntity(
            id = UUID.randomUUID().toString(),
            notebookId = notebookId,
            pageIndex = newIndex,
            template = effectiveTemplate
        )
        notebookDao.insertPage(newPage)
        notebookDao.updateNotebookPageCount(notebookId, newIndex + 1)
        newPage
    }

    suspend fun deletePage(notebookId: String, pageId: String) = withContext(Dispatchers.IO) {
        notebookDao.deletePageCascading(pageId)
        val remaining = notebookDao.getPagesListForNotebook(notebookId)
        // Re-index remaining pages
        for (i in remaining.indices) {
            if (remaining[i].pageIndex != i) {
                notebookDao.updatePage(remaining[i].copy(pageIndex = i))
            }
        }
        notebookDao.updateNotebookPageCount(notebookId, remaining.size)
    }

    suspend fun renameNotebook(notebookId: String, newTitle: String) = withContext(Dispatchers.IO) {
        notebookDao.renameNotebook(notebookId, newTitle)
    }

    suspend fun updateNotebookTemplate(notebookId: String, template: String) = withContext(Dispatchers.IO) {
        notebookDao.updateNotebookTemplate(notebookId, template)
    }

    suspend fun deleteNotebook(notebookId: String) = withContext(Dispatchers.IO) {
        notebookDao.deleteNotebookCascading(notebookId)
    }

    suspend fun saveStroke(stroke: NotebookStrokeEntity) = withContext(Dispatchers.IO) {
        notebookDao.insertStroke(stroke)
    }

    suspend fun saveStrokes(strokes: List<NotebookStrokeEntity>) = withContext(Dispatchers.IO) {
        notebookDao.insertStrokes(strokes)
    }

    suspend fun deleteStrokes(strokeIds: List<String>) = withContext(Dispatchers.IO) {
        notebookDao.deleteStrokesByIds(strokeIds)
    }

    suspend fun saveTextBox(textBox: NotebookTextBoxEntity) = withContext(Dispatchers.IO) {
        notebookDao.insertTextBox(textBox)
    }

    suspend fun deleteTextBox(textBoxId: String) = withContext(Dispatchers.IO) {
        notebookDao.deleteTextBoxById(textBoxId)
    }

    suspend fun saveShape(shape: NotebookShapeEntity) = withContext(Dispatchers.IO) {
        notebookDao.insertShape(shape)
    }

    suspend fun deleteShape(shapeId: String) = withContext(Dispatchers.IO) {
        notebookDao.deleteShapeById(shapeId)
    }

    suspend fun updatePageViewport(pageId: String, scale: Float, panX: Float, panY: Float) = withContext(Dispatchers.IO) {
        notebookDao.updatePageViewport(pageId, scale, panX, panY)
    }
}
