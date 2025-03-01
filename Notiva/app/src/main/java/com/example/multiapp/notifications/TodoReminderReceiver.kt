package com.example.multiapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.multiapp.R

class TodoReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra("todoId", -1)
        val todoTitle = intent.getStringExtra("todoTitle") ?: "Görev"
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Notification channel oluştur (Android 8.0 ve üzeri için gerekli)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Görev Hatırlatıcıları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Görev hatırlatıcıları için bildirimler"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Bildirimi oluştur
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Görev Hatırlatıcısı")
            .setContentText(todoTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        // Bildirimi göster
        notificationManager.notify(todoId.toInt(), notification)
    }
    
    companion object {
        const val CHANNEL_ID = "todo_reminders"
    }
} 