package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class HighlightWithNote(
    @Embedded
    val highlight: HighlightEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "highlightId"
    )
    val note: NoteEntity?
) {
    val hasNote: Boolean get() = note != null && note.noteText.isNotBlank()
}
