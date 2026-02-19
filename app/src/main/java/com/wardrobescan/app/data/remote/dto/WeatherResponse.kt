package com.wardrobescan.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val main: MainData,
    val weather: List<WeatherCondition>,
    val wind: WindData,
    val name: String
)

data class MainData(
    val temp: Double,
    @SerializedName("feels_like")
    val feelsLike: Double,
    val humidity: Int
)

data class WeatherCondition(
    val id: Int = 0,
    val main: String,
    val description: String,
    val icon: String
)

data class WindData(
    val speed: Double
)
