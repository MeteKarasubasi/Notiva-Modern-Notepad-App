package com.example.multiapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.multiapp.data.note.Note
import com.example.multiapp.data.todo.Todo
import com.example.multiapp.viewmodels.NoteViewModel
import com.example.multiapp.viewmodels.TodoViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Ana ekran composable'ı.
 * Bu ekran, uygulamanın ana ekranını oluşturur ve diğer ekranlara geçiş için butonlar içerir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChatbot: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToTodo: () -> Unit,
    noteViewModel: NoteViewModel = hiltViewModel(),
    todoViewModel: TodoViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    var recentNotes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var pendingTodos by remember { mutableStateOf<List<Todo>>(emptyList()) }
    var weatherInfo by remember { mutableStateOf("Hava durumu bilgisi yükleniyor...") }
    var quoteOfDay by remember { mutableStateOf("Günün sözü yükleniyor...") }
    
    // Verileri yükle
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            // Son 3 notu al
            noteViewModel.notes.firstOrNull()?.let { allNotes ->
                recentNotes = allNotes.sortedByDescending { it.updatedAt }.take(3)
            }
            
            // Tamamlanmamış görevleri al
            todoViewModel.activeTodos.firstOrNull()?.let { allTodos ->
                pendingTodos = allTodos.filter { !it.isCompleted }.take(3)
            }
            
            // Günün sözü
            quoteOfDay = getRandomQuote()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notiva") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Karşılama metni
            Text(
                text = "Hoş Geldiniz!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Günün sözü kartı
            QuoteCard(quote = quoteOfDay)
            
            // Ana navigasyon butonları
            HomeScreenButton(
                icon = Icons.AutoMirrored.Filled.Chat,
                text = "Chatbot",
                onClick = onNavigateToChatbot
            )
            
            HomeScreenButton(
                icon = Icons.AutoMirrored.Filled.Note,
                text = "Notlar",
                onClick = onNavigateToNotes
            )
            
            HomeScreenButton(
                icon = Icons.Filled.CheckCircle,
                text = "Yapılacaklar",
                onClick = onNavigateToTodo
            )
            
            // Son notlar bölümü
            if (recentNotes.isNotEmpty()) {
                SectionTitle(title = "Son Notlar", icon = Icons.AutoMirrored.Filled.Note)
                
                recentNotes.forEach { note ->
                    NotePreviewCard(
                        title = note.title,
                        content = note.content,
                        date = note.updatedAt,
                        color = Color(note.color),
                        onClick = { onNavigateToNotes() }
                    )
                }
            }
            
            // Yapılacaklar bölümü
            if (pendingTodos.isNotEmpty()) {
                SectionTitle(title = "Yapılacaklar", icon = Icons.Filled.CheckCircle)
                
                pendingTodos.forEach { todo ->
                    TodoPreviewCard(
                        title = todo.title,
                        isCompleted = todo.isCompleted,
                        dueDate = todo.dueDate,
                        onClick = { onNavigateToTodo() }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun NotePreviewCard(
    title: String,
    content: String,
    date: LocalDateTime,
    color: Color,
    onClick: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.2f)
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = date.format(formatter),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun TodoPreviewCard(
    title: String,
    isCompleted: Boolean,
    dueDate: LocalDateTime?,
    onClick: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (dueDate != null) {
                    Text(
                        text = "Son tarih: ${dueDate.format(formatter)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Detaylar",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun QuoteCard(quote: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.FormatQuote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = quote,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/**
 * Ana ekranda kullanılan buton composable'ı.
 * Bu buton, bir ikon, metin ve ileri ok ikonu içerir.
 */
@Composable
fun HomeScreenButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "İleri",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Rastgele günün sözü
private fun getRandomQuote(): String {
    val quotes = listOf(
        "Başarı, her gün küçük adımlar atarak başlar.",
        "Hayatta en büyük başarı, düştükten sonra tekrar ayağa kalkmaktır.",
        "Bugün yaptığın seçimler, yarının şeklini belirler.",
        "Hayallerinizin peşinden koşmak için asla geç değildir.",
        "Zorluklar, sizi daha güçlü yapan fırsatlardır.",
        "Başarı, hazırlık ve fırsat buluştuğunda ortaya çıkar.",
        "Kendinize inanın, gerisi kendiliğinden gelecektir.",
        "Her gün yeni bir başlangıçtır.",
        "Küçük adımlar, büyük değişimlere yol açar.",
        "Hayat, yaşadığınız anların toplamıdır."
    )
    return quotes.random()
} 