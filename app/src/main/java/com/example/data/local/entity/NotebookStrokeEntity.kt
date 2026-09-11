package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Database entity representing a vector-based handwriting or highlighter stroke in a notebook page.
 * Points are stored in virtual page coordinates (e.g. 0f..1000f, 0f..1414f), preserving infinite
 * vector fidelity under any zoom factor (up to 500%+).
 */
@Entity(
    tableName = "notebook_strokes",
    indices = [
        Index(value = ["pageId"]),
        Index(value = ["notebookId"])
    ]
)
data class NotebookStrokeEntity(
    @PrimaryKey
    val id: String,
    val notebookId: String,
    val pageId: String,
    val toolType: String, // "PEN" or "HIGHLIGHTER"
    val color: Long, // ARGB Color as Long
    val strokeWidth: Float, // Stroke width in dp
    val alpha: Float = 1.0f, // 1.0f for pen, ~0.38f for highlighter
    val pointsData: String, // Compact serialized string: "x1,y1,p1;x2,y2,p2;..."
    val createdAt: Long = System.currentTimeMillis()
) {
    fun parsePoints(): List<InkPoint> {
        if (pointsData.isBlank()) return emptyList()
        val parts = pointsData.split(";")
        val list = ArrayList<InkPoint>(parts.size)
        for (part in parts) {
            val coords = part.split(",")
            if (coords.size >= 2) {
                val x = coords[0].toFloatOrNull() ?: continue
                val y = coords[1].toFloatOrNull() ?: continue
                val p = if (coords.size >= 3) coords[2].toFloatOrNull() ?: 1f else 1f
                list.add(InkPoint(x, y, p))
            }
        }
        return list
    }

    companion object {
        fun serializePoints(points: List<InkPoint>): String {
            if (points.isEmpty()) return ""
            val sb = StringBuilder(points.size * 20)
            for (i in points.indices) {
                val p = points[i]
                sb.append(String.format(java.util.Locale.US, "%.2f,%.2f,%.2f", p.x, p.y, p.pressure))
                if (i < points.size - 1) {
                    sb.append(";")
                }
            }
            return sb.toString()
        }
    }
}
