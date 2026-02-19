package com.wardrobescan.app.data.model

data class WeatherData(
    val temperature: Double = 0.0,     // Celsius
    val feelsLike: Double = 0.0,
    val condition: String = "",         // e.g. "Rain", "Clear", "Clouds"
    val description: String = "",
    val windSpeed: Double = 0.0,       // m/s
    val humidity: Int = 0,
    val icon: String = "",
    val city: String = ""
) {
    val isRainy: Boolean
        get() = condition.lowercase() in listOf("rain", "drizzle", "thunderstorm")

    val isSnowy: Boolean
        get() = condition.lowercase() == "snow"

    val isCold: Boolean
        get() = temperature < 10.0

    val isHot: Boolean
        get() = temperature > 28.0

    val isWindy: Boolean
        get() = windSpeed > 10.0

    fun temperatureInFahrenheit(): Double = temperature * 9.0 / 5.0 + 32.0
}
