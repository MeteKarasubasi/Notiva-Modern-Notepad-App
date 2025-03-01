package com.example.multiapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multiapp.data.note.Note
import com.example.multiapp.data.note.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.UPDATED_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val notes = combine(
        _sortOrder,
        _searchQuery
    ) { sortOrder, query ->
        Pair(sortOrder, query)
    }.flatMapLatest { (sortOrder, query) ->
        when {
            query.isNotBlank() -> repository.searchNotes(query)
            else -> when (sortOrder) {
                SortOrder.CREATED_ASC -> repository.getAllNotesOrderByCreatedAtAsc()
                SortOrder.CREATED_DESC -> repository.getAllNotesOrderByCreatedAtDesc()
                SortOrder.TITLE_ASC -> repository.getAllNotesOrderByTitleAsc()
                SortOrder.UPDATED_DESC -> repository.getAllNotes()
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    suspend fun getNoteById(id: Long): Note? {
        return repository.getNoteById(id)
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addNote(title: String, content: String) = viewModelScope.launch {
        val note = Note(
            title = title,
            content = content,
            color = android.graphics.Color.WHITE,
            priority = com.example.multiapp.data.note.Priority.MEDIUM,
            hasReminder = false
        )
        repository.insertNote(note)
    }

    fun addNote(note: Note) = viewModelScope.launch {
        repository.insertNote(note)
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        repository.updateNote(note)
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        repository.deleteNote(note)
    }
}

enum class SortOrder {
    CREATED_ASC,
    CREATED_DESC,
    TITLE_ASC,
    UPDATED_DESC
} 