package com.wardrobescan.app.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wardrobescan.app.data.model.DominantColor

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromDominantColorList(value: List<DominantColor>): String = gson.toJson(value)

    @TypeConverter
    fun toDominantColorList(value: String): List<DominantColor> {
        val type = object : TypeToken<List<DominantColor>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
