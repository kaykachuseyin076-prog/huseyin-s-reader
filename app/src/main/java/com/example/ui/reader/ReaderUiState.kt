package com.example.ui.reader

import android.graphics.Bitmap
import com.example.data.local.entity.BookEntity
import com.example.util.EpubHelper

data class ReaderUiState(
    val book: BookEntity? = null,
    val format: String = "PDF",
    val currentPage: Int = 0,
    val totalPages: Int = 1,
    val isLoading: Boolean = true,
    val pdfPageBitmap: Bitmap? = null,
    val pdfPageText: String = "",
    val epubChapter: EpubHelper.ChapterContent? = null,
    val errorMessage: String? = null,

    // Controls visibility (tap screen toggles)
    val areControlsVisible: Boolean = false,

    // Reading Settings & Theme
    val settings: ReaderSettings = ReaderSettings(),

    // Navigation & Table of Contents (İçindekiler)
    val tocItems: List<TocItem> = emptyList(),
    val isTocVisible: Boolean = false,

    // In-Book Search (Metin Arama)
    val isSearchVisible: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<BookSearchResult> = emptyList(),
    val isSearching: Boolean = false,

    // Settings Modal
    val isSettingsVisible: Boolean = false,

    // Go to Page Dialog (Sayfaya Git)
    val isGoToPageVisible: Boolean = false,

    // ==========================================
    // HIGHLIGHT & POST-IT STATE (AŞAMA 3)
    // ==========================================
    val highlights: List<com.example.data.local.entity.HighlightWithNote> = emptyList(),
    val bookNotebooks: List<com.example.data.local.entity.NotebookEntity> = emptyList(),
    val inkPages: List<Int> = emptyList(),

    // Active Post-it Note Modal
    val activePostItHighlight: com.example.data.local.entity.HighlightEntity? = null,
    val activePostItNote: com.example.data.local.entity.NoteEntity? = null,
    val isPostItOpen: Boolean = false,

    // Text Selection & Highlight Creation
    val isSelectionSheetOpen: Boolean = false,
    val selectedTextForHighlight: String = "",
    val selectedParagraphIndex: Int = 0,
    val selectedStartOffset: Int = 0,
    val selectedEndOffset: Int = 0,
    val fullParagraphText: String = "",
    val selectedNormLeft: Float = 0f,
    val selectedNormTop: Float = 0f,
    val selectedNormRight: Float = 0f,
    val selectedNormBottom: Float = 0f,

    // Contextual floating toolbar
    val isFloatingToolbarVisible: Boolean = false,
    val floatingSelectedColor: HighlightColor = HighlightColor.ORANGE,
    val isHighlightsOverviewVisible: Boolean = false,

    // Feedback & Undo
    val snackbarMessage: String? = null,
    val undoDeletedHighlight: com.example.data.local.entity.HighlightEntity? = null,
    val undoDeletedNote: com.example.data.local.entity.NoteEntity? = null,

    // ==========================================
    // INK & TABLET PEN STATE (AŞAMA 4)
    // ==========================================
    val isPenModeActive: Boolean = false,
    val activeInkTool: com.example.ui.reader.ink.InkToolType = com.example.ui.reader.ink.InkToolType.PEN,
    val selectedInkColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
    val selectedInkThickness: com.example.ui.reader.ink.PenThickness = com.example.ui.reader.ink.PenThickness.MEDIUM,
    val stylusOnlyMode: Boolean = true,
    val currentPageStrokes: List<com.example.ui.reader.ink.RenderableStroke> = emptyList(),
    val canUndoStroke: Boolean = false,
    val canRedoStroke: Boolean = false,

    // ==========================================
    // GEMINI AI INTEGRATION STATE (AŞAMA 5)
    // ==========================================
    val isAiTranslateVisible: Boolean = false,
    val isAiHelpVisible: Boolean = false,
    val isAiDiscussionVisible: Boolean = false,
    val isGeminiSettingsVisible: Boolean = false,
    val aiActiveSelectedText: String = "",
    val aiActiveHighlightId: String? = null
) {
    val aiSelectedText: String get() = aiActiveSelectedText
    val aiHighlightId: String? get() = aiActiveHighlightId

    val progressPercentage: Int
        get() = if (totalPages > 0) {
            ((currentPage + 1).toFloat() / totalPages.toFloat() * 100).toInt().coerceIn(1, 100)
        } else 0
}
