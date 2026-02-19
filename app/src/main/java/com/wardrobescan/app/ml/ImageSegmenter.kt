package com.wardrobescan.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter as MPImageSegmenter
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageSegmenter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var segmenter: MPImageSegmenter? = null

    private fun getOrCreateSegmenter(): MPImageSegmenter {
        return segmenter ?: run {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("selfie_multiclass_256x256.tflite")
                .build()

            val options = MPImageSegmenter.ImageSegmenterOptions.builder()
                .setBaseOptions(baseOptions)
                .setOutputCategoryMask(true)
                .setOutputConfidenceMasks(false)
                .build()

            MPImageSegmenter.createFromOptions(context, options).also {
                segmenter = it
            }
        }
    }

    /**
     * Segments clothing from the background. Returns a cutout bitmap with
     * transparent background where non-clothing pixels are removed.
     */
    suspend fun segmentClothing(bitmap: Bitmap): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result: ImageSegmenterResult = getOrCreateSegmenter().segment(mpImage)

            val categoryMask = result.categoryMask().orElse(null)
                ?: return@withContext Result.failure(Exception("No segmentation mask produced"))

            val maskWidth = categoryMask.width
            val maskHeight = categoryMask.height
            val maskBuffer = ByteBufferExtractor.extract(categoryMask)

            // Create output bitmap with transparent background
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

            // Scale factor between mask and original
            val scaleX = bitmap.width.toFloat() / maskWidth
            val scaleY = bitmap.height.toFloat() / maskHeight

            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    val maskX = (x / scaleX).toInt().coerceIn(0, maskWidth - 1)
                    val maskY = (y / scaleY).toInt().coerceIn(0, maskHeight - 1)
                    val maskIndex = maskY * maskWidth + maskX

                    val categoryValue = maskBuffer.get(maskIndex).toInt() and 0xFF

                    // Category 0 = background; keep everything else (clothing/person regions)
                    if (categoryValue != 0) {
                        output.setPixel(x, y, bitmap.getPixel(x, y))
                    } else {
                        output.setPixel(x, y, Color.TRANSPARENT)
                    }
                }
            }

            Result.success(output)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        segmenter?.close()
        segmenter = null
    }
}
