package com.example.multiapp.ui.note

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
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
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.multiapp.ui.components.ColorPicker
import com.example.multiapp.ui.components.PriorityPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: NoteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var note by remember { mutableStateOf<Note?>(null) }
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
        viewModel.getNoteById(noteId)?.let { loadedNote ->
            note = loadedNote
            selectedColor = Color(loadedNote.color)
            selectedPriority = loadedNote.priority
            reminderDateTime = loadedNote.reminderTime
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Not Detayı") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, "Düzenle")
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
            note?.let { currentNote ->
                Text(
                    text = currentNote.title,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentNote.content,
                    style = MaterialTheme.typography.bodyLarge
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
                            note?.let { currentNote ->
                                viewModel.updateNote(currentNote.copy(
                                    color = color.toArgb(),
                                    updatedAt = LocalDateTime.now()
                                ))
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
                            viewModel.updateNote(currentNote.copy(priority = priority))
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
                                    onClick = { 
                                        reminderDateTime = null
                                        viewModel.updateNote(currentNote.copy(
                                            reminderTime = null,
                                            hasReminder = false
                                        ))
                                    }
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
                note?.let { currentNote ->
                    viewModel.updateNote(currentNote.copy(
                        reminderTime = reminderDateTime,
                        hasReminder = true
                    ))
                }
            }
        }
    }
}

@Composable
fun PriorityPicker(
    currentPriority: Priority,
    onPrioritySelected: (Priority) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Öncelik Seç") },
        text = {
            Column {
                Priority.values().forEach { priority ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPrioritySelected(priority) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = priority == currentPriority,
                            onClick = { onPrioritySelected(priority) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = when (priority) {
                                Priority.LOW -> Icons.Default.ArrowDownward
                                Priority.MEDIUM -> Icons.Default.Remove
                                Priority.HIGH -> Icons.Default.ArrowUpward
                            },
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(priority.name)
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