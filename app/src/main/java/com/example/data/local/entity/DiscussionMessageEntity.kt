package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "discussion_messages",
    foreignKeys = [
        ForeignKey(
            entity = DiscussionEntity::class,
            parentColumns = ["id"],
            childColumns = ["discussionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["discussionId"])
    ]
)
data class DiscussionMessageEntity(
    @PrimaryKey
    val id: String,
    val discussionId: String,
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
