package com.wardrobescan.app.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardrobescan.app.data.model.ClothingItem
import com.wardrobescan.app.data.repository.AuthRepository
import com.wardrobescan.app.data.repository.StorageRepository
import com.wardrobescan.app.data.repository.WardrobeRepository
import com.wardrobescan.app.ml.ClothingAnalyzer
import com.wardrobescan.app.util.Analytics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

/** Progress stage of one image in the batch pipeline. */
enum class ProcessingStatus { PENDING, ANALYZING, SAVING, ERROR }

/**
 * Tracks one captured image as it moves through analysis → upload → save.
 *
 * @param id             Stable key for Compose lazy-list identity.
 * @param uri            Source URI of the captured photo.
 * @param thumbnailBitmap Decoded bitmap shown as a preview; null until loaded.
 * @param status         Current stage in the pipeline.
 * @param errorMessage   Human-readable error when [status] is [ProcessingStatus.ERROR].
 */
data class ProcessingItemState(
    val id: String,
    val uri: Uri,
    val thumbnailBitmap: Bitmap? = null,
    val status: ProcessingStatus = ProcessingStatus.PENDING,
    val errorMessage: String? = null
)

/**
 * Manages the two-stage multi-capture pipeline:
 *
 *  **Stage 1 – Camera screen**: [addCapturedUri] accumulates captured URIs in [capturedUris]
 *  without starting any background work.
 *
 *  **Stage 2 – Wardrobe screen**: [startProcessing] atomically drains [capturedUris] into
 *  [processingItems] and launches a coroutine per image (analyse → upload → save). When an
 *  item is saved the WardrobeRepository's Flow surfaces it in the grid; the processing card
 *  is then removed from [processingItems].
 *
 * This ViewModel is obtained via [hiltViewModel] at the [NavGraph][com.wardrobescan.app.ui.navigation.NavGraph]
 * level so the **same instance** is shared by ScanScreen and WardrobeScreen throughout
 * the session without coupling them directly to each other.
 */
@HiltViewModel
class BulkScanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clothingAnalyzer: ClothingAnalyzer,
    private val storageRepository: StorageRepository,
    private val wardrobeRepository: WardrobeRepository,
    private val authRepository: AuthRepository,
    private val analytics: Analytics
) : ViewModel() {

    /** URIs captured in the camera view, not yet processed (stage 1). */
    private val _capturedUris = MutableStateFlow<List<Uri>>(emptyList())
    val capturedUris: StateFlow<List<Uri>> = _capturedUris.asStateFlow()

    /** Live processing state for each in-flight image, displayed on WardrobeScreen (stage 2). */
    private val _processingItems = MutableStateFlow<List<ProcessingItemState>>(emptyList())
    val processingItems: StateFlow<List<ProcessingItemState>> = _processingItems.asStateFlow()

    /** Serialises access to the MediaPipe segmenter which is not thread-safe. */
    private val analysisMutex = Mutex()

    // ── Stage 1: camera screen ────────────────────────────────────────────────

    /** Appends [uri] to the list of pending captures without starting any work. */
    fun addCapturedUri(uri: Uri) {
        _capturedUris.update { it + uri }
    }

    /** Removes a single URI from the pending list before Done is tapped. */
    fun removeCapturedUri(uri: Uri) {
        _capturedUris.update { current -> current.filter { it != uri } }
    }

    /**
     * Discards all pending captured URIs without starting processing.
     * Called when the user opens ScanScreen to ensure no stale URIs from a previous
     * session (e.g., backed out without tapping Done) pollute the new session.
     */
    fun clearPendingUris() {
        _capturedUris.value = emptyList()
    }

    // ── Stage 2: triggered on Done ────────────────────────────────────────────

    /**
     * Atomically moves all pending URIs into [processingItems] and launches a coroutine
     * for each image. Safe to call immediately before navigating to WardrobeScreen — by
     * the time the Compose frame settles, at least [ProcessingStatus.PENDING] cards will
     * already be visible.
     */
    fun startProcessing() {
        val uris = _capturedUris.value
        if (uris.isEmpty()) return
        _capturedUris.value = emptyList()

        analytics.scanStarted()
        val newItems = uris.map { uri ->
            ProcessingItemState(id = UUID.randomUUID().toString(), uri = uri)
        }
        _processingItems.update { it + newItems }

        // Launch each item's pipeline concurrently; the analysisMutex serialises
        // the heavy ML step while upload/save run in parallel for other items.
        newItems.forEach { item ->
            viewModelScope.launch { processItem(item.id) }
        }
    }

    // ── Pipeline internals ────────────────────────────────────────────────────

    private suspend fun processItem(id: String) {
        val item = getItem(id) ?: return

        val bitmap = loadBitmapFromUri(item.uri)
        if (bitmap == null) {
            updateItem(id) { it.copy(status = ProcessingStatus.ERROR, errorMessage = "Failed to load image") }
            return
        }
        updateItem(id) { it.copy(thumbnailBitmap = bitmap, status = ProcessingStatus.ANALYZING) }

        val result = analysisMutex.withLock { clothingAnalyzer.analyze(bitmap) }
        result.fold(
            onSuccess = { segmented ->
                analytics.scanSuccess(
                    segmented.analysisResult.category.name,
                    segmented.analysisResult.confidence
                )
                updateItem(id) { it.copy(status = ProcessingStatus.SAVING) }
                saveItem(id, item.uri, segmented)
            },
            onFailure = { e ->
                updateItem(id) {
                    it.copy(
                        status = ProcessingStatus.ERROR,
                        errorMessage = "Analysis failed: ${e.message}"
                    )
                }
            }
        )
    }

    private suspend fun saveItem(
        id: String,
        uri: Uri,
        segmented: ClothingAnalyzer.SegmentedResult
    ) {
        val userId = authRepository.currentUser?.uid ?: run {
            updateItem(id) { it.copy(status = ProcessingStatus.ERROR, errorMessage = "Not authenticated") }
            return
        }

        try {
            val originalUrl = storageRepository.uploadOriginalImage(userId, uri).getOrThrow()

            val stream = ByteArrayOutputStream()
            segmented.cutoutBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val cutoutUrl = storageRepository.uploadCutoutImage(userId, stream.toByteArray()).getOrThrow()

            val clothingItem = ClothingItem(
                userId = userId,
                category = segmented.analysisResult.category.name.lowercase(),
                subcategory = segmented.analysisResult.subcategory ?: "",
                labels = segmented.analysisResult.labels,
                colors = segmented.analysisResult.colors,
                imageUrl = originalUrl,
                cutoutUrl = cutoutUrl,
                warmthScore = 3,
                waterproof = false,
                userNotes = "",
                confidence = segmented.analysisResult.confidence
            )

            wardrobeRepository.addItem(userId, clothingItem).getOrThrow()
            // Remove the placeholder card; the WardrobeRepository Flow will surface the new item.
            _processingItems.update { current -> current.filter { it.id != id } }
        } catch (e: Exception) {
            updateItem(id) {
                it.copy(
                    status = ProcessingStatus.ERROR,
                    errorMessage = "Failed to save: ${e.message}"
                )
            }
        }
    }

    private suspend fun loadBitmapFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (uri.scheme == "file") BitmapFactory.decodeFile(uri.path)
            else context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun getItem(id: String): ProcessingItemState? =
        _processingItems.value.find { it.id == id }

    private fun updateItem(id: String, transform: (ProcessingItemState) -> ProcessingItemState) {
        _processingItems.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }
}
