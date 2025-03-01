package com.example.multiapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.multiapp.data.dao.ChatMessageDao
import com.example.multiapp.data.model.ChatMessage
import com.example.multiapp.data.todo.Todo
import com.example.multiapp.data.todo.TodoDao
import com.example.multiapp.data.note.Note
import com.example.multiapp.data.note.NoteDao
import com.example.multiapp.util.Converters

@Database(
    entities = [
        ChatMessage::class,
        Todo::class,
        Note::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun todoDao(): TodoDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 