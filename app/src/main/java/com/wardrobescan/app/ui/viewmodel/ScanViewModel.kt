package com.wardrobescan.app.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardrobescan.app.data.model.AnalysisResult
import com.wardrobescan.app.data.model.ClothingCategory
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

data class ScanItemState(
    val id: String,
    val uri: Uri,
    val bitmap: Bitmap? = null,
    val cutoutBitmap: Bitmap? = null,
    val analysisResult: AnalysisResult? = null,
    val selectedCategory: ClothingCategory? = null,
    val needsManualCategory: Boolean = false,
    val warmthScore: Int = 3,
    val waterproof: Boolean = false,
    val userNotes: String = "",
    val isAnalyzing: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

data class ScanUiState(
    val items: List<ScanItemState> = emptyList()
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clothingAnalyzer: ClothingAnalyzer,
    private val storageRepository: StorageRepository,
    private val wardrobeRepository: WardrobeRepository,
    private val authRepository: AuthRepository,
    private val analytics: Analytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val analysisMutex = Mutex()

    fun addImages(uris: List<Uri>) {
        analytics.scanStarted()
        val newItems = uris.map { uri ->
            ScanItemState(id = UUID.randomUUID().toString(), uri = uri, isAnalyzing = true)
        }
        _uiState.update { it.copy(items = it.items + newItems) }
        newItems.forEach { item ->
            viewModelScope.launch { processItem(item.id) }
        }
    }

    fun retryItem(id: String) {
        val item = getItem(id) ?: return
        if (item.analysisResult != null) {
            // Analysis succeeded previously — retry the save
            updateItem(id) { it.copy(isSaving = true, error = null) }
            viewModelScope.launch { saveItem(id) }
        } else {
            // Analysis failed — retry full pipeline
            updateItem(id) { it.copy(isAnalyzing = true, error = null) }
            viewModelScope.launch { processItem(id) }
        }
    }

    fun removeItem(id: String) {
        _uiState.update { s -> s.copy(items = s.items.filter { it.id != id }) }
    }

    fun updateCategory(id: String, category: ClothingCategory) {
        val prev = getItem(id)?.selectedCategory
        updateItem(id) { it.copy(selectedCategory = category, needsManualCategory = false) }
        if (prev != category) analytics.scanManualFix(prev?.name ?: "unknown", category.name)
    }

    fun updateWarmthScore(id: String, score: Int) {
        updateItem(id) { it.copy(warmthScore = score) }
    }

    fun updateWaterproof(id: String, waterproof: Boolean) {
        updateItem(id) { it.copy(waterproof = waterproof) }
    }

    fun updateNotes(id: String, notes: String) {
        updateItem(id) { it.copy(userNotes = notes) }
    }

    private suspend fun processItem(id: String) {
        val item = getItem(id) ?: return

        val bitmap = loadBitmapFromUri(item.uri)
        if (bitmap == null) {
            updateItem(id) { it.copy(isAnalyzing = false, error = "Failed to load image") }
            return
        }
        updateItem(id) { it.copy(bitmap = bitmap) }

        val result = analysisMutex.withLock { clothingAnalyzer.analyze(bitmap) }
        result.fold(
            onSuccess = { segmented ->
                val needsManual = segmented.analysisResult.confidence < ClothingAnalyzer.CONFIDENCE_THRESHOLD
                updateItem(id) {
                    it.copy(
                        isAnalyzing = false,
                        cutoutBitmap = segmented.cutoutBitmap,
                        analysisResult = segmented.analysisResult,
                        selectedCategory = segmented.analysisResult.category,
                        needsManualCategory = needsManual,
                        isSaving = true
                    )
                }
                analytics.scanSuccess(segmented.analysisResult.category.name, segmented.analysisResult.confidence)
                saveItem(id)
            },
            onFailure = { e ->
                updateItem(id) { it.copy(isAnalyzing = false, error = "Analysis failed: ${e.message}") }
            }
        )
    }

    private suspend fun saveItem(id: String) {
        val item = getItem(id) ?: return
        val userId = authRepository.currentUser?.uid ?: return

        try {
            val originalUrl = storageRepository.uploadOriginalImage(userId, item.uri).getOrThrow()

            var cutoutUrl = ""
            item.cutoutBitmap?.let { cutout ->
                val stream = ByteArrayOutputStream()
                cutout.compress(Bitmap.CompressFormat.PNG, 100, stream)
                cutoutUrl = storageRepository.uploadCutoutImage(userId, stream.toByteArray()).getOrThrow()
            }

            // Re-read item to pick up any category/warmth edits made during upload
            val current = getItem(id) ?: item
            val clothingItem = ClothingItem(
                userId = userId,
                category = current.selectedCategory?.name?.lowercase() ?: "top",
                subcategory = current.analysisResult?.subcategory ?: "",
                labels = current.analysisResult?.labels ?: emptyList(),
                colors = current.analysisResult?.colors ?: emptyList(),
                imageUrl = originalUrl,
                cutoutUrl = cutoutUrl,
                warmthScore = current.warmthScore,
                waterproof = current.waterproof,
                userNotes = current.userNotes,
                confidence = current.analysisResult?.confidence ?: 0f
            )

            wardrobeRepository.addItem(userId, clothingItem).getOrThrow()
            _uiState.update { s -> s.copy(items = s.items.filter { it.id != id }) }
        } catch (e: Exception) {
            updateItem(id) { it.copy(isSaving = false, error = "Failed to save: ${e.message}") }
        }
    }

    private suspend fun loadBitmapFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (uri.scheme == "file") {
                BitmapFactory.decodeFile(uri.path)
            } else {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getItem(id: String): ScanItemState? = _uiState.value.items.find { it.id == id }

    private fun updateItem(id: String, transform: (ScanItemState) -> ScanItemState) {
        _uiState.update { s -> s.copy(items = s.items.map { if (it.id == id) transform(it) else it }) }
    }

    override fun onCleared() {
        super.onCleared()
        clothingAnalyzer.close()
    }
}
