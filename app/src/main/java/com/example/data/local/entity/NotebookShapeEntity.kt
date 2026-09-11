package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Geometric vector shape (line, arrow, rectangle, circle) on a notebook page.
 */
@Entity(
    tableName = "notebook_shapes",
    indices = [
        Index(value = ["pageId"])
    ]
)
data class NotebookShapeEntity(
    @PrimaryKey
    val id: String,
    val notebookId: String,
    val pageId: String,
    val shapeType: String, // LINE, ARROW, RECTANGLE, CIRCLE
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val color: Long = 0xFF1E1E1EL,
    val strokeWidth: Float = 3.0f,
    val isFilled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
