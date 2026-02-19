package com.wardrobescan.app.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.wardrobescan.app.data.model.AnalysisResult
import com.wardrobescan.app.data.model.ClothingCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the full clothing analysis pipeline:
 * 1. Segment clothing from background (MediaPipe)
 * 2. Label the cropped image (ML Kit)
 * 3. Map labels to clothing category
 * 4. Extract dominant colors
 *
 * Designed so the labeling step can be swapped with a custom TFLite model.
 */
@Singleton
class ClothingAnalyzer @Inject constructor(
    private val imageSegmenter: ImageSegmenter,
    private val imageLabeler: ImageLabeler,
    private val colorExtractor: ColorExtractor
) {
    companion object {
        const val CONFIDENCE_THRESHOLD = 0.6f
    }

    data class SegmentedResult(
        val cutoutBitmap: Bitmap,
        val analysisResult: AnalysisResult
    )

    /**
     * Full analysis pipeline. Returns the cutout bitmap and the analysis result.
     * If confidence is below threshold, category will still be the best guess
     * but the caller should prompt the user to confirm.
     */
    suspend fun analyze(bitmap: Bitmap): Result<SegmentedResult> {
        // Step 1: Segment
        val segmentResult = imageSegmenter.segmentClothing(bitmap)
        val cutoutBitmap = segmentResult.getOrElse { e ->
            return Result.failure(Exception("Segmentation failed: ${e.message}", e))
        }

        // Step 2: Label the cutout
        val inputImage = InputImage.fromBitmap(cutoutBitmap, 0)
        val labelResult = imageLabeler.labelImage(inputImage)
        val labels = labelResult.getOrElse { e ->
            return Result.failure(Exception("Labeling failed: ${e.message}", e))
        }

        // Step 3: Map labels to category
        val labelStrings = labels.map { it.text }
        val bestMatch = LabelCategoryMapper.mapBestMatch(labelStrings)
        val category = bestMatch?.first ?: ClothingCategory.TOP
        val matchedLabel = bestMatch?.second
        val confidence = if (matchedLabel != null) {
            labels.find { it.text.equals(matchedLabel, ignoreCase = true) }?.confidence ?: 0f
        } else {
            0f
        }

        // Step 4: Extract colors
        val colors = colorExtractor.extractColors(cutoutBitmap)

        val analysisResult = AnalysisResult(
            category = category,
            subcategory = matchedLabel,
            labels = labelStrings,
            confidence = confidence,
            colors = colors
        )

        return Result.success(SegmentedResult(cutoutBitmap, analysisResult))
    }

    fun close() {
        imageSegmenter.close()
    }
}
