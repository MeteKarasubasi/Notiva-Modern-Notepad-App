package com.example.multiapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.multiapp.ui.note.NoteScreen

@Composable
fun NotepadScreen(
    onNoteClick: (Long) -> Unit,
    onAddNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    NoteScreen(
        onNoteClick = onNoteClick,
        onAddNote = onAddNote,
        modifier = modifier
    )
} 