package com.example.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ReaderTheme(val displayName: String) {
    LIGHT("Açık"),
    SEPIA("Sepya"),
    DARK("Koyu");

    val backgroundColor: Color
        get() = when (this) {
            LIGHT -> Color(0xFFFBFBF9)
            SEPIA -> Color(0xFFF5EEDA)
            DARK -> Color(0xFF141518)
        }

    val textColor: Color
        get() = when (this) {
            LIGHT -> Color(0xFF191C1E)
            SEPIA -> Color(0xFF3F3124)
            DARK -> Color(0xFFE2E3E8)
        }

    val secondaryTextColor: Color
        get() = when (this) {
            LIGHT -> Color(0xFF74777F)
            SEPIA -> Color(0xFF7C6B5A)
            DARK -> Color(0xFF8F9199)
        }

    val surfaceColor: Color
        get() = when (this) {
            LIGHT -> Color(0xFFF1F3F8)
            SEPIA -> Color(0xFFEAE1CB)
            DARK -> Color(0xFF1E2024)
        }

    val borderColor: Color
        get() = when (this) {
            LIGHT -> Color(0xFFE1E2E6)
            SEPIA -> Color(0xFFDFD4BA)
            DARK -> Color(0xFF2C2E33)
        }

    val accentColor: Color
        get() = when (this) {
            LIGHT -> Color(0xFF005AC1)
            SEPIA -> Color(0xFF8D4F1E)
            DARK -> Color(0xFFA8C7FA)
        }
}

enum class ReaderFontFamily(val displayName: String) {
    SERIF("Serif (Kitap)"),
    SANS_SERIF("Sans-Serif (Modern)"),
    MONOSPACE("Monospace (Daktilo)");

    val composeFontFamily: FontFamily
        get() = when (this) {
            SERIF -> FontFamily.Serif
            SANS_SERIF -> FontFamily.SansSerif
            MONOSPACE -> FontFamily.Monospace
        }
}

data class ReaderSettings(
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val fontSizeSp: Float = 17f,
    val lineSpacingMultiplier: Float = 1.55f,
    val horizontalPaddingDp: Int = 24,
    val fontFamily: ReaderFontFamily = ReaderFontFamily.SERIF,
    val showProgressAsPercentage: Boolean = false
) {
    val fontSize: TextUnit get() = fontSizeSp.sp
    val lineHeight: TextUnit get() = (fontSizeSp * lineSpacingMultiplier).sp
    val horizontalPadding: Dp get() = horizontalPaddingDp.dp
}

/**
 * Navigation item for Table of Contents (İçindekiler)
 */
data class TocItem(
    val title: String,
    val targetIndex: Int,
    val isChapter: Boolean = true
)

/**
 * Search occurrence inside book text
 */
data class BookSearchResult(
    val targetIndex: Int,
    val targetTitle: String,
    val snippet: String,
    val matchWord: String
)

/**
 * Architecture model preparing for future Highlights, Notes, and Bookmark positions
 * (Aşama 3 & Aşama 4 için hazır mimari)
 */
data class ReadingLocator(
    val bookId: String,
    val format: String,
    val chapterOrPageIndex: Int,
    val paragraphIndex: Int = 0,
    val startOffset: Int = 0,
    val endOffset: Int = 0,
    val selectedText: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
