package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.InkStrokeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InkStrokeDao {

    @Query("""
        SELECT * FROM ink_strokes 
        WHERE bookId = :bookId AND format = :format AND pageOrChapter = :pageOrChapter 
        ORDER BY createdAt ASC
    """)
    fun getStrokesForPage(bookId: String, format: String, pageOrChapter: Int): Flow<List<InkStrokeEntity>>

    @Query("SELECT * FROM ink_strokes WHERE bookId = :bookId ORDER BY pageOrChapter ASC, createdAt ASC")
    fun getStrokesForBook(bookId: String): Flow<List<InkStrokeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStroke(stroke: InkStrokeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStrokes(strokes: List<InkStrokeEntity>)

    @Query("DELETE FROM ink_strokes WHERE id = :id")
    suspend fun deleteStrokeById(id: String)

    @Query("DELETE FROM ink_strokes WHERE id IN (:ids)")
    suspend fun deleteStrokesByIds(ids: List<String>)

    @Query("DELETE FROM ink_strokes WHERE bookId = :bookId AND format = :format AND pageOrChapter = :pageOrChapter")
    suspend fun deleteStrokesForPage(bookId: String, format: String, pageOrChapter: Int)

    @Query("DELETE FROM ink_strokes WHERE bookId = :bookId")
    suspend fun deleteStrokesForBook(bookId: String)
}
