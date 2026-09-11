package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an independent digital notebook or a book-linked study notebook.
 */
@Entity(
    tableName = "notebooks",
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["highlightId"])
    ]
)
data class NotebookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val defaultTemplate: String = "BLANK", // BLANK, LINED, GRID, DOTTED, CORNELL, LECTURE, BOOK_NOTE, SUMMARY
    val bookId: String? = null, // Link to a specific book
    val bookTitle: String? = null, // Title of the source book for instant breadcrumb rendering
    val pdfPage: Int? = null, // 0-indexed page in PDF
    val chapterIndex: Int? = null, // 0-indexed chapter in EPUB
    val chapterTitle: String? = null, // Section/chapter label
    val highlightId: String? = null, // Optional link to a specific highlight
    val sourceSnippet: String? = null, // Quoted text or passage preview
    val pageCount: Int = 1,
    val coverColor: Long = 0xFF1976D2L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
