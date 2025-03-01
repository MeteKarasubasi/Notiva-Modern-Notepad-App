package com.example.multiapp.data.note

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getAllNotesOrderByCreatedAtAsc(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllNotesOrderByCreatedAtDesc(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY title ASC")
    fun getAllNotesOrderByTitleAsc(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    fun searchNotes(query: String): Flow<List<Note>>
} 