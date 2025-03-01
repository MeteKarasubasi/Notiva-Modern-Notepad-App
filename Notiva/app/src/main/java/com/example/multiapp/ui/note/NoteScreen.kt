package com.example.multiapp.ui.note

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.multiapp.data.note.Note
import com.example.multiapp.viewmodels.NoteViewModel
import com.example.multiapp.viewmodels.SortOrder
import java.time.format.DateTimeFormatter
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.time.LocalDateTime
import com.example.multiapp.data.note.Priority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    onNoteClick: (Long) -> Unit,
    onAddNote: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        SearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::setSearchQuery,
            onSortClick = { showSortDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        if (notes.isEmpty() && searchQuery.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Henüz not eklenmemiş",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes) { note ->
                    NoteItem(
                        note = note,
                        onClick = { onNoteClick(note.id) },
                        onDelete = { viewModel.deleteNote(note) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddNote,
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Not Ekle")
        }
    }

    if (showAddNoteDialog) {
        AddEditNoteDialog(
            onDismiss = { showAddNoteDialog = false },
            onSave = { title, content ->
                val newNote = Note(
                    id = 0,
                    title = title,
                    content = content,
                    color = Color.White.toArgb(),
                    priority = Priority.MEDIUM,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
                viewModel.addNote(newNote)
                showAddNoteDialog = false
            }
        )
    }

    if (showSortDialog) {
        SortDialog(
            currentSortOrder = sortOrder,
            onSortOrderSelected = { order ->
                viewModel.setSortOrder(order)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier,
        placeholder = { Text("Notlarda ara...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onSortClick) {
                Icon(Icons.Default.Sort, contentDescription = "Sırala")
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteItem(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val backgroundColor = Color(note.color)
    
    // Kırmızı ve mavi tonları için özel kontrol
    val isRedOrBlue = (backgroundColor.red > 0.7f && backgroundColor.green < 0.5f) || // Kırmızı tonları
                      (backgroundColor.blue > 0.7f && backgroundColor.green < 0.5f)    // Mavi tonları
    
    // Genel parlaklık kontrolü
    val brightness = ((backgroundColor.red * 299) + 
                     (backgroundColor.green * 587) + 
                     (backgroundColor.blue * 114)) / 1000
    val isLightColor = if (isRedOrBlue) false else brightness > 0.5
    
    val textColor = if (isLightColor) Color.Black else Color.White
    val iconTint = if (isLightColor) MaterialTheme.colorScheme.primary else Color.White

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textColor
                    )
                    if (note.hasReminder) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Hatırlatıcı var",
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { 
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TITLE, note.title)
                                putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Notu Paylaş"))
                        }
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Paylaş",
                            tint = iconTint
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = if (isLightColor) MaterialTheme.colorScheme.error else Color.White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Son güncelleme: ${note.updatedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}",
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SortDialog(
    currentSortOrder: SortOrder,
    onSortOrderSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sıralama") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortOption(
                    text = "Son güncellemeye göre",
                    selected = currentSortOrder == SortOrder.UPDATED_DESC,
                    onClick = { onSortOrderSelected(SortOrder.UPDATED_DESC) }
                )
                SortOption(
                    text = "Oluşturma tarihi (Yeni)",
                    selected = currentSortOrder == SortOrder.CREATED_DESC,
                    onClick = { onSortOrderSelected(SortOrder.CREATED_DESC) }
                )
                SortOption(
                    text = "Oluşturma tarihi (Eski)",
                    selected = currentSortOrder == SortOrder.CREATED_ASC,
                    onClick = { onSortOrderSelected(SortOrder.CREATED_ASC) }
                )
                SortOption(
                    text = "Başlığa göre",
                    selected = currentSortOrder == SortOrder.TITLE_ASC,
                    onClick = { onSortOrderSelected(SortOrder.TITLE_ASC) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

@Composable
fun SortOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(text = text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteDialog(
    note: Note? = null,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note == null) "Not Ekle" else "Notu Düzenle") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Başlık") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("İçerik") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, content) },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
} 