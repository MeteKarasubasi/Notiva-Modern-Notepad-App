package com.example.multiapp.data

import com.example.multiapp.data.model.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Son mesajları takip eden ve analiz eden sınıf.
 * Bu sınıf, son 5 mesajı saklar ve bu mesajlara dayalı analizler yapar.
 */
@Singleton
class MessageHistoryTracker @Inject constructor() {
    
    // Son 5 mesajı saklayan liste
    private val messageHistory = mutableListOf<ChatMessage>()
    
    // Maksimum mesaj geçmişi boyutu
    private val MAX_HISTORY_SIZE = 5
    
    /**
     * Yeni bir mesajı geçmişe ekler.
     * @param message Eklenecek mesaj
     */
    fun addMessage(message: ChatMessage) {
        messageHistory.add(message)
        
        // Geçmiş boyutu maksimum boyutu aşarsa, en eski mesajı çıkar
        if (messageHistory.size > MAX_HISTORY_SIZE) {
            messageHistory.removeAt(0)
        }
    }
    
    /**
     * Son mesajları döndürür.
     * @return Son mesajların listesi
     */
    fun getRecentMessages(): List<ChatMessage> {
        return messageHistory.toList()
    }
    
    /**
     * Son kullanıcı mesajlarını döndürür.
     * @return Son kullanıcı mesajlarının listesi
     */
    fun getRecentUserMessages(): List<ChatMessage> {
        return messageHistory.filter { it.isFromUser }
    }
    
    /**
     * Son bot mesajlarını döndürür.
     * @return Son bot mesajlarının listesi
     */
    fun getRecentBotMessages(): List<ChatMessage> {
        return messageHistory.filter { !it.isFromUser }
    }
    
    /**
     * Son mesajlarda belirli bir anahtar kelime olup olmadığını kontrol eder.
     * @param keyword Aranacak anahtar kelime
     * @return Anahtar kelime bulunursa true, bulunmazsa false
     */
    fun containsKeyword(keyword: String): Boolean {
        return messageHistory.any { 
            it.content.lowercase().contains(keyword.lowercase()) 
        }
    }
    
    /**
     * Son mesajlarda belirli bir konunun geçip geçmediğini kontrol eder.
     * @param topics Kontrol edilecek konular listesi
     * @return Konu bulunursa true, bulunmazsa false
     */
    fun containsTopics(topics: List<String>): Boolean {
        return topics.any { topic ->
            containsKeyword(topic)
        }
    }
    
    /**
     * Son mesajlarda en sık geçen kelimeleri bulur.
     * @param excludeCommonWords Yaygın kelimeleri hariç tutmak için true
     * @return En sık geçen kelimelerin haritası (kelime -> sıklık)
     */
    fun getMostFrequentWords(excludeCommonWords: Boolean = true): Map<String, Int> {
        val wordCounts = mutableMapOf<String, Int>()
        val commonWords = if (excludeCommonWords) {
            setOf("ve", "veya", "ile", "bu", "şu", "o", "bir", "için", "gibi", "de", "da", "ki", "ne", "mi", "mı", "mu", "mü")
        } else {
            emptySet()
        }
        
        messageHistory.forEach { message ->
            message.content
                .lowercase()
                .replace(Regex("[^\\w\\sğüşıöçĞÜŞİÖÇ]"), " ")
                .split(Regex("\\s+"))
                .filter { it.length > 2 && it !in commonWords }
                .forEach { word ->
                    wordCounts[word] = (wordCounts[word] ?: 0) + 1
                }
        }
        
        return wordCounts.toList().sortedByDescending { it.second }.toMap()
    }
    
    /**
     * Son mesajlardaki konuyu tahmin eder.
     * @return Tahmin edilen konu
     */
    fun detectTopic(): String {
        val frequentWords = getMostFrequentWords()
        
        // Konu kategorileri ve ilgili anahtar kelimeler
        val topicKeywords = mapOf(
            "matematik" to listOf("hesapla", "çöz", "sonuç", "formül", "denklem", "integral", "türev", "limit"),
            "hava durumu" to listOf("hava", "sıcaklık", "yağmur", "kar", "nem", "rüzgar", "güneş", "fırtına"),
            "genel bilgi" to listOf("nedir", "kimdir", "ne zaman", "nerede", "nasıl", "anlat", "açıkla", "tarih")
        )
        
        // Her konu için puan hesapla
        val topicScores = mutableMapOf<String, Int>()
        
        topicKeywords.forEach { (topic, keywords) ->
            var score = 0
            keywords.forEach { keyword ->
                if (containsKeyword(keyword)) {
                    score += 1
                }
            }
            topicScores[topic] = score
        }
        
        // En yüksek puanlı konuyu döndür
        return topicScores.maxByOrNull { it.value }?.key ?: "genel"
    }
    
    /**
     * Mesaj geçmişini temizler.
     */
    fun clearHistory() {
        messageHistory.clear()
    }
} 