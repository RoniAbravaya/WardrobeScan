package com.wardrobescan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wardrobescan.app.data.model.Outfit

@Entity(tableName = "outfits")
data class OutfitEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val itemIds: List<String>,
    val occasion: String,
    val rating: Int,
    val saved: Boolean,
    val weatherSummary: String,
    val createdAtMillis: Long
) {
    fun toModel(): Outfit = Outfit(
        id = id,
        userId = userId,
        itemIds = itemIds,
        occasion = occasion,
        rating = rating,
        saved = saved,
        weatherSummary = weatherSummary
    )

    companion object {
        fun fromModel(outfit: Outfit): OutfitEntity = OutfitEntity(
            id = outfit.id,
            userId = outfit.userId,
            itemIds = outfit.itemIds,
            occasion = outfit.occasion,
            rating = outfit.rating,
            saved = outfit.saved,
            weatherSummary = outfit.weatherSummary,
            createdAtMillis = outfit.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        )
    }
}
