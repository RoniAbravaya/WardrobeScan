package com.wardrobescan.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ClothingItem(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val category: String = "",
    val subcategory: String = "",
    val labels: List<String> = emptyList(),
    val colors: List<DominantColor> = emptyList(),
    val imageUrl: String = "",
    val cutoutUrl: String = "",
    val season: String = "all",
    val warmthScore: Int = 3,          // 1 (very light) – 5 (very warm)
    val waterproof: Boolean = false,
    val breathable: Boolean = true,
    val userNotes: String = "",
    val confidence: Float = 0f,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    val categoryEnum: ClothingCategory?
        get() = ClothingCategory.fromString(category)
}
