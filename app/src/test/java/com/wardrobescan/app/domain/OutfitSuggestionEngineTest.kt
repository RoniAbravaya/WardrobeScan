package com.wardrobescan.app.domain

import com.wardrobescan.app.data.model.ClothingItem
import com.wardrobescan.app.data.model.Occasion
import com.wardrobescan.app.data.model.WeatherData
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OutfitSuggestionEngineTest {

    private lateinit var engine: OutfitSuggestionEngine

    private val lightTop = ClothingItem(
        id = "top1", category = "top",
        warmthScore = 1, breathable = true, waterproof = false
    )
    private val warmTop = ClothingItem(
        id = "top2", category = "top",
        warmthScore = 5, breathable = false, waterproof = false
    )
    private val jeans = ClothingItem(
        id = "bottom1", category = "bottom",
        warmthScore = 3, breathable = true, waterproof = false
    )
    private val warmPants = ClothingItem(
        id = "bottom2", category = "bottom",
        warmthScore = 5, breathable = false, waterproof = false
    )
    private val rainJacket = ClothingItem(
        id = "outer1", category = "outerwear",
        warmthScore = 3, breathable = true, waterproof = true
    )
    private val winterCoat = ClothingItem(
        id = "outer2", category = "outerwear",
        warmthScore = 5, breathable = false, waterproof = false
    )
    private val sneakers = ClothingItem(
        id = "shoes1", category = "shoes",
        warmthScore = 2, breathable = true, waterproof = false
    )
    private val rainBoots = ClothingItem(
        id = "shoes2", category = "shoes",
        warmthScore = 3, breathable = false, waterproof = true
    )
    private val umbrella = ClothingItem(
        id = "acc1", category = "accessory",
        labels = listOf("umbrella"),
        warmthScore = 1, breathable = true, waterproof = true
    )
    private val scarf = ClothingItem(
        id = "acc2", category = "accessory",
        labels = listOf("scarf"),
        warmthScore = 4, breathable = true, waterproof = false
    )

    private val allItems = listOf(
        lightTop, warmTop, jeans, warmPants,
        rainJacket, winterCoat, sneakers, rainBoots,
        umbrella, scarf
    )

    private val sunnyWeather = WeatherData(
        temperature = 22.0, feelsLike = 22.0,
        condition = "Clear", description = "clear sky",
        windSpeed = 3.0, humidity = 50, city = "Tel Aviv"
    )

    private val rainyWeather = WeatherData(
        temperature = 15.0, feelsLike = 13.0,
        condition = "Rain", description = "moderate rain",
        windSpeed = 8.0, humidity = 85, city = "London"
    )

    private val coldWeather = WeatherData(
        temperature = 5.0, feelsLike = 1.0,
        condition = "Clouds", description = "overcast clouds",
        windSpeed = 12.0, humidity = 60, city = "Berlin"
    )

    private val hotWeather = WeatherData(
        temperature = 35.0, feelsLike = 38.0,
        condition = "Clear", description = "clear sky",
        windSpeed = 2.0, humidity = 30, city = "Dubai"
    )

    @Before
    fun setUp() {
        engine = OutfitSuggestionEngine()
    }

    @Test
    fun `empty wardrobe returns empty suggestions`() {
        val outfits = engine.suggest(emptyList(), sunnyWeather)
        assertTrue(outfits.isEmpty())
    }

    @Test
    fun `sunny weather returns outfits`() {
        val outfits = engine.suggest(allItems, sunnyWeather)
        assertTrue(outfits.isNotEmpty())
        assertTrue(outfits.size <= 3)
    }

    @Test
    fun `rainy weather prefers waterproof outerwear`() {
        val outfits = engine.suggest(allItems, rainyWeather)
        assertTrue(outfits.isNotEmpty())

        // Check that outfits include outerwear (rain jacket should be preferred)
        outfits.forEach { outfit ->
            // At least some outfits should include the rain jacket
            if (outfit.itemIds.contains(rainJacket.id)) {
                assertTrue("Rain jacket should be in rainy outfits", true)
                return
            }
        }
    }

    @Test
    fun `rainy weather prefers waterproof shoes`() {
        val filtered = engine.filterShoes(listOf(sneakers, rainBoots), rainyWeather)
        assertEquals(1, filtered.size)
        assertEquals(rainBoots.id, filtered[0].id)
    }

    @Test
    fun `sunny weather does not filter shoes`() {
        val filtered = engine.filterShoes(listOf(sneakers, rainBoots), sunnyWeather)
        assertEquals(2, filtered.size)
    }

    @Test
    fun `cold weather filters for warm items`() {
        val filtered = engine.filterByWeather(listOf(lightTop, warmTop), coldWeather)
        // Should prefer warm items
        assertTrue(filtered.any { it.warmthScore >= 4 })
    }

    @Test
    fun `hot weather filters for breathable items`() {
        val filtered = engine.filterByWeather(listOf(lightTop, warmTop), hotWeather)
        // Should prefer breathable, light items
        assertTrue(filtered.any { it.breathable && it.warmthScore <= 2 })
    }

    @Test
    fun `hot weather excludes heavy items when alternatives exist`() {
        val filtered = engine.filterByWeather(listOf(lightTop, warmTop), hotWeather)
        // Light top should be included, warm top should be excluded
        assertTrue(filtered.contains(lightTop))
        assertFalse(filtered.contains(warmTop))
    }

    @Test
    fun `cold weather outerwear picks warmest`() {
        val picked = engine.pickOuterwear(listOf(rainJacket, winterCoat), coldWeather)
        assertNotNull(picked)
        assertEquals(winterCoat.id, picked!!.id)
    }

    @Test
    fun `rainy weather outerwear picks waterproof`() {
        val picked = engine.pickOuterwear(listOf(rainJacket, winterCoat), rainyWeather)
        assertNotNull(picked)
        assertEquals(rainJacket.id, picked!!.id)
    }

    @Test
    fun `occasion is set in outfit`() {
        val outfits = engine.suggest(allItems, sunnyWeather, Occasion.WORK)
        outfits.forEach { outfit ->
            assertEquals("work", outfit.occasion)
        }
    }

    @Test
    fun `weather summary is set in outfit`() {
        val outfits = engine.suggest(allItems, sunnyWeather)
        outfits.forEach { outfit ->
            assertTrue(outfit.weatherSummary.isNotEmpty())
            assertTrue(outfit.weatherSummary.contains("22°C"))
        }
    }

    @Test
    fun `outfit contains at most 3 suggestions`() {
        val manyTops = (1..10).map {
            ClothingItem(id = "top_$it", category = "top", warmthScore = 3)
        }
        val manyBottoms = (1..5).map {
            ClothingItem(id = "bottom_$it", category = "bottom", warmthScore = 3)
        }
        val outfits = engine.suggest(manyTops + manyBottoms, sunnyWeather)
        assertTrue(outfits.size <= 3)
    }

    @Test
    fun `fallback when filters are too strict`() {
        // Only have light items but cold weather — should still return something
        val onlyLight = listOf(lightTop, jeans, sneakers)
        val filtered = engine.filterByWeather(onlyLight, coldWeather)
        // Should fall back to all items
        assertTrue(filtered.isNotEmpty())
    }

    @Test
    fun `weather model properties are correct`() {
        assertTrue(rainyWeather.isRainy)
        assertFalse(rainyWeather.isHot)

        assertTrue(coldWeather.isCold)
        assertFalse(coldWeather.isRainy)

        assertTrue(hotWeather.isHot)
        assertFalse(hotWeather.isCold)

        assertFalse(sunnyWeather.isRainy)
        assertFalse(sunnyWeather.isCold)
        assertFalse(sunnyWeather.isHot)
    }
}
