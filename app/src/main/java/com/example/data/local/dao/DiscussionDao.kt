package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.DiscussionEntity
import com.example.data.local.entity.DiscussionMessageEntity
import com.example.data.local.entity.DiscussionWithMessages
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscussionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscussion(discussion: DiscussionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: DiscussionMessageEntity)

    @Transaction
    @Query("SELECT * FROM discussions WHERE highlightId = :highlightId LIMIT 1")
    fun getDiscussionByHighlightIdFlow(highlightId: String): Flow<DiscussionWithMessages?>

    @Transaction
    @Query("SELECT * FROM discussions WHERE highlightId = :highlightId LIMIT 1")
    suspend fun getDiscussionByHighlightId(highlightId: String): DiscussionWithMessages?

    @Transaction
    @Query("SELECT * FROM discussions WHERE (:highlightId IS NOT NULL AND highlightId = :highlightId) OR (bookId = :bookId AND selectedText = :selectedText) ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getDiscussionByHighlightOrText(highlightId: String?, bookId: String, selectedText: String): DiscussionWithMessages?

    @Transaction
    @Query("SELECT * FROM discussions WHERE id = :discussionId LIMIT 1")
    suspend fun getDiscussionById(discussionId: String): DiscussionWithMessages?

    @Transaction
    @Query("SELECT * FROM discussions WHERE id = :discussionId LIMIT 1")
    fun getDiscussionByIdFlow(discussionId: String): Flow<DiscussionWithMessages?>

    @Query("UPDATE discussions SET updatedAt = :updatedAt WHERE id = :discussionId")
    suspend fun updateTimestamp(discussionId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM discussions WHERE id = :discussionId")
    suspend fun deleteDiscussion(discussionId: String)

    @Query("DELETE FROM discussions WHERE highlightId = :highlightId")
    suspend fun deleteByHighlightId(highlightId: String)

    @Query("SELECT COUNT(*) FROM discussions WHERE highlightId = :highlightId")
    suspend fun hasDiscussionForHighlight(highlightId: String): Int
}
