package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a page within a digital notebook.
 */
@Entity(
    tableName = "notebook_pages",
    indices = [
        Index(value = ["notebookId", "pageIndex"]),
        Index(value = ["notebookId"])
    ]
)
data class NotebookPageEntity(
    @PrimaryKey
    val id: String,
    val notebookId: String,
    val pageIndex: Int,
    val template: String = "BLANK", // BLANK, LINED, GRID, DOTTED, CORNELL, LECTURE, BOOK_NOTE, SUMMARY
    val zoomScale: Float = 1.0f,
    val panOffsetX: Float = 0.0f,
    val panOffsetY: Float = 0.0f,
    val bookPageNumber: Int? = null, // Optional future link to a specific book page
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
