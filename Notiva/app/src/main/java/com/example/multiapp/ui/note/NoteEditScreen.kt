package com.example.multiapp.ui.note

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.multiapp.data.note.Note
import com.example.multiapp.data.note.Priority
import com.example.multiapp.viewmodels.NoteViewModel
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.multiapp.ui.components.ColorPicker
import com.example.multiapp.ui.components.PriorityPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Long? = null,
    onBack: () -> Unit,
    viewModel: NoteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var existingNote by remember { mutableStateOf<Note?>(null) }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var reminderDateTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showPriorityPicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDateTime?>(null) }

    val dateDialogState = rememberMaterialDialogState()
    val timeDialogState = rememberMaterialDialogState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            createNotificationChannel(context)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(context, permission) -> {
                    createNotificationChannel(context)
                }
                else -> {
                    notificationPermissionLauncher.launch(permission)
                }
            }
        }
    }

    LaunchedEffect(noteId) {
        if (noteId != null) {
            viewModel.getNoteById(noteId)?.let { note ->
                existingNote = note
                title = note.title
                content = note.content
                selectedColor = Color(note.color)
                selectedPriority = note.priority
                reminderDateTime = note.reminderTime
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null || noteId == 0L) "Yeni Not" else "Notu Düzenle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val note = if (noteId == null || noteId == 0L) {
                                Note(
                                    id = 0,
                                    title = title,
                                    content = content,
                                    color = selectedColor.toArgb(),
                                    priority = selectedPriority,
                                    reminderTime = reminderDateTime,
                                    hasReminder = reminderDateTime != null
                                )
                            } else {
                                existingNote?.copy(
                                    title = title,
                                    content = content,
                                    color = selectedColor.toArgb(),
                                    priority = selectedPriority,
                                    reminderTime = reminderDateTime,
                                    hasReminder = reminderDateTime != null,
                                    updatedAt = LocalDateTime.now()
                                ) ?: Note(
                                    id = noteId,
                                    title = title,
                                    content = content,
                                    color = selectedColor.toArgb(),
                                    priority = selectedPriority,
                                    reminderTime = reminderDateTime,
                                    hasReminder = reminderDateTime != null
                                )
                            }
                            
                            if (noteId == null || noteId == 0L) {
                                viewModel.addNote(note)
                            } else {
                                viewModel.updateNote(note)
                            }
                            onBack()
                        },
                        enabled = title.isNotBlank() && content.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Save, "Kaydet")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Başlık") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("İçerik") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Renk seçici
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Not Rengi")
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .clickable { showColorPicker = true }
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }

            if (showColorPicker) {
                ColorPicker(
                    onColorSelected = { color ->
                        selectedColor = color
                        showColorPicker = false
                        existingNote?.let { note ->
                            viewModel.updateNote(note.copy(color = color.toArgb()))
                        }
                    },
                    onDismiss = { showColorPicker = false }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Öncelik seçici
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Öncelik")
                AssistChip(
                    onClick = { showPriorityPicker = true },
                    label = { Text(selectedPriority.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = when (selectedPriority) {
                                Priority.LOW -> Icons.Default.ArrowDownward
                                Priority.MEDIUM -> Icons.Default.Remove
                                Priority.HIGH -> Icons.Default.ArrowUpward
                            },
                            contentDescription = null
                        )
                    }
                )
            }

            if (showPriorityPicker) {
                PriorityPicker(
                    currentPriority = selectedPriority,
                    onPrioritySelected = { priority ->
                        selectedPriority = priority
                        showPriorityPicker = false
                    },
                    onDismiss = { showPriorityPicker = false }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hatırlatıcı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hatırlatıcı")
                if (reminderDateTime != null) {
                    AssistChip(
                        onClick = { dateDialogState.show() },
                        label = { 
                            Text(
                                reminderDateTime?.format(
                                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                                ) ?: ""
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Notifications, null) },
                        trailingIcon = {
                            IconButton(
                                onClick = { reminderDateTime = null }
                            ) {
                                Icon(Icons.Default.Clear, "Hatırlatıcıyı Kaldır")
                            }
                        }
                    )
                } else {
                    FilledTonalIconButton(
                        onClick = { dateDialogState.show() }
                    ) {
                        Icon(Icons.Default.AddAlarm, "Hatırlatıcı Ekle")
                    }
                }
            }
        }
    }

    // Hatırlatıcı Dialog'ları
    MaterialDialog(
        dialogState = dateDialogState,
        buttons = {
            positiveButton(text = "Tamam")
            negativeButton(text = "İptal")
        }
    ) {
        datepicker(
            title = "Tarih Seçin"
        ) { date ->
            selectedDate = LocalDateTime.of(
                date.year,
                date.monthValue,
                date.dayOfMonth,
                0,
                0
            )
            timeDialogState.show()
        }
    }

    MaterialDialog(
        dialogState = timeDialogState,
        buttons = {
            positiveButton(text = "Tamam")
            negativeButton(text = "İptal")
        }
    ) {
        timepicker(
            title = "Saat Seçin",
            is24HourClock = true
        ) { time ->
            if (selectedDate != null) {
                reminderDateTime = selectedDate?.withHour(time.hour)?.withMinute(time.minute)
            }
        }
    }
}

@Composable
fun ColorPicker(
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        Color.White,
        Color.Red,        // Ana Renk - Kırmızı
        Color.Blue,       // Ana Renk - Mavi
        Color.Yellow,     // Ana Renk - Sarı
        Color(0xFFFF4081), // Ara Renk - Pembe
        Color(0xFF9C27B0), // Ara Renk - Mor
        Color(0xFF00BCD4), // Ara Renk - Turkuaz
        Color(0xFF4CAF50), // Ana Renk - Yeşil
        Color(0xFFFF9800), // Ara Renk - Turuncu
        Color(0xFF795548), // Ara Renk - Kahverengi
        Color(0xFF607D8B), // Ara Renk - Gri
        Color(0xFF009688)  // Ara Renk - Deniz Yeşili
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renk Seç") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    colors.take(6).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { onColorSelected(color) }
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    colors.drop(6).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { onColorSelected(color) }
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Not Hatırlatıcıları"
        val descriptionText = "Not uygulaması hatırlatıcıları"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("note_reminders", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
} 