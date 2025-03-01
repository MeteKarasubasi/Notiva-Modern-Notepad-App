package com.example.multiapp.data.note

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class Priority {
    LOW, MEDIUM, HIGH
}

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val isArchived: Boolean = false,
    val color: Int = Color.White.toArgb(),
    val priority: Priority = Priority.MEDIUM,
    val reminderTime: LocalDateTime? = null,
    val hasReminder: Boolean = false
) 