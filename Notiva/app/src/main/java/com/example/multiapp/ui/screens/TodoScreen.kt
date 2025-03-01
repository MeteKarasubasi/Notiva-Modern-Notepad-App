package com.example.multiapp.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.multiapp.data.todo.Priority
import com.example.multiapp.data.todo.Todo
import com.example.multiapp.viewmodels.TodoViewModel
import org.burnoutcrew.reorderable.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodoScreen(
    viewModel: TodoViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showCompletedTasks by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    val activeTodos by viewModel.activeTodos.collectAsState(initial = emptyList())
    val completedTodos by viewModel.completedTodos.collectAsState(initial = emptyList())
    val activeTodoCount by viewModel.activeTodoCount.collectAsState(initial = 0)
    val uiState by viewModel.uiState.collectAsState()

    // Reorderable state
    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            viewModel.updateTodoOrder(from.index, to.index)
        },
        canDragOver = { draggedOver, _ -> !draggedOver.key.toString().startsWith("completed-") }
    )

    // Android 12+ için alarm izni kontrolü
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showPermissionDialog = true
            }
        }
    }

    // Hata durumu için Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Başlık ve istatistikler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Görevler",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Aktif: $activeTodoCount",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Aktif görevler
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                state = reorderState.listState,
                modifier = Modifier
                    .weight(1f)
                    .reorderable(reorderState)
            ) {
                itemsIndexed(
                    items = activeTodos,
                    key = { _, todo -> todo.id }
                ) { index, todo ->
                    ReorderableItem(
                        reorderableState = reorderState,
                        key = todo.id
                    ) { isDragging ->
                        val elevation = if (isDragging) 8.dp else 2.dp
                        TodoItem(
                            todo = todo,
                            onToggleCompletion = { viewModel.toggleTodoCompletion(todo) },
                            onDelete = { viewModel.deleteTodo(todo) },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .detectReorderAfterLongPress(reorderState)
                        )
                    }
                }
            }

            // Tamamlanan görevler bölümü
            OutlinedButton(
                onClick = { showCompletedTasks = !showCompletedTasks },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showCompletedTasks) "Tamamlananları Gizle" else "Tamamlananları Göster")
            }

            AnimatedVisibility(visible = showCompletedTasks) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp)
                ) {
                    itemsIndexed(
                        items = completedTodos,
                        key = { _, todo -> "completed-${todo.id}" }
                    ) { _, todo ->
                        TodoItem(
                            todo = todo,
                            onToggleCompletion = { viewModel.toggleTodoCompletion(todo) },
                            onDelete = { viewModel.deleteTodo(todo) }
                        )
                    }
                }
            }

            // Yeni görev ekleme butonu
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.End)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Görev Ekle")
            }
        }
    }

    // İzin dialog'u
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("İzin Gerekli") },
            text = { 
                Text("Hatırlatıcıları kullanabilmek için tam zamanlı alarm iznini vermeniz gerekmektedir.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                        showPermissionDialog = false
                    }
                ) {
                    Text("Ayarlara Git")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Yeni görev ekleme dialog'u
    if (showAddDialog) {
        AddTodoDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, description, dueDate, hasReminder, reminderTime ->
                viewModel.addTodo(
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    hasReminder = hasReminder,
                    reminderTime = reminderTime
                )
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoItem(
    todo: Todo,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggleCompletion() }
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null
                )
                if (todo.description.isNotBlank()) {
                    Text(
                        text = todo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (todo.dueDate != null) {
                    Text(
                        text = "Bitiş: ${todo.dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (todo.hasReminder) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Hatırlatıcı",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, LocalDateTime?, Boolean, LocalDateTime?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var hasReminder by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf<LocalDateTime?>(null) }
    
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    
    var selectedReminderDate by remember { mutableStateOf(LocalDateTime.now()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Yeni Görev",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Başlık alanı
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Başlık") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Title,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Açıklama alanı
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Açıklama") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bitiş tarihi seçici
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Bitiş Tarihi",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (dueDate != null)
                                    dueDate!!.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                                else
                                    "Tarih seçilmedi",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Row {
                                if (dueDate != null) {
                                    IconButton(onClick = { dueDate = null }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Tarihi Kaldır",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                IconButton(onClick = { showDueDatePicker = true }) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = "Tarih Seç",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Hatırlatıcı seçenekleri
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hatırlatıcı",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Switch(
                                checked = hasReminder,
                                onCheckedChange = { 
                                    hasReminder = it
                                    if (!it) reminderTime = null
                                }
                            )
                        }
                        
                        AnimatedVisibility(visible = hasReminder) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (reminderTime != null)
                                            reminderTime!!.format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm"))
                                        else
                                            "Hatırlatma zamanı seçilmedi",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    IconButton(onClick = { showReminderDatePicker = true }) {
                                        Icon(
                                            Icons.Default.Notifications,
                                            contentDescription = "Hatırlatıcı Ayarla",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, description, dueDate, hasReminder, reminderTime)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("İptal")
            }
        }
    )
    
    // Bitiş tarihi seçici dialog
    if (showDueDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // Seçilen tarihi milisaniyeden LocalDateTime'a dönüştür
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        
                        // Mevcut saati koru, sadece tarihi güncelle
                        dueDate = LocalDateTime.now()
                            .withYear(date.year)
                            .withMonth(date.monthValue)
                            .withDayOfMonth(date.dayOfMonth)
                    }
                    showDueDatePicker = false
                }) {
                    Text("Tamam")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) {
                    Text("İptal")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = { Text("Bitiş Tarihi Seç") }
            )
        }
    }
    
    // Hatırlatıcı tarihi seçici dialog
    if (showReminderDatePicker) {
        val reminderDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        
        DatePickerDialog(
            onDismissRequest = { showReminderDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // Seçilen tarihi milisaniyeden LocalDateTime'a dönüştür
                    reminderDatePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        
                        // Seçilen tarihi kaydet ve saat seçimine geç
                        selectedReminderDate = LocalDateTime.now()
                            .withYear(date.year)
                            .withMonth(date.monthValue)
                            .withDayOfMonth(date.dayOfMonth)
                    }
                    showReminderDatePicker = false
                    showReminderTimePicker = true
                }) {
                    Text("Devam")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDatePicker = false }) {
                    Text("İptal")
                }
            }
        ) {
            DatePicker(
                state = reminderDatePickerState,
                showModeToggle = false,
                title = { Text("Hatırlatma Tarihi Seç") }
            )
        }
    }
    
    // Hatırlatıcı saati seçici dialog
    if (showReminderTimePicker) {
        val timePickerState = rememberTimePickerState()
        
        TimePickerDialog(
            onDismissRequest = { showReminderTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // Seçilen saati al ve hatırlatma zamanını güncelle
                    val hour = timePickerState.hour
                    val minute = timePickerState.minute
                    
                    // Seçilen tarih ve saati birleştir
                    reminderTime = selectedReminderDate
                        .withHour(hour)
                        .withMinute(minute)
                        .withSecond(0)
                        .withNano(0)
                    
                    showReminderTimePicker = false
                }) {
                    Text("Tamam")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderTimePicker = false }) {
                    Text("İptal")
                }
            }
        ) {
            TimePicker(
                state = timePickerState,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable (() -> Unit),
    dismissButton: @Composable (() -> Unit),
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = content
    )
} 