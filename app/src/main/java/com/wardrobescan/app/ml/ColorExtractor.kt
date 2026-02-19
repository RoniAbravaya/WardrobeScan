package com.wardrobescan.app.ml

import android.graphics.Bitmap
import android.graphics.Color
import androidx.palette.graphics.Palette
import com.wardrobescan.app.data.model.DominantColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ColorExtractor @Inject constructor() {

    /**
     * Extracts the top 3 dominant colors from a bitmap.
     * Returns colors with hex value, human-readable name, and percentage.
     */
    suspend fun extractColors(bitmap: Bitmap): List<DominantColor> = withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap)
            .maximumColorCount(8)
            .generate()

        val swatches = palette.swatches
            .sortedByDescending { it.population }
            .take(3)

        val totalPopulation = swatches.sumOf { it.population }.toFloat()

        swatches.map { swatch ->
            DominantColor(
                hex = colorToHex(swatch.rgb),
                name = getColorName(swatch.rgb),
                percentage = if (totalPopulation > 0) {
                    (swatch.population / totalPopulation * 100).coerceIn(0f, 100f)
                } else 0f
            )
        }
    }

    private fun colorToHex(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    internal fun getColorName(color: Int): String {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        val hsv = FloatArray(3)
        Color.RGBToHSV(r, g, b, hsv)
        val hue = hsv[0]
        val saturation = hsv[1]
        val value = hsv[2]

        // Handle achromatic colors
        if (saturation < 0.1f) {
            return when {
                value < 0.15f -> "Black"
                value < 0.4f -> "Dark Gray"
                value < 0.65f -> "Gray"
                value < 0.85f -> "Light Gray"
                else -> "White"
            }
        }

        // Handle low saturation (pastel/muted)
        val prefix = when {
            value < 0.3f -> "Dark "
            saturation < 0.3f && value > 0.7f -> "Light "
            else -> ""
        }

        val colorName = when {
            hue < 15f -> "Red"
            hue < 40f -> "Orange"
            hue < 65f -> "Yellow"
            hue < 160f -> "Green"
            hue < 195f -> "Teal"
            hue < 250f -> "Blue"
            hue < 290f -> "Purple"
            hue < 330f -> "Pink"
            else -> "Red"
        }

        return "$prefix$colorName"
    }
}
