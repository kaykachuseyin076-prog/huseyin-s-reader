package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BookDao
import com.example.data.local.dao.DiscussionDao
import com.example.data.local.dao.FolderDao
import com.example.data.local.dao.HighlightDao
import com.example.data.local.dao.InkStrokeDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.NotebookDao
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.DiscussionEntity
import com.example.data.local.entity.DiscussionMessageEntity
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.HighlightEntity
import com.example.data.local.entity.InkStrokeEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.NotebookEntity
import com.example.data.local.entity.NotebookPageEntity
import com.example.data.local.entity.NotebookShapeEntity
import com.example.data.local.entity.NotebookStrokeEntity
import com.example.data.local.entity.NotebookTextBoxEntity

@Database(
    entities = [
        BookEntity::class,
        FolderEntity::class,
        HighlightEntity::class,
        NoteEntity::class,
        InkStrokeEntity::class,
        DiscussionEntity::class,
        DiscussionMessageEntity::class,
        NotebookEntity::class,
        NotebookPageEntity::class,
        NotebookStrokeEntity::class,
        NotebookTextBoxEntity::class,
        NotebookShapeEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun folderDao(): FolderDao
    abstract fun highlightDao(): HighlightDao
    abstract fun noteDao(): NoteDao
    abstract fun inkStrokeDao(): InkStrokeDao
    abstract fun discussionDao(): DiscussionDao
    abstract fun notebookDao(): NotebookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reader_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
