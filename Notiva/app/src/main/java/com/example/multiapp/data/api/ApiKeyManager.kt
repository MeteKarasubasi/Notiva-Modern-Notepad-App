package com.example.multiapp.data.api

import android.content.Context
import android.content.SharedPreferences
import com.example.multiapp.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API anahtarlarını yöneten sınıf.
 * Bu sınıf, API anahtarlarını saklar, yönetir ve API durumlarını takip eder.
 */
@Singleton
class ApiKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("api_keys", Context.MODE_PRIVATE)
    
    // API durumlarını takip etmek için
    private val apiStatus = mutableMapOf<ApiType, ApiStatus>()
    
    init {
        // API durumlarını başlat
        ApiType.values().forEach { apiType ->
            apiStatus[apiType] = ApiStatus()
        }
        
        // BuildConfig'den API anahtarlarını yükle
        initializeApiKeys()
    }
    
    /**
     * BuildConfig'den API anahtarlarını yükler ve SharedPreferences'a kaydeder
     */
    private fun initializeApiKeys() {
        // Weather API
        if (BuildConfig.WEATHER_API_KEY.isNotBlank()) {
            saveWeatherApiKey(BuildConfig.WEATHER_API_KEY)
        }
        
        // Gemini API
        if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            saveGeminiApiKey(BuildConfig.GEMINI_API_KEY)
        }
    }
    
    /**
     * Weather API anahtarını döndürür.
     * @return API anahtarı veya null
     */
    fun getWeatherApiKey(): String? {
        return sharedPreferences.getString("weather_api_key", BuildConfig.WEATHER_API_KEY)?.takeIf { it.isNotBlank() }
    }
    
    /**
     * Weather API anahtarını kaydeder.
     * @param apiKey API anahtarı
     */
    fun saveWeatherApiKey(apiKey: String) {
        sharedPreferences.edit().putString("weather_api_key", apiKey).apply()
    }
    
    /**
     * Gemini API anahtarını döndürür.
     * @return API anahtarı veya null
     */
    fun getGeminiApiKey(): String? {
        return sharedPreferences.getString("gemini_api_key", BuildConfig.GEMINI_API_KEY)?.takeIf { it.isNotBlank() }
    }
    
    /**
     * Gemini API anahtarını kaydeder.
     * @param apiKey API anahtarı
     */
    fun saveGeminiApiKey(apiKey: String) {
        sharedPreferences.edit().putString("gemini_api_key", apiKey).apply()
    }
    
    /**
     * API'nin kullanılabilir olup olmadığını kontrol eder.
     * @param apiType API tipi
     * @return API kullanılabilir ise true, değilse false
     */
    fun isApiAvailable(apiType: ApiType): Boolean {
        val status = apiStatus[apiType] ?: return false
        
        // Eğer API'de hata varsa ve son hatadan bu yana yeterli zaman geçmediyse
        if (status.hasError && System.currentTimeMillis() - status.lastErrorTime < ERROR_COOLDOWN_MS) {
            return false
        }
        
        // API anahtarını kontrol et
        return when (apiType) {
            ApiType.WEATHER -> getWeatherApiKey() != null
            ApiType.WIKIPEDIA -> true // Wikipedia API anahtarı gerektirmez
            ApiType.GEMINI -> getGeminiApiKey() != null
        }
    }
    
    /**
     * API hatasını kaydeder.
     * @param apiType API tipi
     */
    fun markApiError(apiType: ApiType) {
        val status = apiStatus[apiType] ?: ApiStatus()
        status.hasError = true
        status.lastErrorTime = System.currentTimeMillis()
        status.errorCount++
        apiStatus[apiType] = status
    }
    
    /**
     * API durumunu sıfırlar.
     * @param apiType API tipi
     */
    fun resetApiStatus(apiType: ApiType) {
        apiStatus[apiType] = ApiStatus()
    }
    
    /**
     * API tiplerini temsil eden enum.
     */
    enum class ApiType {
        WEATHER,
        WIKIPEDIA,
        GEMINI
    }
    
    /**
     * API durumunu temsil eden sınıf.
     */
    data class ApiStatus(
        var hasError: Boolean = false,
        var lastErrorTime: Long = 0,
        var errorCount: Int = 0
    )
    
    companion object {
        // Hata sonrası bekleme süresi (30 dakika)
        private const val ERROR_COOLDOWN_MS = 30 * 60 * 1000
    }
} 