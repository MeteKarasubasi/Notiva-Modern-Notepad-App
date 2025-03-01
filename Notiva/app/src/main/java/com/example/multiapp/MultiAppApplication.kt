package com.example.multiapp

import android.app.Application
import com.example.multiapp.data.dao.ChatMessageDao
import com.example.multiapp.data.model.ChatMessage
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MultiAppApplication : Application() {
    
    @Inject
    lateinit var chatMessageDao: ChatMessageDao
    
    override fun onCreate() {
        super.onCreate()
        
        // Uygulama başlatıldığında sohbeti temizle
        clearChatOnStartup()
    }
    
    private fun clearChatOnStartup() {
        CoroutineScope(Dispatchers.IO).launch {
            // Mevcut mesaj sayısını kontrol et
            val messageCount = chatMessageDao.getMessageCount()
            
            // Eğer mesaj yoksa veya birden fazla mesaj varsa temizle
            if (messageCount != 1) {
                // Tüm mesajları sil
                chatMessageDao.deleteAllMessages()
                
                // Hoş geldiniz mesajını ekle
                val welcomeMessage = ChatMessage(
                    content = "Merhaba! Size nasıl yardımcı olabilirim?\n\nNot: Farklı bir konuda bilgi almak için lütfen sohbeti temizleyiniz.",
                    isFromUser = false
                )
                chatMessageDao.insertMessage(welcomeMessage)
            }
        }
    }
} 