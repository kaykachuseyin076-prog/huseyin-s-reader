package com.example.ui.reader

import androidx.compose.ui.graphics.Color

enum class HighlightColor(
    val id: String,
    val displayName: String,
    val lineColor: Color,
    val dotColor: Color,
    val postItBackground: Color,
    val postItBorder: Color,
    val postItHeaderColor: Color,
    val chipColor: Color
) {
    YELLOW(
        id = "YELLOW",
        displayName = "Sarı",
        lineColor = Color(0xFFF59E0B),
        dotColor = Color(0xFFF59E0B),
        postItBackground = Color(0xFFFFFDE7),
        postItBorder = Color(0xFFFFEE58),
        postItHeaderColor = Color(0xFFFDD835),
        chipColor = Color(0xFFFFD54F)
    ),
    ORANGE(
        id = "ORANGE",
        displayName = "Turuncu",
        lineColor = Color(0xFFEA580C),
        dotColor = Color(0xFFEA580C),
        postItBackground = Color(0xFFFFF3E0),
        postItBorder = Color(0xFFFFB74D),
        postItHeaderColor = Color(0xFFFF9800),
        chipColor = Color(0xFFFF9800)
    ),
    GREEN(
        id = "GREEN",
        displayName = "Yeşil",
        lineColor = Color(0xFF16A34A),
        dotColor = Color(0xFF16A34A),
        postItBackground = Color(0xFFE8F5E9),
        postItBorder = Color(0xFFA5D6A7),
        postItHeaderColor = Color(0xFF66BB6A),
        chipColor = Color(0xFF81C784)
    ),
    BLUE(
        id = "BLUE",
        displayName = "Mavi",
        lineColor = Color(0xFF2563EB),
        dotColor = Color(0xFF2563EB),
        postItBackground = Color(0xFFE1F5FE),
        postItBorder = Color(0xFF81D4FA),
        postItHeaderColor = Color(0xFF42A5F5),
        chipColor = Color(0xFF64B5F6)
    ),
    PINK(
        id = "PINK",
        displayName = "Pembe",
        lineColor = Color(0xFFDB2777),
        dotColor = Color(0xFFDB2777),
        postItBackground = Color(0xFFFCE4EC),
        postItBorder = Color(0xFFF48FB1),
        postItHeaderColor = Color(0xFFEC407A),
        chipColor = Color(0xFFF06292)
    );

    companion object {
        fun fromId(id: String?): HighlightColor {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ORANGE
        }
    }
}
