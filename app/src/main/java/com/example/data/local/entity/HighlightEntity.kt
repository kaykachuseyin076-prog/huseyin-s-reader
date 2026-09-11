package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["bookId", "chapterOrPageIndex"])
    ]
)
data class HighlightEntity(
    @PrimaryKey
    val id: String,
    val bookId: String,
    val format: String, // "EPUB" or "PDF"
    val chapterOrPageIndex: Int,
    val paragraphIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String,
    val colorId: String, // "YELLOW", "ORANGE", "GREEN", "BLUE", "PINK"
    val normLeft: Float = 0f,
    val normTop: Float = 0f,
    val normRight: Float = 0f,
    val normBottom: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)
