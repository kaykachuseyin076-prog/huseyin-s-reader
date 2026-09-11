package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Point in a freehand ink stroke with optional pressure and timestamp.
 * Coordinates (x, y) are stored as normalized coordinates (0.0f .. 1.0f)
 * relative to the page or chapter content bounds, ensuring resolution,
 * orientation, and zoom independence.
 */
data class InkPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

/**
 * Database entity representing a vector-based handwriting or highlighter stroke.
 * Preserves the exact mathematical points rather than rasterizing to a bitmap,
 * enabling lossless scaling, stroke-based erasing, undo/redo, and color editing.
 */
@Entity(
    tableName = "ink_strokes",
    indices = [
        Index(value = ["bookId", "pageOrChapter"]),
        Index(value = ["bookId"])
    ]
)
data class InkStrokeEntity(
    @PrimaryKey
    val id: String,
    val bookId: String,
    val format: String, // "PDF" or "EPUB"
    val pageOrChapter: Int,
    val toolType: String, // "PEN" or "HIGHLIGHTER"
    val color: Long, // ARGB Color as Long (e.g. 0xFF1A1A1AL)
    val strokeWidth: Float, // Normalized stroke width in dp
    val alpha: Float = 1.0f, // 1.0f for pen, ~0.4f for highlighter
    val pointsData: String, // Compact serialized string: "x1,y1,p1;x2,y2,p2;..."
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Fast parser for the compact points representation.
     */
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
        /**
         * Fast serializer for ink points into compact format.
         */
        fun serializePoints(points: List<InkPoint>): String {
            if (points.isEmpty()) return ""
            val sb = StringBuilder(points.size * 20)
            for (i in points.indices) {
                val p = points[i]
                sb.append(String.format(java.util.Locale.US, "%.5f,%.5f,%.2f", p.x, p.y, p.pressure))
                if (i < points.size - 1) {
                    sb.append(";")
                }
            }
            return sb.toString()
        }
    }
}
