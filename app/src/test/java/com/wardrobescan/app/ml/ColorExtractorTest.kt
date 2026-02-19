package com.wardrobescan.app.ml

import com.wardrobescan.app.data.model.DominantColor
import org.junit.Assert.*
import org.junit.Test

class ColorExtractorTest {

    @Test
    fun `DominantColor data class holds values correctly`() {
        val color = DominantColor(hex = "#FF0000", name = "Red", percentage = 55.5f)
        assertEquals("#FF0000", color.hex)
        assertEquals("Red", color.name)
        assertEquals(55.5f, color.percentage, 0.01f)
    }

    @Test
    fun `DominantColor equality based on hex`() {
        val color1 = DominantColor(hex = "#FF0000", name = "Red", percentage = 55.5f)
        val color2 = DominantColor(hex = "#FF0000", name = "Red", percentage = 55.5f)
        assertEquals(color1, color2)
    }

    @Test
    fun `DominantColor inequality for different hex`() {
        val color1 = DominantColor(hex = "#FF0000", name = "Red", percentage = 55.5f)
        val color2 = DominantColor(hex = "#0000FF", name = "Blue", percentage = 45.0f)
        assertNotEquals(color1, color2)
    }

    @Test
    fun `DominantColor percentage bounds`() {
        val fullColor = DominantColor(hex = "#000000", name = "Black", percentage = 100f)
        assertEquals(100f, fullColor.percentage, 0.01f)

        val zeroColor = DominantColor(hex = "#FFFFFF", name = "White", percentage = 0f)
        assertEquals(0f, zeroColor.percentage, 0.01f)
    }
}
