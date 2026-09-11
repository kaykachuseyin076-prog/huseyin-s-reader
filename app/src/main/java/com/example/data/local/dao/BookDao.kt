package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY CASE WHEN lastReadTimestamp > 0 THEN lastReadTimestamp ELSE addedTimestamp END DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE format = :format ORDER BY CASE WHEN lastReadTimestamp > 0 THEN lastReadTimestamp ELSE addedTimestamp END DESC")
    fun getBooksByFormat(format: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: String): BookEntity?

    @Query("SELECT * FROM books")
    suspend fun getAllBooksList(): List<BookEntity>

    @Query("SELECT * FROM books WHERE uriString = :uriString LIMIT 1")
    suspend fun getBookByUri(uriString: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(books: List<BookEntity>)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE books SET currentPage = :currentPage, totalPages = :totalPages, lastReadProgress = :progress, lastReadTimestamp = :timestamp WHERE id = :id")
    suspend fun updateReadingProgress(id: String, currentPage: Int, totalPages: Int, progress: Float, timestamp: Long)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: String)

    @Query("DELETE FROM books WHERE id IN (:ids)")
    suspend fun deleteBooksByIds(ids: List<String>)

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int
}
