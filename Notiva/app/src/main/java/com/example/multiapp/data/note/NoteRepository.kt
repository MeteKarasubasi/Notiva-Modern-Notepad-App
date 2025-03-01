package com.example.multiapp.data.note

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    fun getAllNotesOrderByCreatedAtAsc(): Flow<List<Note>> = noteDao.getAllNotesOrderByCreatedAtAsc()

    fun getAllNotesOrderByCreatedAtDesc(): Flow<List<Note>> = noteDao.getAllNotesOrderByCreatedAtDesc()

    fun getAllNotesOrderByTitleAsc(): Flow<List<Note>> = noteDao.getAllNotesOrderByTitleAsc()

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)
} 