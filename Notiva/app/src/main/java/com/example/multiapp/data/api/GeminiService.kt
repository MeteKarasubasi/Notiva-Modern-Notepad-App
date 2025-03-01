package com.example.multiapp.data.api

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

interface GeminiService {
    fun initialize(apiKey: String)
    suspend fun generateContent(prompt: String): String
}

@Singleton
class GeminiServiceImpl @Inject constructor() : GeminiService {
    private var model: GenerativeModel? = null
    private var apiKey: String? = null
    private val TAG = "GeminiService"

    override fun initialize(apiKey: String) {
        Log.d(TAG, "Initializing Gemini model...")
        this.apiKey = apiKey
        model = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )
        Log.d(TAG, "Gemini model initialized")
    }

    override suspend fun generateContent(prompt: String): String {
        return try {
            Log.d(TAG, "Generating content for prompt: $prompt")
            
            val chat = model?.startChat()
            val response = chat?.sendMessage(prompt)
                ?: throw Exception("Model not initialized or chat failed to start")
            
            Log.d(TAG, "Response received from Gemini")
            val text = response.text ?: throw Exception("Empty response received")
            Log.d(TAG, "Response text: $text")
            
            text
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content: ${e.message}", e)
            throw Exception("Gemini API error: ${e.message}", e)
        }
    }
} 