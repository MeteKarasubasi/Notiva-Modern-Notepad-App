package com.example.multiapp.data.todo

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY orderIndex ASC")
    fun getActiveTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE isCompleted = 1 ORDER BY createdAt DESC")
    fun getCompletedTodos(): Flow<List<Todo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: Todo): Long

    @Update
    suspend fun updateTodo(todo: Todo)

    @Delete
    suspend fun deleteTodo(todo: Todo)

    @Query("UPDATE todos SET orderIndex = :newIndex WHERE id = :todoId")
    suspend fun updateTodoOrder(todoId: Long, newIndex: Int)

    @Query("SELECT * FROM todos WHERE hasReminder = 1 AND reminderTime > :currentTime")
    suspend fun getUpcomingReminders(currentTime: LocalDateTime): List<Todo>

    @Query("UPDATE todos SET isCompleted = :isCompleted, completedAt = CASE WHEN :isCompleted = 1 THEN :currentTime ELSE NULL END WHERE id = :todoId")
    suspend fun updateTodoCompletion(todoId: Long, isCompleted: Boolean, currentTime: LocalDateTime)

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 0")
    fun getActiveTodoCount(): Flow<Int>
} 