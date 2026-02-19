package com.wardrobescan.app.domain

import com.wardrobescan.app.data.model.ClothingCategory
import com.wardrobescan.app.data.model.ClothingItem
import com.wardrobescan.app.data.model.Occasion
import com.wardrobescan.app.data.model.Outfit
import com.wardrobescan.app.data.model.WeatherData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rule-based outfit suggestion engine.
 *
 * Rules:
 * - Rain → prefer waterproof outerwear + closed shoes
 * - Cold (<10°C) → increase warmthScore requirement, outerwear required
 * - Hot (>28°C) → prefer breathable/light items (warmthScore ≤ 2)
 * - Occasion filter: Casual / Work / Going out
 */
@Singleton
class OutfitSuggestionEngine @Inject constructor() {

    companion object {
        private const val MIN_WARMTH_COLD = 4
        private const val MAX_WARMTH_HOT = 2
        private const val SUGGESTIONS_COUNT = 3
    }

    /**
     * Generates up to [SUGGESTIONS_COUNT] outfit suggestions.
     */
    fun suggest(
        items: List<ClothingItem>,
        weather: WeatherData,
        occasion: Occasion = Occasion.CASUAL
    ): List<Outfit> {
        if (items.isEmpty()) return emptyList()

        val tops = items.filter { it.categoryEnum == ClothingCategory.TOP }
        val bottoms = items.filter { it.categoryEnum == ClothingCategory.BOTTOM }
        val outerwear = items.filter { it.categoryEnum == ClothingCategory.OUTERWEAR }
        val dresses = items.filter { it.categoryEnum == ClothingCategory.DRESS }
        val shoes = items.filter { it.categoryEnum == ClothingCategory.SHOES }
        val accessories = items.filter { it.categoryEnum == ClothingCategory.ACCESSORY }

        // Filter items by weather conditions
        val filteredTops = filterByWeather(tops, weather)
        val filteredBottoms = filterByWeather(bottoms, weather)
        val filteredOuterwear = filterByWeather(outerwear, weather)
        val filteredDresses = filterByWeather(dresses, weather)
        val filteredShoes = filterShoes(shoes, weather)

        val outfits = mutableListOf<Outfit>()

        // Strategy 1: Top + Bottom combinations
        for (top in filteredTops.shuffled().take(SUGGESTIONS_COUNT)) {
            val bottom = filteredBottoms.shuffled().firstOrNull() ?: continue
            val outfitItems = mutableListOf(top.id, bottom.id)

            // Add outerwear if cold or rainy
            if (weather.isCold || weather.isRainy) {
                val jacket = pickOuterwear(filteredOuterwear, weather)
                jacket?.let { outfitItems.add(it.id) }
            }

            // Add shoes
            filteredShoes.shuffled().firstOrNull()?.let { outfitItems.add(it.id) }

            // Add weather-appropriate accessory
            pickAccessory(accessories, weather)?.let { outfitItems.add(it.id) }

            outfits.add(
                Outfit(
                    itemIds = outfitItems,
                    occasion = occasion.name.lowercase(),
                    weatherSummary = "${weather.temperature.toInt()}°C, ${weather.condition}"
                )
            )
        }

        // Strategy 2: Dress combinations (if available and weather-appropriate)
        if (outfits.size < SUGGESTIONS_COUNT && filteredDresses.isNotEmpty() && !weather.isCold) {
            for (dress in filteredDresses.shuffled().take(SUGGESTIONS_COUNT - outfits.size)) {
                val outfitItems = mutableListOf(dress.id)
                filteredShoes.shuffled().firstOrNull()?.let { outfitItems.add(it.id) }

                if (weather.isRainy) {
                    pickOuterwear(filteredOuterwear, weather)?.let { outfitItems.add(it.id) }
                }

                outfits.add(
                    Outfit(
                        itemIds = outfitItems,
                        occasion = occasion.name.lowercase(),
                        weatherSummary = "${weather.temperature.toInt()}°C, ${weather.condition}"
                    )
                )
            }
        }

        return outfits.take(SUGGESTIONS_COUNT)
    }

    internal fun filterByWeather(items: List<ClothingItem>, weather: WeatherData): List<ClothingItem> {
        return items.filter { item ->
            when {
                weather.isCold -> item.warmthScore >= MIN_WARMTH_COLD
                weather.isHot -> item.warmthScore <= MAX_WARMTH_HOT && item.breathable
                else -> true
            }
        }.ifEmpty { items } // Fallback to all items if filters are too strict
    }

    internal fun filterShoes(shoes: List<ClothingItem>, weather: WeatherData): List<ClothingItem> {
        if (weather.isRainy || weather.isSnowy) {
            val waterproofShoes = shoes.filter { it.waterproof }
            if (waterproofShoes.isNotEmpty()) return waterproofShoes
        }
        return shoes
    }

    internal fun pickOuterwear(
        outerwear: List<ClothingItem>,
        weather: WeatherData
    ): ClothingItem? {
        return when {
            weather.isRainy -> outerwear
                .filter { it.waterproof }
                .maxByOrNull { it.warmthScore }
                ?: outerwear.firstOrNull()

            weather.isCold -> outerwear
                .maxByOrNull { it.warmthScore }

            else -> outerwear.firstOrNull()
        }
    }

    private fun pickAccessory(
        accessories: List<ClothingItem>,
        weather: WeatherData
    ): ClothingItem? {
        return when {
            weather.isRainy -> accessories.firstOrNull {
                it.labels.any { label ->
                    label.lowercase() in listOf("umbrella", "rain hat", "waterproof")
                }
            }
            weather.isCold -> accessories.firstOrNull {
                it.labels.any { label ->
                    label.lowercase() in listOf("scarf", "glove", "gloves", "beanie", "hat")
                }
            }
            weather.isHot -> accessories.firstOrNull {
                it.labels.any { label ->
                    label.lowercase() in listOf("sunglasses", "hat", "cap")
                }
            }
            else -> null
        }
    }
}
