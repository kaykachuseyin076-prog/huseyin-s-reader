package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = HighlightEntity::class,
            parentColumns = ["id"],
            childColumns = ["highlightId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["highlightId"]),
        Index(value = ["bookId"])
    ]
)
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val highlightId: String,
    val bookId: String,
    val noteText: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
