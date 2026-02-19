package com.wardrobescan.app.ml

import com.wardrobescan.app.data.model.ClothingCategory

/**
 * Deterministic mapping from ML Kit Image Labeling labels to ClothingCategory.
 * Designed to be easily extended or swapped with a custom TFLite model.
 */
object LabelCategoryMapper {

    private val labelMap: Map<String, ClothingCategory> = mapOf(
        // Tops
        "shirt" to ClothingCategory.TOP,
        "t-shirt" to ClothingCategory.TOP,
        "blouse" to ClothingCategory.TOP,
        "polo shirt" to ClothingCategory.TOP,
        "tank top" to ClothingCategory.TOP,
        "crop top" to ClothingCategory.TOP,
        "sweater" to ClothingCategory.TOP,
        "hoodie" to ClothingCategory.TOP,
        "sweatshirt" to ClothingCategory.TOP,
        "jersey" to ClothingCategory.TOP,
        "top" to ClothingCategory.TOP,
        "sleeve" to ClothingCategory.TOP,
        "turtleneck" to ClothingCategory.TOP,
        "cardigan" to ClothingCategory.TOP,
        "vest" to ClothingCategory.TOP,

        // Bottoms
        "jeans" to ClothingCategory.BOTTOM,
        "pants" to ClothingCategory.BOTTOM,
        "trousers" to ClothingCategory.BOTTOM,
        "shorts" to ClothingCategory.BOTTOM,
        "skirt" to ClothingCategory.BOTTOM,
        "leggings" to ClothingCategory.BOTTOM,
        "denim" to ClothingCategory.BOTTOM,
        "chinos" to ClothingCategory.BOTTOM,
        "sweatpants" to ClothingCategory.BOTTOM,

        // Outerwear
        "jacket" to ClothingCategory.OUTERWEAR,
        "coat" to ClothingCategory.OUTERWEAR,
        "blazer" to ClothingCategory.OUTERWEAR,
        "parka" to ClothingCategory.OUTERWEAR,
        "windbreaker" to ClothingCategory.OUTERWEAR,
        "raincoat" to ClothingCategory.OUTERWEAR,
        "overcoat" to ClothingCategory.OUTERWEAR,
        "trench coat" to ClothingCategory.OUTERWEAR,
        "bomber jacket" to ClothingCategory.OUTERWEAR,
        "leather jacket" to ClothingCategory.OUTERWEAR,
        "down jacket" to ClothingCategory.OUTERWEAR,

        // Dresses
        "dress" to ClothingCategory.DRESS,
        "gown" to ClothingCategory.DRESS,
        "sundress" to ClothingCategory.DRESS,
        "cocktail dress" to ClothingCategory.DRESS,
        "maxi dress" to ClothingCategory.DRESS,
        "jumpsuit" to ClothingCategory.DRESS,
        "romper" to ClothingCategory.DRESS,

        // Shoes
        "shoe" to ClothingCategory.SHOES,
        "shoes" to ClothingCategory.SHOES,
        "sneaker" to ClothingCategory.SHOES,
        "sneakers" to ClothingCategory.SHOES,
        "boot" to ClothingCategory.SHOES,
        "boots" to ClothingCategory.SHOES,
        "sandal" to ClothingCategory.SHOES,
        "sandals" to ClothingCategory.SHOES,
        "heel" to ClothingCategory.SHOES,
        "heels" to ClothingCategory.SHOES,
        "loafer" to ClothingCategory.SHOES,
        "slipper" to ClothingCategory.SHOES,
        "footwear" to ClothingCategory.SHOES,
        "running shoe" to ClothingCategory.SHOES,
        "high heel" to ClothingCategory.SHOES,

        // Accessories
        "hat" to ClothingCategory.ACCESSORY,
        "cap" to ClothingCategory.ACCESSORY,
        "scarf" to ClothingCategory.ACCESSORY,
        "glove" to ClothingCategory.ACCESSORY,
        "gloves" to ClothingCategory.ACCESSORY,
        "belt" to ClothingCategory.ACCESSORY,
        "tie" to ClothingCategory.ACCESSORY,
        "bow tie" to ClothingCategory.ACCESSORY,
        "watch" to ClothingCategory.ACCESSORY,
        "sunglasses" to ClothingCategory.ACCESSORY,
        "glasses" to ClothingCategory.ACCESSORY,
        "bag" to ClothingCategory.ACCESSORY,
        "handbag" to ClothingCategory.ACCESSORY,
        "backpack" to ClothingCategory.ACCESSORY,
        "purse" to ClothingCategory.ACCESSORY,
        "jewelry" to ClothingCategory.ACCESSORY,
        "necklace" to ClothingCategory.ACCESSORY,
        "bracelet" to ClothingCategory.ACCESSORY,
        "earring" to ClothingCategory.ACCESSORY,
        "ring" to ClothingCategory.ACCESSORY,
        "wallet" to ClothingCategory.ACCESSORY,
        "umbrella" to ClothingCategory.ACCESSORY,
        "headband" to ClothingCategory.ACCESSORY,
        "beanie" to ClothingCategory.ACCESSORY,

        // Generic clothing labels that need context
        "clothing" to ClothingCategory.TOP,
        "fashion" to ClothingCategory.TOP,
        "textile" to ClothingCategory.TOP,
        "fabric" to ClothingCategory.TOP,
    )

    /**
     * Maps a label string to a [ClothingCategory].
     * Returns null if no mapping is found—in that case, prompt the user to choose.
     */
    fun map(label: String): ClothingCategory? {
        return labelMap[label.lowercase().trim()]
    }

    /**
     * Maps a list of labels, returning the most likely category
     * based on the first match (labels assumed to be sorted by confidence).
     */
    fun mapBestMatch(labels: List<String>): Pair<ClothingCategory, String>? {
        for (label in labels) {
            val category = map(label)
            if (category != null) {
                return category to label
            }
        }
        return null
    }

    /**
     * Returns all known labels for a specific category.
     */
    fun labelsForCategory(category: ClothingCategory): List<String> {
        return labelMap.filter { it.value == category }.keys.toList()
    }
}
