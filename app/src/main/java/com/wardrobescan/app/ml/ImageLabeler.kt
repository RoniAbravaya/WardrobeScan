package com.wardrobescan.app.ml

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class LabelResult(
    val text: String,
    val confidence: Float
)

@Singleton
class ImageLabeler @Inject constructor() {

    private val labeler by lazy {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build()
        ImageLabeling.getClient(options)
    }

    /**
     * Runs ML Kit Image Labeling on the given image.
     * Returns labels sorted by confidence descending.
     */
    suspend fun labelImage(inputImage: InputImage): Result<List<LabelResult>> {
        return try {
            val labels = labeler.process(inputImage).await()
            val results = labels.map { label ->
                LabelResult(
                    text = label.text,
                    confidence = label.confidence
                )
            }.sortedByDescending { it.confidence }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
