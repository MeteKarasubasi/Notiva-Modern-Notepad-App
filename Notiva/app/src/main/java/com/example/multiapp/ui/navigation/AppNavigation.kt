package com.example.multiapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.multiapp.ui.screens.ChatbotScreen
import com.example.multiapp.ui.screens.HomeScreen
import com.example.multiapp.ui.screens.NotepadScreen
import com.example.multiapp.ui.screens.TodoScreen
import com.example.multiapp.ui.note.NoteDetailScreen
import com.example.multiapp.ui.note.NoteEditScreen

/**
 * Uygulama navigasyonunu yöneten composable.
 * Bu composable, uygulama içindeki ekranlar arası geçişleri yönetir.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToChatbot = { navController.navigate("chatbot") },
                onNavigateToNotes = { navController.navigate("notes") },
                onNavigateToTodo = { navController.navigate("todo") }
            )
        }
        
        composable("chatbot") {
            ChatbotScreen(
                onNavigateToHome = { navController.popBackStack() }
            )
        }
        
        composable("notes") {
            NotepadScreen(
                onNoteClick = { noteId ->
                    navController.navigate("note_detail/$noteId") 
                },
                onAddNote = {
                    navController.navigate("note_edit/0") 
                },
                modifier = androidx.compose.ui.Modifier
            )
        }
        
        composable("note_detail/{noteId}") { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")?.toLongOrNull() ?: 0L
            NoteDetailScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("note_edit/$noteId") }
            )
        }
        
        composable("note_edit/{noteId}") { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")?.toLongOrNull() ?: 0L
            NoteEditScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("todo") {
            TodoScreen(
                modifier = androidx.compose.ui.Modifier
            )
        }
    }
} 