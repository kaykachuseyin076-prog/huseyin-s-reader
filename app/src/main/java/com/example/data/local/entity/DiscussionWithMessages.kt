package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class DiscussionWithMessages(
    @Embedded
    val discussion: DiscussionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "discussionId"
    )
    val messages: List<DiscussionMessageEntity>
)
