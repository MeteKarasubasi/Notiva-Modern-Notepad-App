package com.example.multiapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.multiapp.data.database.dao.NoteDao
import com.example.multiapp.data.database.dao.TodoDao
import com.example.multiapp.data.database.entity.Note
import com.example.multiapp.data.database.entity.Todo

@Database(
    entities = [Note::class, Todo::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun todoDao(): TodoDao
} 