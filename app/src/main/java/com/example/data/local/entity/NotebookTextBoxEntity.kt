package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Text box placed on a notebook page in virtual page coordinates.
 */
@Entity(
    tableName = "notebook_text_boxes",
    indices = [
        Index(value = ["pageId"])
    ]
)
data class NotebookTextBoxEntity(
    @PrimaryKey
    val id: String,
    val notebookId: String,
    val pageId: String,
    val text: String,
    val x: Float, // Virtual page coordinate
    val y: Float, // Virtual page coordinate
    val width: Float = 260f,
    val height: Float = 100f,
    val fontSizeSp: Float = 16f,
    val isBold: Boolean = false,
    val color: Long = 0xFF1E1E1EL,
    val createdAt: Long = System.currentTimeMillis()
)
