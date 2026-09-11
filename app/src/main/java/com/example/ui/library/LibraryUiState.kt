package com.example.ui.library

import com.example.data.local.entity.BookEntity

enum class BookFilter {
    ALL,
    PDF,
    EPUB,
    READING
}

data class LibraryUiState(
    val books: List<BookEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val scanProgressText: String? = null,
    val scannedCount: Int = 0,
    val foundCount: Int = 0,
    val searchQuery: String = "",
    val selectedFilter: BookFilter = BookFilter.ALL,
    val totalBooksCount: Int = 0,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)
