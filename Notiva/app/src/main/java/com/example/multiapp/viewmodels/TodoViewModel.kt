package com.example.multiapp.viewmodels

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multiapp.data.todo.Todo
import com.example.multiapp.data.todo.TodoDao
import com.example.multiapp.notifications.TodoReminderReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import java.util.*

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoDao: TodoDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoUiState())
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    val activeTodos = todoDao.getActiveTodos()
    val completedTodos = todoDao.getCompletedTodos()
    val activeTodoCount = todoDao.getActiveTodoCount()

    fun addTodo(title: String, description: String = "", dueDate: LocalDateTime? = null, hasReminder: Boolean = false, reminderTime: LocalDateTime? = null) {
        viewModelScope.launch {
            val todo = Todo(
                title = title,
                description = description,
                dueDate = dueDate,
                hasReminder = hasReminder,
                reminderTime = reminderTime
            )
            val todoId = todoDao.insertTodo(todo)
            
            if (hasReminder && reminderTime != null) {
                scheduleReminder(todo)
            }
        }
    }

    fun toggleTodoCompletion(todo: Todo) {
        viewModelScope.launch {
            val newIsCompleted = !todo.isCompleted
            todoDao.updateTodoCompletion(todo.id, newIsCompleted, LocalDateTime.now())
            
            if (newIsCompleted && todo.hasReminder) {
                cancelReminder(todo.id)
            }
        }
    }

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            todoDao.deleteTodo(todo)
            if (todo.hasReminder) {
                cancelReminder(todo.id)
            }
        }
    }

    fun updateTodoOrder(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            try {
                val currentTodos = activeTodos.firstOrNull()
                if (currentTodos.isNullOrEmpty()) {
                    _uiState.update { it.copy(error = "Görevler listesi boş veya yüklenemedi") }
                    return@launch
                }

                val todos = currentTodos.toMutableList()
                
                // Indekslerin geçerli olduğunu kontrol et
                if (fromIndex !in todos.indices || toIndex !in todos.indices) {
                    _uiState.update { it.copy(error = "Geçersiz sıralama indeksleri") }
                    return@launch
                }

                _uiState.update { it.copy(isLoading = true) }
                
                try {
                    if (fromIndex < toIndex) {
                        for (i in fromIndex until toIndex) {
                            Collections.swap(todos, i, i + 1)
                        }
                    } else {
                        for (i in fromIndex downTo toIndex + 1) {
                            Collections.swap(todos, i, i - 1)
                        }
                    }
                    
                    // Sıralama değerlerini veritabanında güncelle
                    todos.forEachIndexed { index, todo ->
                        todoDao.updateTodoOrder(todo.id, index)
                    }
                    
                    _uiState.update { it.copy(error = null) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Sıralama güncellenirken bir hata oluştu: ${e.localizedMessage}") }
                } finally {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = "Görevler yüklenirken bir hata oluştu: ${e.localizedMessage}",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun scheduleReminder(todo: Todo) {
        if (todo.reminderTime == null) return
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TodoReminderReceiver::class.java).apply {
            putExtra("todoId", todo.id)
            putExtra("todoTitle", todo.title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todo.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    todo.reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    pendingIntent
                )
            } else {
                // Kullanıcıyı ayarlar sayfasına yönlendir
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        } else {
            // Android 12'den önceki sürümler için
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                todo.reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                pendingIntent
            )
        }
    }

    private fun cancelReminder(todoId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TodoReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todoId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

data class TodoUiState(
    val isLoading: Boolean = false,
    val error: String? = null
) 