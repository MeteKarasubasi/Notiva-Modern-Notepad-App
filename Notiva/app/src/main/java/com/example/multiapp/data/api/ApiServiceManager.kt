package com.example.multiapp.data.api

import android.util.Log
import com.example.multiapp.BuildConfig
import com.example.multiapp.data.MessageHistoryTracker
import com.example.multiapp.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API servislerini yöneten sınıf.
 * Bu sınıf, API çağrılarını yönetir, yeniden deneme ve yedek mekanizmaları içerir.
 */
@Singleton
class ApiServiceManager @Inject constructor(
    private val weatherService: WeatherService,
    private val wikipediaService: WikipediaService,
    private val geocodingService: GeocodingService,
    private val geminiService: GeminiService,
    private val apiKeyManager: ApiKeyManager,
    private val messageHistoryTracker: MessageHistoryTracker
) {
    
    companion object {
        // Maksimum yeniden deneme sayısı
        private const val MAX_RETRY_COUNT = 2
        private const val TAG = "ApiServiceManager"
    }
    
    /**
     * Hava durumu sorgusunu işler.
     * @param query Sorgu metni
     * @return Hava durumu yanıtı
     */
    suspend fun queryWeather(query: String): String {
        return try {
            // Şehir adını çıkar
            val cityName = extractCityName(query)
            
            // Şehir koordinatlarını bul
            val coordinates = getCoordinates(cityName)
            if (coordinates == null) {
                return "Üzgünüm, belirtilen şehir için konum bilgisi bulunamadı."
            }
            
            println("DEBUG: Hava durumu sorgusu: şehir=$cityName, lat=${coordinates.first}, lon=${coordinates.second}")
            
            // API isteği yap
            val response = weatherService.getWeather(
                latitude = coordinates.first,
                longitude = coordinates.second
            )
            
            if (!response.isSuccessful) {
                println("DEBUG: Hava durumu API hatası: ${response.code()} - ${response.errorBody()?.string()}")
                return "Hava durumu bilgisi alınamadı. Lütfen daha sonra tekrar deneyin."
            }
            
            // Yanıtı işle
            val weatherData = response.body()
            if (weatherData != null && weatherData.properties.timeseries.isNotEmpty()) {
                val current = weatherData.properties.timeseries[0]
                val instant = current.data.instant.details
                val next1h = current.data.next_1_hours
                
                """
                ${cityName.capitalize()} için hava durumu:
                Sıcaklık: ${instant.air_temperature}°C
                Nem: ${instant.relative_humidity}%
                Rüzgar Hızı: ${instant.wind_speed} m/s
                Rüzgar Yönü: ${instant.wind_from_direction}°
                Bulutluluk: ${instant.cloud_area_fraction}%
                ${if (next1h?.details?.precipitation_amount != null) "\nYağış Miktarı (1 saat): ${next1h.details.precipitation_amount} mm" else ""}
                Durum: ${next1h?.summary?.symbol_code ?: "Bilinmiyor"}
                """.trimIndent()
            } else {
                "Üzgünüm, hava durumu bilgisi bulunamadı."
            }
            
        } catch (e: Exception) {
            println("DEBUG: Hava durumu sorgusu hatası: ${e.message}")
            "Hava durumu bilgisi alınırken bir hata oluştu. Lütfen daha sonra tekrar deneyin."
        }
    }
    
    /**
     * Wikipedia sorgusunu işler.
     * @param query Sorgu metni
     * @return Wikipedia yanıtı
     */
    suspend fun queryWikipedia(query: String): String {
        return try {
            // Arama terimini hazırla
            val searchTerm = prepareWikipediaSearchTerm(query)
            
            println("DEBUG: Wikipedia sorgusu: $searchTerm")
            
            // API isteği yap
            val response = wikipediaService.getPageSummary(searchTerm)
            
            if (!response.isSuccessful) {
                println("DEBUG: Wikipedia API hatası: ${response.code()} - ${response.errorBody()?.string()}")
                apiKeyManager.markApiError(ApiKeyManager.ApiType.WIKIPEDIA)
                return "Wikipedia'dan bilgi alınamadı. Lütfen daha sonra tekrar deneyin."
            }
            
            // Yanıtı işle
            val wikiData = response.body()
            if (wikiData != null && wikiData.extract.isNotBlank()) {
                """
                ${wikiData.title}:
                ${wikiData.extract}
                """.trimIndent()
            } else {
                "Üzgünüm, aradığınız bilgi Wikipedia'da bulunamadı."
            }
            
        } catch (e: Exception) {
            println("DEBUG: Wikipedia sorgusu hatası: ${e.message}")
            apiKeyManager.markApiError(ApiKeyManager.ApiType.WIKIPEDIA)
            "Wikipedia'dan bilgi alınırken bir hata oluştu. Lütfen daha sonra tekrar deneyin."
        }
    }
    
    /**
     * Gemini sorgusunu işler.
     * @param query Sorgu metni
     * @return Gemini yanıtı
     */
    suspend fun queryGemini(query: String): String {
        return try {
            val cleanedQuery = cleanMessage(query)
            if (cleanedQuery.isBlank()) {
                return "Üzgünüm, boş bir mesaj aldım. Lütfen bir soru sorun veya mesaj yazın."
            }

            Log.d(TAG, "Gemini sorgusu başlatılıyor: $cleanedQuery")

            val history = messageHistoryTracker.getRecentMessages()
            val prompt = buildGeminiPrompt(cleanedQuery, history)
            val apiKey = apiKeyManager.getGeminiApiKey() ?: return "API anahtarı bulunamadı. Lütfen API anahtarını kontrol edin."

            // Initialize Gemini service with API key
            geminiService.initialize(apiKey)

            var retryCount = 0
            var lastError: Exception? = null

            while (retryCount < MAX_RETRY_COUNT) {
                try {
                    Log.d(TAG, "Gemini API isteği yapılıyor (Deneme ${retryCount + 1})")

                    val response = geminiService.generateContent(prompt)
                    if (response.isBlank()) {
                        throw Exception("Boş yanıt alındı")
                    }

                    Log.d(TAG, "Gemini yanıt başarılı")
                    return response.trim()

                } catch (e: Exception) {
                    lastError = e
                    Log.e(TAG, "Gemini hatası (Deneme ${retryCount + 1}): ${e.message}")
                    retryCount++
                    
                    if (retryCount < MAX_RETRY_COUNT) {
                        kotlinx.coroutines.delay(1000L * retryCount)
                    }
                }
            }

            Log.e(TAG, "Gemini tüm denemeler başarısız: ${lastError?.message}")
            apiKeyManager.markApiError(ApiKeyManager.ApiType.GEMINI)
            
            "Üzgünüm, şu anda yanıt veremiyorum. Teknik bir sorun oluştu: ${lastError?.message ?: "Bilinmeyen hata"}"

        } catch (e: Exception) {
            Log.e(TAG, "Gemini beklenmeyen hata: ${e.message}")
            apiKeyManager.markApiError(ApiKeyManager.ApiType.GEMINI)
            "Üzgünüm, beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyin."
        }
    }
    
    /**
     * Sorgudan şehir adını çıkarır.
     * @param query Sorgu metni
     * @return Şehir adı
     */
    private fun extractCityName(query: String): String {
        // Şehir adını bulmak için yaygın kalıpları kontrol et
        val patterns = listOf(
            "(.*?)(?:ilinde|ilinin|ilimizin|şehrinde|şehrinin|'da|'de|'ta|'te|kentinde|kentinin).*?hava.*",
            ".*?hava.*?(durumu|nasıl).*?(.*?)(?:ilinde|ilinin|ilimizin|şehrinde|şehrinin|'da|'de|'ta|'te|kentinde|kentinin)",
            "(.*?)(?:'nin|'nın|'nun|'nün).*?hava.*"
        )
        
        for (pattern in patterns) {
            val regex = Regex(pattern)
            val matchResult = regex.find(query.lowercase())
            if (matchResult != null) {
                val city = matchResult.groupValues[1].trim()
                if (city.isNotBlank()) return city
            }
        }
        
        // Eğer kalıp bulunamazsa, basit kelime eşleştirmesi yap
        val cityKeywords = listOf("istanbul", "ankara", "izmir", "bursa", "antalya", "adana", "konya", "gaziantep", "şanlıurfa", "mersin")
        for (city in cityKeywords) {
            if (query.lowercase().contains(city)) {
                return city
            }
        }
        
        // Varsayılan olarak "istanbul" döndür
        return "istanbul"
    }
    
    /**
     * Wikipedia araması için terimi hazırlar.
     * @param query Sorgu metni
     * @return Arama terimi
     */
    private fun prepareWikipediaSearchTerm(query: String): String {
        // "nedir", "kimdir" gibi soru kalıplarını kaldır
        return query.lowercase()
            .replace(Regex("(nedir|kimdir|ne demek|nerede|ne zaman|kim|kimin|hangi)(\\?)?"), "")
            .replace(Regex("hakkında|konusunda|bilgi"), "")
            .trim()
    }
    
    /**
     * Mesajı temizler ve formatlar.
     * @param message Temizlenecek mesaj
     * @return Temizlenmiş mesaj
     */
    private fun cleanMessage(message: String): String {
        return message
            .split("\n") // Satırlara ayır
            .map { it.trim() } // Her satırı temizle
            .filter { it.isNotBlank() } // Boş satırları kaldır
            .joinToString(" ") // Tek satır haline getir
            .replace(Regex("\\s+"), " ") // Fazla boşlukları tek boşluğa çevir
            .trim() // Baştaki ve sondaki boşlukları temizle
    }
    
    /**
     * Gemini için istek metnini oluşturur.
     * @param query Sorgu metni
     * @param history Mesaj geçmişi
     * @return İstek metni
     */
    private fun buildGeminiPrompt(query: String, history: List<ChatMessage>): String {
        val prompt = StringBuilder()
        
        // Son 5 mesajı ekle
        history.takeLast(5).forEach { message ->
            val cleanedContent = cleanMessage(message.content)
            if (cleanedContent.isNotBlank()) {
                prompt.append("${if (message.isFromUser) "Human" else "Assistant"}: $cleanedContent\n")
            }
        }
        
        // Yeni soruyu ekle
        prompt.append("Human: $query\nAssistant:")
        
        return prompt.toString()
    }

    private suspend fun getCoordinates(cityName: String): Pair<Double, Double>? {
        try {
            val response = geocodingService.searchLocation(
                query = "$cityName, Turkey"
            )
            
            if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
                val location = response.body()!![0]
                return Pair(location.lat.toDouble(), location.lon.toDouble())
            }
        } catch (e: Exception) {
            println("DEBUG: Geocoding hatası: ${e.message}")
        }
        return null
    }
} 