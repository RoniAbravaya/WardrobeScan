package com.wardrobescan.app.data.repository

import com.wardrobescan.app.BuildConfig
import com.wardrobescan.app.data.model.WeatherData
import com.wardrobescan.app.data.remote.WeatherApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApiService
) {
    private var cachedWeather: WeatherData? = null
    private var cacheTimestamp: Long = 0L
    private val cacheValidityMs = 30 * 60 * 1000L // 30 minutes

    suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherData> {
        val now = System.currentTimeMillis()
        cachedWeather?.let {
            if (now - cacheTimestamp < cacheValidityMs) {
                return Result.success(it)
            }
        }

        return try {
            val response = weatherApi.getCurrentWeather(
                latitude = latitude,
                longitude = longitude,
                apiKey = BuildConfig.WEATHER_API_KEY
            )
            val weather = WeatherData(
                temperature = response.main.temp,
                feelsLike = response.main.feelsLike,
                condition = response.weather.firstOrNull()?.main ?: "Unknown",
                description = response.weather.firstOrNull()?.description ?: "",
                windSpeed = response.wind.speed,
                humidity = response.main.humidity,
                icon = response.weather.firstOrNull()?.icon ?: "",
                city = response.name
            )
            cachedWeather = weather
            cacheTimestamp = now
            Result.success(weather)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
