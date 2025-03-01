package com.example.multiapp.data.todo

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null,
    val dueDate: LocalDateTime? = null,
    val hasReminder: Boolean = false,
    val reminderTime: LocalDateTime? = null,
    val priority: Priority = Priority.MEDIUM,
    val orderIndex: Int = 0
)

enum class Priority {
    LOW, MEDIUM, HIGH
} 