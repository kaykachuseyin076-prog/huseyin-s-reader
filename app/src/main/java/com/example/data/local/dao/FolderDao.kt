package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM scanned_folders ORDER BY addedTimestamp DESC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("DELETE FROM scanned_folders WHERE uriString = :uriString")
    suspend fun deleteFolder(uriString: String)

    @Query("SELECT * FROM scanned_folders")
    suspend fun getAllFoldersList(): List<FolderEntity>
}
