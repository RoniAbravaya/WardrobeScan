package com.wardrobescan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wardrobescan.app.data.model.ClothingItem
import com.wardrobescan.app.data.model.DominantColor

@Entity(tableName = "clothing_items")
data class ClothingItemEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val category: String,
    val subcategory: String,
    val labels: List<String>,
    val colors: List<DominantColor>,
    val imageUrl: String,
    val cutoutUrl: String,
    val season: String,
    val warmthScore: Int,
    val waterproof: Boolean,
    val breathable: Boolean,
    val userNotes: String,
    val confidence: Float,
    val createdAtMillis: Long
) {
    fun toModel(): ClothingItem = ClothingItem(
        id = id,
        userId = userId,
        category = category,
        subcategory = subcategory,
        labels = labels,
        colors = colors,
        imageUrl = imageUrl,
        cutoutUrl = cutoutUrl,
        season = season,
        warmthScore = warmthScore,
        waterproof = waterproof,
        breathable = breathable,
        userNotes = userNotes,
        confidence = confidence
    )

    companion object {
        fun fromModel(item: ClothingItem): ClothingItemEntity = ClothingItemEntity(
            id = item.id,
            userId = item.userId,
            category = item.category,
            subcategory = item.subcategory,
            labels = item.labels,
            colors = item.colors,
            imageUrl = item.imageUrl,
            cutoutUrl = item.cutoutUrl,
            season = item.season,
            warmthScore = item.warmthScore,
            waterproof = item.waterproof,
            breathable = item.breathable,
            userNotes = item.userNotes,
            confidence = item.confidence,
            createdAtMillis = item.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        )
    }
}
