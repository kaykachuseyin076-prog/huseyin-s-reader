package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: String)

    @Query("DELETE FROM notes WHERE highlightId = :highlightId")
    suspend fun deleteNoteByHighlightId(highlightId: String)

    @Query("SELECT * FROM notes WHERE highlightId = :highlightId LIMIT 1")
    suspend fun getNoteByHighlightId(highlightId: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE highlightId = :highlightId LIMIT 1")
    fun getNoteFlowByHighlightId(highlightId: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY createdAt ASC")
    fun getNotesForBook(bookId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>
}
