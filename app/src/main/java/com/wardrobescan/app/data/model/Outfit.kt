package com.wardrobescan.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Outfit(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val itemIds: List<String> = emptyList(),
    val occasion: String = "casual",
    val rating: Int = 0,                // 0 = unrated, 1–5 stars
    val saved: Boolean = false,
    val weatherSummary: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

enum class Occasion(val displayName: String) {
    CASUAL("Casual"),
    WORK("Work"),
    GOING_OUT("Going Out");

    companion object {
        fun fromString(value: String): Occasion =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CASUAL
    }
}
