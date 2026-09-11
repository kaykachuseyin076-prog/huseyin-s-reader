package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.HighlightEntity
import com.example.data.local.entity.HighlightWithNote
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity)

    @Query("DELETE FROM highlights WHERE id = :highlightId")
    suspend fun deleteHighlight(highlightId: String)

    @Query("DELETE FROM highlights WHERE bookId = :bookId")
    suspend fun deleteAllHighlightsForBook(bookId: String)

    @Query("SELECT * FROM highlights WHERE id = :id LIMIT 1")
    suspend fun getHighlightById(id: String): HighlightEntity?

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY createdAt ASC")
    fun getHighlightsForBook(bookId: String): Flow<List<HighlightEntity>>

    @Transaction
    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY createdAt ASC")
    fun getHighlightsWithNotesForBook(bookId: String): Flow<List<HighlightWithNote>>

    @Transaction
    @Query("SELECT * FROM highlights WHERE bookId = :bookId AND chapterOrPageIndex = :chapterOrPageIndex ORDER BY createdAt ASC")
    fun getHighlightsWithNotesForLocation(bookId: String, chapterOrPageIndex: Int): Flow<List<HighlightWithNote>>
}
