package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.NotebookEntity
import com.example.data.local.entity.NotebookPageEntity
import com.example.data.local.entity.NotebookShapeEntity
import com.example.data.local.entity.NotebookStrokeEntity
import com.example.data.local.entity.NotebookTextBoxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotebookDao {

    // ==========================================
    // NOTEBOOKS
    // ==========================================

    @Query("SELECT * FROM notebooks ORDER BY updatedAt DESC")
    fun getAllNotebooks(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE bookId = :bookId ORDER BY updatedAt DESC")
    fun getNotebooksForBook(bookId: String): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE bookId = :bookId AND (pdfPage = :pageIndex OR chapterIndex = :pageIndex) ORDER BY updatedAt DESC")
    fun getNotebooksForBookLocation(bookId: String, pageIndex: Int): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE highlightId = :highlightId LIMIT 1")
    suspend fun getNotebookForHighlightSync(highlightId: String): NotebookEntity?

    @Query("SELECT * FROM notebooks WHERE highlightId = :highlightId LIMIT 1")
    fun observeNotebookForHighlight(highlightId: String): Flow<NotebookEntity?>

    @Query("SELECT * FROM notebooks WHERE id = :notebookId LIMIT 1")
    suspend fun getNotebookById(notebookId: String): NotebookEntity?

    @Query("SELECT * FROM notebooks WHERE id = :notebookId LIMIT 1")
    fun observeNotebookById(notebookId: String): Flow<NotebookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebook(notebook: NotebookEntity)

    @Update
    suspend fun updateNotebook(notebook: NotebookEntity)

    @Query("UPDATE notebooks SET title = :newTitle, updatedAt = :updatedAt WHERE id = :notebookId")
    suspend fun renameNotebook(notebookId: String, newTitle: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notebooks SET defaultTemplate = :newTemplate, updatedAt = :updatedAt WHERE id = :notebookId")
    suspend fun updateNotebookTemplate(notebookId: String, newTemplate: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notebooks SET pageCount = :pageCount, updatedAt = :updatedAt WHERE id = :notebookId")
    suspend fun updateNotebookPageCount(notebookId: String, pageCount: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM notebooks WHERE id = :notebookId")
    suspend fun deleteNotebook(notebookId: String)

    @Query("DELETE FROM notebook_pages WHERE notebookId = :notebookId")
    suspend fun deletePagesForNotebook(notebookId: String)

    @Query("DELETE FROM notebook_strokes WHERE notebookId = :notebookId")
    suspend fun deleteStrokesForNotebook(notebookId: String)

    @Query("DELETE FROM notebook_text_boxes WHERE notebookId = :notebookId")
    suspend fun deleteTextBoxesForNotebook(notebookId: String)

    @Query("DELETE FROM notebook_shapes WHERE notebookId = :notebookId")
    suspend fun deleteShapesForNotebook(notebookId: String)

    @Transaction
    suspend fun deleteNotebookCascading(notebookId: String) {
        deleteShapesForNotebook(notebookId)
        deleteTextBoxesForNotebook(notebookId)
        deleteStrokesForNotebook(notebookId)
        deletePagesForNotebook(notebookId)
        deleteNotebook(notebookId)
    }

    // ==========================================
    // PAGES
    // ==========================================

    @Query("SELECT * FROM notebook_pages WHERE notebookId = :notebookId ORDER BY pageIndex ASC")
    fun getPagesForNotebook(notebookId: String): Flow<List<NotebookPageEntity>>

    @Query("SELECT * FROM notebook_pages WHERE notebookId = :notebookId ORDER BY pageIndex ASC")
    suspend fun getPagesListForNotebook(notebookId: String): List<NotebookPageEntity>

    @Query("SELECT * FROM notebook_pages WHERE id = :pageId LIMIT 1")
    suspend fun getPageById(pageId: String): NotebookPageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: NotebookPageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<NotebookPageEntity>)

    @Update
    suspend fun updatePage(page: NotebookPageEntity)

    @Query("UPDATE notebook_pages SET zoomScale = :scale, panOffsetX = :panX, panOffsetY = :panY WHERE id = :pageId")
    suspend fun updatePageViewport(pageId: String, scale: Float, panX: Float, panY: Float)

    @Query("DELETE FROM notebook_pages WHERE id = :pageId")
    suspend fun deletePage(pageId: String)

    @Query("DELETE FROM notebook_strokes WHERE pageId = :pageId")
    suspend fun deleteStrokesForPage(pageId: String)

    @Query("DELETE FROM notebook_text_boxes WHERE pageId = :pageId")
    suspend fun deleteTextBoxesForPage(pageId: String)

    @Query("DELETE FROM notebook_shapes WHERE pageId = :pageId")
    suspend fun deleteShapesForPage(pageId: String)

    @Transaction
    suspend fun deletePageCascading(pageId: String) {
        deleteStrokesForPage(pageId)
        deleteTextBoxesForPage(pageId)
        deleteShapesForPage(pageId)
        deletePage(pageId)
    }

    // ==========================================
    // STROKES
    // ==========================================

    @Query("SELECT * FROM notebook_strokes WHERE pageId = :pageId ORDER BY createdAt ASC")
    fun getStrokesForPage(pageId: String): Flow<List<NotebookStrokeEntity>>

    @Query("SELECT * FROM notebook_strokes WHERE pageId = :pageId ORDER BY createdAt ASC")
    suspend fun getStrokesListForPage(pageId: String): List<NotebookStrokeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStroke(stroke: NotebookStrokeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStrokes(strokes: List<NotebookStrokeEntity>)

    @Query("DELETE FROM notebook_strokes WHERE id = :strokeId")
    suspend fun deleteStrokeById(strokeId: String)

    @Query("DELETE FROM notebook_strokes WHERE id IN (:strokeIds)")
    suspend fun deleteStrokesByIds(strokeIds: List<String>)

    @Query("DELETE FROM notebook_strokes WHERE pageId = :pageId")
    suspend fun clearPageStrokes(pageId: String)

    // ==========================================
    // TEXT BOXES
    // ==========================================

    @Query("SELECT * FROM notebook_text_boxes WHERE pageId = :pageId ORDER BY createdAt ASC")
    fun getTextBoxesForPage(pageId: String): Flow<List<NotebookTextBoxEntity>>

    @Query("SELECT * FROM notebook_text_boxes WHERE pageId = :pageId ORDER BY createdAt ASC")
    suspend fun getTextBoxesListForPage(pageId: String): List<NotebookTextBoxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTextBox(textBox: NotebookTextBoxEntity)

    @Query("DELETE FROM notebook_text_boxes WHERE id = :textBoxId")
    suspend fun deleteTextBoxById(textBoxId: String)

    // ==========================================
    // SHAPES
    // ==========================================

    @Query("SELECT * FROM notebook_shapes WHERE pageId = :pageId ORDER BY createdAt ASC")
    fun getShapesForPage(pageId: String): Flow<List<NotebookShapeEntity>>

    @Query("SELECT * FROM notebook_shapes WHERE pageId = :pageId ORDER BY createdAt ASC")
    suspend fun getShapesListForPage(pageId: String): List<NotebookShapeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShape(shape: NotebookShapeEntity)

    @Query("DELETE FROM notebook_shapes WHERE id = :shapeId")
    suspend fun deleteShapeById(shapeId: String)
}
