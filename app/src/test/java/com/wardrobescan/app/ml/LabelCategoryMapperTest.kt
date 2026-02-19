package com.wardrobescan.app.ml

import com.wardrobescan.app.data.model.ClothingCategory
import org.junit.Assert.*
import org.junit.Test

class LabelCategoryMapperTest {

    @Test
    fun `map known top labels`() {
        assertEquals(ClothingCategory.TOP, LabelCategoryMapper.map("Shirt"))
        assertEquals(ClothingCategory.TOP, LabelCategoryMapper.map("t-shirt"))
        assertEquals(ClothingCategory.TOP, LabelCategoryMapper.map("BLOUSE"))
        assertEquals(ClothingCategory.TOP, LabelCategoryMapper.map("sweater"))
        assertEquals(ClothingCategory.TOP, LabelCategoryMapper.map("Hoodie"))
    }

    @Test
    fun `map known bottom labels`() {
        assertEquals(ClothingCategory.BOTTOM, LabelCategoryMapper.map("jeans"))
        assertEquals(ClothingCategory.BOTTOM, LabelCategoryMapper.map("Pants"))
        assertEquals(ClothingCategory.BOTTOM, LabelCategoryMapper.map("shorts"))
        assertEquals(ClothingCategory.BOTTOM, LabelCategoryMapper.map("SKIRT"))
        assertEquals(ClothingCategory.BOTTOM, LabelCategoryMapper.map("leggings"))
    }

    @Test
    fun `map known outerwear labels`() {
        assertEquals(ClothingCategory.OUTERWEAR, LabelCategoryMapper.map("jacket"))
        assertEquals(ClothingCategory.OUTERWEAR, LabelCategoryMapper.map("Coat"))
        assertEquals(ClothingCategory.OUTERWEAR, LabelCategoryMapper.map("blazer"))
        assertEquals(ClothingCategory.OUTERWEAR, LabelCategoryMapper.map("parka"))
        assertEquals(ClothingCategory.OUTERWEAR, LabelCategoryMapper.map("windbreaker"))
    }

    @Test
    fun `map known dress labels`() {
        assertEquals(ClothingCategory.DRESS, LabelCategoryMapper.map("dress"))
        assertEquals(ClothingCategory.DRESS, LabelCategoryMapper.map("Gown"))
        assertEquals(ClothingCategory.DRESS, LabelCategoryMapper.map("jumpsuit"))
    }

    @Test
    fun `map known shoe labels`() {
        assertEquals(ClothingCategory.SHOES, LabelCategoryMapper.map("shoe"))
        assertEquals(ClothingCategory.SHOES, LabelCategoryMapper.map("sneakers"))
        assertEquals(ClothingCategory.SHOES, LabelCategoryMapper.map("Boots"))
        assertEquals(ClothingCategory.SHOES, LabelCategoryMapper.map("sandals"))
    }

    @Test
    fun `map known accessory labels`() {
        assertEquals(ClothingCategory.ACCESSORY, LabelCategoryMapper.map("hat"))
        assertEquals(ClothingCategory.ACCESSORY, LabelCategoryMapper.map("scarf"))
        assertEquals(ClothingCategory.ACCESSORY, LabelCategoryMapper.map("belt"))
        assertEquals(ClothingCategory.ACCESSORY, LabelCategoryMapper.map("sunglasses"))
        assertEquals(ClothingCategory.ACCESSORY, LabelCategoryMapper.map("backpack"))
    }

    @Test
    fun `unknown labels return null`() {
        assertNull(LabelCategoryMapper.map("banana"))
        assertNull(LabelCategoryMapper.map("car"))
        assertNull(LabelCategoryMapper.map("building"))
        assertNull(LabelCategoryMapper.map(""))
    }

    @Test
    fun `mapBestMatch returns first matching label`() {
        val labels = listOf("person", "building", "jacket", "clothing")
        val result = LabelCategoryMapper.mapBestMatch(labels)
        assertNotNull(result)
        assertEquals(ClothingCategory.OUTERWEAR, result!!.first)
        assertEquals("jacket", result.second)
    }

    @Test
    fun `mapBestMatch returns null when no labels match`() {
        val labels = listOf("person", "building", "sky")
        val result = LabelCategoryMapper.mapBestMatch(labels)
        assertNull(result)
    }

    @Test
    fun `mapBestMatch with empty list returns null`() {
        val result = LabelCategoryMapper.mapBestMatch(emptyList())
        assertNull(result)
    }

    @Test
    fun `labelsForCategory returns known labels`() {
        val topLabels = LabelCategoryMapper.labelsForCategory(ClothingCategory.TOP)
        assertTrue(topLabels.isNotEmpty())
        assertTrue("shirt" in topLabels)
        assertTrue("sweater" in topLabels)
    }

    @Test
    fun `case insensitive mapping`() {
        assertEquals(
            LabelCategoryMapper.map("JACKET"),
            LabelCategoryMapper.map("jacket")
        )
        assertEquals(
            LabelCategoryMapper.map("Jeans"),
            LabelCategoryMapper.map("jeans")
        )
    }

    @Test
    fun `whitespace is trimmed`() {
        assertEquals(ClothingCategory.TOP, LabelCategoryMapper.map("  shirt  "))
        assertEquals(ClothingCategory.BOTTOM, LabelCategoryMapper.map(" jeans "))
    }
}
