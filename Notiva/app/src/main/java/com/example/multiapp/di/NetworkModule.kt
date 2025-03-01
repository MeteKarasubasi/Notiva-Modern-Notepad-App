package com.example.multiapp.di

import android.content.Context
import com.example.multiapp.BuildConfig
import com.example.multiapp.data.MessageHistoryTracker
import com.example.multiapp.data.QueryDetector
import com.example.multiapp.data.api.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

/**
 * Ağ istekleri için gerekli bağımlılıkları sağlayan modül.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * OkHttpClient sağlar.
     * @return OkHttpClient örneği
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Weather API için Retrofit sağlar.
     * @param okHttpClient OkHttpClient örneği
     * @return Weather API için Retrofit örneği
     */
    @Provides
    @Singleton
    @Named("weatherRetrofit")
    fun provideWeatherRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.met.no/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Weather servisi sağlar.
     * @param retrofit Weather API için Retrofit örneği
     * @return WeatherService örneği
     */
    @Provides
    @Singleton
    fun provideWeatherService(@Named("weatherRetrofit") retrofit: Retrofit): WeatherService {
        return retrofit.create(WeatherService::class.java)
    }

    /**
     * Geocoding API için Retrofit sağlar.
     * @param okHttpClient OkHttpClient örneği
     * @return Geocoding API için Retrofit örneği
     */
    @Provides
    @Singleton
    @Named("geocodingRetrofit")
    fun provideGeocodingRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Geocoding servisi sağlar.
     * @param retrofit Geocoding API için Retrofit örneği
     * @return GeocodingService örneği
     */
    @Provides
    @Singleton
    fun provideGeocodingService(@Named("geocodingRetrofit") retrofit: Retrofit): GeocodingService {
        return retrofit.create(GeocodingService::class.java)
    }

    /**
     * Wikipedia API için Retrofit sağlar.
     * @param okHttpClient OkHttpClient örneği
     * @return Wikipedia API için Retrofit örneği
     */
    @Provides
    @Singleton
    @Named("wikipediaRetrofit")
    fun provideWikipediaRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://tr.wikipedia.org/api/rest_v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Wikipedia servisi sağlar.
     * @param retrofit Wikipedia API için Retrofit örneği
     * @return WikipediaService örneği
     */
    @Provides
    @Singleton
    fun provideWikipediaService(@Named("wikipediaRetrofit") retrofit: Retrofit): WikipediaService {
        return retrofit.create(WikipediaService::class.java)
    }

    /**
     * ApiKeyManager sağlar.
     * @param context Uygulama bağlamı
     * @return ApiKeyManager örneği
     */
    @Provides
    @Singleton
    fun provideApiKeyManager(@ApplicationContext context: Context): ApiKeyManager {
        return ApiKeyManager(context)
    }
    
    @Provides
    @Singleton
    fun provideMessageHistoryTracker(): MessageHistoryTracker {
        return MessageHistoryTracker()
    }
    
    @Provides
    @Singleton
    fun provideQueryDetector(
        messageHistoryTracker: MessageHistoryTracker,
        apiKeyManager: ApiKeyManager
    ): QueryDetector {
        return QueryDetector(messageHistoryTracker, apiKeyManager)
    }

    @Provides
    @Singleton
    fun provideGeminiService(): GeminiService {
        return GeminiServiceImpl()
    }

    /**
     * ApiServiceManager sağlar.
     * @param weatherService Weather servisi
     * @param wikipediaService Wikipedia servisi
     * @param geocodingService Geocoding servisi
     * @param geminiService Gemini servisi
     * @param apiKeyManager API anahtarı yöneticisi
     * @param messageHistoryTracker Mesaj geçmişi takipçisi
     * @return ApiServiceManager örneği
     */
    @Provides
    @Singleton
    fun provideApiServiceManager(
        weatherService: WeatherService,
        wikipediaService: WikipediaService,
        geocodingService: GeocodingService,
        geminiService: GeminiService,
        apiKeyManager: ApiKeyManager,
        messageHistoryTracker: MessageHistoryTracker
    ): ApiServiceManager {
        return ApiServiceManager(
            weatherService,
            wikipediaService,
            geocodingService,
            geminiService,
            apiKeyManager,
            messageHistoryTracker
        )
    }
} 