package com.example.multiapp.data.repository

import com.example.multiapp.data.api.GeminiService
import com.example.multiapp.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val geminiService: GeminiService
) {
    fun generateResponse(userMessage: String, apiKey: String): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())

            // Initialize Gemini service with API key
            geminiService.initialize(apiKey)

            // Generate response
            val response = geminiService.generateContent(userMessage)
            if (response.isNotBlank()) {
                emit(Resource.Success(response))
            } else {
                emit(Resource.Error("Boş yanıt alındı"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Gemini API hatası: ${e.localizedMessage}"))
        }
    }
} 