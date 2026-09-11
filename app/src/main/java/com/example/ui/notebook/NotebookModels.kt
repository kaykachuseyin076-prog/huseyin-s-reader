package com.example.ui.notebook

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.InkPoint
import com.example.data.local.entity.NotebookShapeEntity
import com.example.data.local.entity.NotebookStrokeEntity
import com.example.data.local.entity.NotebookTextBoxEntity

enum class NotebookTool(val displayName: String) {
    PEN("Kalem"),
    HIGHLIGHTER("Fosforlu"),
    ERASER("Silgi"),
    LASSO("Lasso"),
    HAND("El / Kaydır"),
    TEXT("Metin"),
    SHAPE("Şekil")
}

enum class NotebookShapeType(val displayName: String) {
    LINE("Çizgi"),
    ARROW("Ok"),
    RECTANGLE("Dikdörtgen"),
    CIRCLE("Daire")
}

enum class NotebookPenThickness(
    val displayName: String,
    val penDp: Dp,
    val highlighterDp: Dp,
    val penVirtualPx: Float,
    val highlighterVirtualPx: Float
) {
    THIN("İnce", 2.5.dp, 16.dp, 3.5f, 22f),
    MEDIUM("Orta", 5.0.dp, 26.dp, 7.0f, 36f),
    THICK("Kalın", 9.0.dp, 38.dp, 12.0f, 54f)
}

object NotebookPalettes {
    val PEN_COLORS = listOf(
        InkColorOption("black", "Siyah", Color(0xFF1E1E1E), 0xFF1E1E1EL),
        InkColorOption("red", "Kırmızı", Color(0xFFD32F2F), 0xFFD32F2FL),
        InkColorOption("blue", "Mavi", Color(0xFF1976D2), 0xFF1976D2L),
        InkColorOption("green", "Yeşil", Color(0xFF388E3C), 0xFF388E3CL),
        InkColorOption("orange", "Turuncu", Color(0xFFF57C00), 0xFFF57C00L)
    )

    val HIGHLIGHTER_COLORS = listOf(
        InkColorOption("yellow", "Sarı", Color(0xFFFFD54F), 0xFFFFD54FL),
        InkColorOption("orange", "Turuncu", Color(0xFFFFB74D), 0xFFFFB74DL),
        InkColorOption("green", "Yeşil", Color(0xFF81C784), 0xFF81C784L),
        InkColorOption("blue", "Mavi", Color(0xFF64B5F6), 0xFF64B5F6L),
        InkColorOption("pink", "Pembe", Color(0xFFF06292), 0xFFF06292L)
    )
}

data class InkColorOption(
    val id: String,
    val name: String,
    val color: Color,
    val colorLong: Long
)

/**
 * State of the active Lasso selection on the canvas.
 */
data class LassoSelection(
    val strokeIds: Set<String>,
    val boundingBox: Rect,
    val currentOffset: Offset = Offset.Zero
)

/**
 * Action recorded on the undo/redo stack.
 */
sealed class NotebookUndoAction {
    data class AddStroke(val stroke: NotebookStrokeEntity) : NotebookUndoAction()
    data class EraseStrokes(val strokes: List<NotebookStrokeEntity>) : NotebookUndoAction()
    data class MoveStrokes(
        val oldPoints: Map<String, List<InkPoint>>,
        val newStrokes: List<NotebookStrokeEntity>
    ) : NotebookUndoAction()
    data class AddTextBox(val textBox: NotebookTextBoxEntity) : NotebookUndoAction()
    data class DeleteTextBox(val textBox: NotebookTextBoxEntity) : NotebookUndoAction()
    data class AddShape(val shape: NotebookShapeEntity) : NotebookUndoAction()
    data class DeleteShape(val shape: NotebookShapeEntity) : NotebookUndoAction()
}
