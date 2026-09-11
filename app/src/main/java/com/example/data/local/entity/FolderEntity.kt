package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_folders")
data class FolderEntity(
    @PrimaryKey
    val uriString: String,
    val displayName: String,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val lastScannedTimestamp: Long = System.currentTimeMillis()
)
