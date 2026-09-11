package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: String, // Typically URI string or unique identifier
    val uriString: String,
    val title: String,
    val author: String? = null,
    val format: String, // "PDF" or "EPUB"
    val fileSize: Long = 0L,
    val coverPath: String? = null,
    val currentPage: Int = 0, // 0-based page for PDF, chapter index for EPUB
    val totalPages: Int = 0, // Total pages for PDF, total chapters for EPUB
    val lastReadProgress: Float = 0f, // 0.0 to 1.0
    val lastReadTimestamp: Long = 0L,
    val addedTimestamp: Long = System.currentTimeMillis()
)
