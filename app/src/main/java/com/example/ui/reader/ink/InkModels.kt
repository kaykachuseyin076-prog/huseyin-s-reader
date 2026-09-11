package com.example.ui.reader.ink

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class InkToolType(val displayName: String) {
    PEN("Kalem"),
    HIGHLIGHTER("Fosforlu"),
    ERASER("Silgi")
}

enum class PenThickness(val displayName: String, val penDp: Dp, val highlighterDp: Dp) {
    THIN("İnce", 2.5.dp, 14.dp),
    MEDIUM("Orta", 5.0.dp, 22.dp),
    THICK("Kalın", 9.0.dp, 34.dp)
}

data class InkColorOption(
    val id: String,
    val name: String,
    val color: Color
)

object InkPalettes {
    val PEN_COLORS = listOf(
        InkColorOption("black", "Siyah", Color(0xFF1E1E1E)),
        InkColorOption("red", "Kırmızı", Color(0xFFD32F2F)),
        InkColorOption("blue", "Mavi", Color(0xFF1976D2)),
        InkColorOption("green", "Yeşil", Color(0xFF388E3C)),
        InkColorOption("orange", "Turuncu", Color(0xFFF57C00))
    )

    val HIGHLIGHTER_COLORS = listOf(
        InkColorOption("yellow", "Sarı", Color(0xFFFFD54F)),
        InkColorOption("orange", "Turuncu", Color(0xFFFFB74D)),
        InkColorOption("green", "Yeşil", Color(0xFF81C784)),
        InkColorOption("blue", "Mavi", Color(0xFF64B5F6)),
        InkColorOption("pink", "Pembe", Color(0xFFF06292))
    )
}

/**
 * In-memory representation of an active or rendered stroke.
 */
data class RenderableStroke(
    val id: String,
    val toolType: InkToolType,
    val color: Color,
    val strokeWidthDp: Float,
    val alpha: Float,
    val points: List<com.example.data.local.entity.InkPoint>,
    val pageOrChapter: Int = -1
) {
    companion object {
        fun fromEntity(entity: com.example.data.local.entity.InkStrokeEntity): RenderableStroke {
            val tool = try {
                InkToolType.valueOf(entity.toolType)
            } catch (_: Exception) {
                InkToolType.PEN
            }
            return RenderableStroke(
                id = entity.id,
                toolType = tool,
                color = Color(entity.color.toULong()),
                strokeWidthDp = entity.strokeWidth,
                alpha = entity.alpha,
                points = entity.parsePoints(),
                pageOrChapter = entity.pageOrChapter
            )
        }
    }
}

/**
 * Action recorded on the undo/redo stack.
 */
sealed class InkAction {
    data class Add(val stroke: RenderableStroke) : InkAction()
    data class Erase(val strokes: List<RenderableStroke>) : InkAction()
}
