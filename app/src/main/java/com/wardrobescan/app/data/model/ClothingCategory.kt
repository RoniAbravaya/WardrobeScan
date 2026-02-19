package com.wardrobescan.app.data.model

enum class ClothingCategory(val displayName: String) {
    TOP("Top"),
    BOTTOM("Bottom"),
    OUTERWEAR("Outerwear"),
    DRESS("Dress"),
    SHOES("Shoes"),
    ACCESSORY("Accessory");

    companion object {
        fun fromString(value: String): ClothingCategory? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
