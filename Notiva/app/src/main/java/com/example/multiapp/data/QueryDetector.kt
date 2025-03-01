package com.example.multiapp.data

import com.example.multiapp.data.api.ApiKeyManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sorgu tipini belirleyen sınıf.
 * Bu sınıf, mesaj içeriğini ve mesaj geçmişini analiz ederek sorgu tipini belirler.
 */
@Singleton
class QueryDetector @Inject constructor(
    private val messageHistoryTracker: MessageHistoryTracker,
    private val apiKeyManager: ApiKeyManager
) {
    
    /**
     * Verilen mesaj içeriğine ve mesaj geçmişine göre sorgu tipini belirler.
     * @param content Mesaj içeriği
     * @return Belirlenen sorgu tipi
     */
    fun detectQueryType(content: String): QueryType {
        val lowercaseContent = content.lowercase().trim()
        
        println("DEBUG: Sorgu tipi belirleniyor: '$lowercaseContent'")
        
        // API kullanılabilirlik kontrolleri
        val isGeminiAvailable = apiKeyManager.isApiAvailable(ApiKeyManager.ApiType.GEMINI)
        val isWeatherAvailable = apiKeyManager.isApiAvailable(ApiKeyManager.ApiType.WEATHER)
        val isWikipediaAvailable = apiKeyManager.isApiAvailable(ApiKeyManager.ApiType.WIKIPEDIA)
        
        // Öncelikle standart yanıtları kontrol et
        if (isStandardResponse(lowercaseContent)) {
            println("DEBUG: Sorgu tipi: STANDARD")
            return QueryType.STANDARD
        }
        
        // Gemini API kullanılabilir ise, önce Gemini'yi dene
        if (isGeminiAvailable) {
            println("DEBUG: Sorgu tipi: GEMINI (Öncelikli)")
            return QueryType.GEMINI
        }
        
        // Gemini API kullanılamıyorsa, diğer API'leri kontrol et
        return when {
            // Hava durumu sorguları
            isWeatherQuery(lowercaseContent) && isWeatherAvailable -> {
                println("DEBUG: Sorgu tipi: WEATHER (Yedek)")
                QueryType.WEATHER
            }
            
            // Wikipedia sorguları - sadece spesifik soru kalıpları için
            isWikipediaAvailable && isWikipediaSpecificQuery(lowercaseContent) -> {
                println("DEBUG: Sorgu tipi: WIKIPEDIA (Yedek)")
                QueryType.WIKIPEDIA
            }
            
            // Hiçbir API kullanılamıyorsa, standart yanıt
            else -> {
                println("DEBUG: Sorgu tipi: STANDARD (API kullanılamıyor)")
                QueryType.STANDARD
            }
        }
    }
    
    /**
     * Wikipedia için spesifik soru kalıplarını kontrol eder
     */
    private fun isWikipediaSpecificQuery(content: String): Boolean {
        // Sadece "nedir", "kimdir" gibi spesifik soru kalıpları için Wikipedia'yı kullan
        return content.matches(Regex(".*\\b(nedir|kimdir|ne demek|kim|kimin|hangi|nerede|ne zaman)\\b.*\\??")) ||
               content.matches(Regex(".*\\b(hakkında|konusunda)\\b.*bilgi.*")) ||
               content.matches(Regex(".*\\b(tanımı|anlamı|açıklaması)\\b.*"))
    }
    
    /**
     * Verilen içeriğin hava durumu sorgusu olup olmadığını kontrol eder.
     * @param content Kontrol edilecek içerik
     * @return Hava durumu sorgusu ise true, değilse false
     */
    private fun isWeatherQuery(content: String): Boolean {
        val weatherKeywords = listOf(
            "hava", "hava durumu", "yağmur", "kar", "sıcaklık", "nem", "rüzgar", "güneş", "bulut",
            "fırtına", "yağış", "derece", "hissedilen", "meteoroloji", "tahmin", "yağacak mı",
            "hava nasıl", "bugün hava", "yarın hava", "sıcak mı", "soğuk mu", "şemsiye", "don",
            "dolu", "sis", "puslu", "parçalı bulutlu", "açık hava", "kapalı hava", "gök gürültüsü",
            "şimşek", "kasırga", "tayfun", "sel", "sağanak", "lodos", "poyraz", "meltem"
        )
        
        return weatherKeywords.any { content.contains(it) } ||
               content.matches(Regex(".*(hava|yağmur|kar|sıcaklık|derece).*ne.*(olacak|olur|durumda).*")) ||
               content.matches(Regex(".*(bugün|yarın|hafta|pazartesi|salı|çarşamba|perşembe|cuma|cumartesi|pazar).*hava.*")) ||
               content.matches(Regex(".*\\w+'[dt][ae]\\s+hava.*")) ||
               content.matches(Regex(".*(kaç|ne kadar).*(derece|sıcaklık).*"))
    }
    
    /**
     * Verilen içeriğin standart bir yanıt gerektirip gerektirmediğini kontrol eder.
     * @param content Kontrol edilecek içerik
     * @return Standart yanıt gerekiyorsa true, gerekmiyorsa false
     */
    private fun isStandardResponse(content: String): Boolean {
        val greetingKeywords = listOf(
            "merhaba", "selam", "hey", "hi", "hello", "günaydın", "iyi sabahlar", 
            "iyi akşamlar", "iyi geceler", "hoşça kal", "görüşürüz", "bye", "bb", "güle güle"
        )
        
        val thankKeywords = listOf(
            "teşekkür", "teşekkürler", "sağol", "eyvallah", "eyv", "tşk", "teşekkür ederim",
            "çok teşekkürler", "sağ ol", "sağolasın"
        )
        
        return greetingKeywords.any { content.contains(it) } ||
               thankKeywords.any { content.contains(it) } ||
               content.matches(Regex(".*(nasılsın|naber|ne haber).*")) ||
               content.matches(Regex(".*(aptal|salak|mal|gerizekalı|beyinsiz).*")) ||
               content.matches(Regex(".*(saçmalık|saçma|anlamsız|boş|sacma).*")) ||
               content.matches(Regex(".*(küfür|küfr|mk|aq|amk|sg|siktir).*")) ||
               content.matches(Regex(".*(bıktım|usandım|sıkıldım|of|ahh|yapma|etme).*"))
    }
    
    /**
     * Sorgu tiplerini temsil eden enum.
     */
    enum class QueryType {
        STANDARD,
        WEATHER,
        WIKIPEDIA,
        GEMINI
    }
} 