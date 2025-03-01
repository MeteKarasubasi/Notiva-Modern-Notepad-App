package com.example.multiapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multiapp.data.MessageHistoryTracker
import com.example.multiapp.data.QueryDetector
import com.example.multiapp.data.api.ApiServiceManager
import com.example.multiapp.data.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Chatbot ekranı için ViewModel.
 * Bu sınıf, kullanıcı mesajlarını işler ve uygun API'lere yönlendirir.
 */
@HiltViewModel
class ChatbotViewModel @Inject constructor(
    private val apiServiceManager: ApiServiceManager,
    private val queryDetector: QueryDetector,
    private val messageHistoryTracker: MessageHistoryTracker
) : ViewModel() {
    
    // API seçim durumu
    private val _selectedApi = MutableStateFlow(ApiType.DIALOGPT)
    val selectedApi: StateFlow<ApiType> = _selectedApi.asStateFlow()
    
    // Mesaj listesi
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    // Yükleniyor durumu
    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * API tipini değiştirir
     * @param apiType Yeni API tipi
     */
    fun setApiType(apiType: ApiType) {
        _selectedApi.value = apiType
        // API değiştiğinde sohbeti temizle
        clearMessages()
    }
    
    /**
     * Kullanıcı mesajını işler.
     * @param content Kullanıcı mesajı
     */
    fun handleUserMessage(content: String) {
        if (content.isBlank()) return
        
        // Kullanıcı mesajını ekle
        val userMessage = ChatMessage(content = content, isFromUser = true)
        addMessage(userMessage)
        
        // Mesajı geçmişe ekle
        messageHistoryTracker.addMessage(userMessage)
        
        // Sorguyu işle
        _isLoading.value = true
        viewModelScope.launch {
            val response = handleQuery(content)
            
            // Bot yanıtını ekle
            val botMessage = ChatMessage(content = response, isFromUser = false)
            addMessage(botMessage)
            
            // Mesajı geçmişe ekle
            messageHistoryTracker.addMessage(botMessage)
            
            _isLoading.value = false
        }
    }
    
    /**
     * Tüm mesajları temizler.
     */
    fun clearMessages() {
        _messages.value = emptyList()
        messageHistoryTracker.clearHistory()
    }
    
    /**
     * Sorguyu işler ve seçili API'ye yönlendirir.
     * @param query Sorgu metni
     * @return API yanıtı
     */
    private suspend fun handleQuery(query: String): String {
        return withContext(Dispatchers.IO) {
            // Seçili API'ye göre sorguyu yönlendir
            when (selectedApi.value) {
                ApiType.WEATHER -> {
                    apiServiceManager.queryWeather(query)
                }
                ApiType.WIKIPEDIA -> {
                    apiServiceManager.queryWikipedia(query)
                }
                ApiType.DIALOGPT -> {
                    apiServiceManager.queryGemini(query)
                }
            }
        }
    }
    
    /**
     * Mesaj listesine yeni mesaj ekler.
     * @param message Eklenecek mesaj
     */
    private fun addMessage(message: ChatMessage) {
        val currentMessages = _messages.value
        _messages.value = currentMessages + message
    }
    
    /**
     * Kullanılabilir API tipleri
     */
    enum class ApiType(val title: String, val description: String) {
        WEATHER("Hava Durumu", "Hava durumu sorgulama"),
        WIKIPEDIA("Wikipedia", "Bilgi içeren sorular"),
        DIALOGPT("Genel", "Genel sorgular")
    }
} 