package com.example.multiapp.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface WeatherService {
    @Headers(
        "User-Agent: Notiva/1.0 (ismailmetekarasubasi@gmail.com)",
        "Accept: application/json"
    )
    @GET("weatherapi/locationforecast/2.0/compact")
    suspend fun getWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double
    ): Response<WeatherResponse>
}

data class WeatherResponse(
    val properties: WeatherProperties
)

data class WeatherProperties(
    val timeseries: List<TimeseriesItem>
)

data class TimeseriesItem(
    val time: String,
    val data: WeatherData
)

data class WeatherData(
    val instant: InstantData,
    val next_1_hours: NextHours?,
    val next_6_hours: NextHours?,
    val next_12_hours: NextHours?
)

data class InstantData(
    val details: WeatherDetails
)

data class WeatherDetails(
    val air_pressure_at_sea_level: Double?,
    val air_temperature: Double?,
    val cloud_area_fraction: Double?,
    val relative_humidity: Double?,
    val wind_from_direction: Double?,
    val wind_speed: Double?
)

data class NextHours(
    val summary: WeatherSummary,
    val details: PrecipitationDetails?
)

data class WeatherSummary(
    val symbol_code: String
)

data class PrecipitationDetails(
    val precipitation_amount: Double?
)