package com.wardrobescan.app.ui.viewmodel

import android.graphics.Bitmap
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

data class ScanUiState(
    val isCapturing: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isSaving: Boolean = false,
    val capturedBitmap: Bitmap? = null,
    val cutoutBitmap: Bitmap? = null,
    val analysisResult: AnalysisResult? = null,
    val needsManualCategory: Boolean = false,
    val selectedCategory: ClothingCategory? = null,
    val userNotes: String = "",
    val warmthScore: Int = 3,
    val waterproof: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val clothingAnalyzer: ClothingAnalyzer,
    private val storageRepository: StorageRepository,
    private val wardrobeRepository: WardrobeRepository,
    private val authRepository: AuthRepository,
    private val analytics: Analytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onPhotoCaptured(bitmap: Bitmap) {
        analytics.scanStarted()
        _uiState.value = _uiState.value.copy(
            capturedBitmap = bitmap,
            isAnalyzing = true,
            error = null
        )
        analyzeImage(bitmap)
    }

    private fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            val result = clothingAnalyzer.analyze(bitmap)
            result.fold(
                onSuccess = { segmented ->
                    val needsManual = segmented.analysisResult.confidence < ClothingAnalyzer.CONFIDENCE_THRESHOLD
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        cutoutBitmap = segmented.cutoutBitmap,
                        analysisResult = segmented.analysisResult,
                        selectedCategory = segmented.analysisResult.category,
                        needsManualCategory = needsManual
                    )
                    analytics.scanSuccess(
                        segmented.analysisResult.category.name,
                        segmented.analysisResult.confidence
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        error = "Analysis failed: ${error.message}"
                    )
                }
            )
        }
    }

    fun onCategorySelected(category: ClothingCategory) {
        val prev = _uiState.value.selectedCategory
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            needsManualCategory = false
        )
        if (prev != category) {
            analytics.scanManualFix(prev?.name ?: "unknown", category.name)
        }
    }

    fun onWarmthScoreChanged(score: Int) {
        _uiState.value = _uiState.value.copy(warmthScore = score)
    }

    fun onWaterproofToggled(waterproof: Boolean) {
        _uiState.value = _uiState.value.copy(waterproof = waterproof)
    }

    fun onNotesChanged(notes: String) {
        _uiState.value = _uiState.value.copy(userNotes = notes)
    }

    fun saveItem(capturedImageUri: Uri) {
        val state = _uiState.value
        val userId = authRepository.currentUser?.uid ?: return

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            try {
                // Upload original image
                val originalUrl = storageRepository.uploadOriginalImage(userId, capturedImageUri)
                    .getOrThrow()

                // Upload cutout image
                var cutoutUrl = ""
                state.cutoutBitmap?.let { cutout ->
                    val stream = ByteArrayOutputStream()
                    cutout.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    cutoutUrl = storageRepository.uploadCutoutImage(userId, stream.toByteArray())
                        .getOrThrow()
                }

                // Create clothing item
                val item = ClothingItem(
                    userId = userId,
                    category = state.selectedCategory?.name?.lowercase() ?: "top",
                    subcategory = state.analysisResult?.subcategory ?: "",
                    labels = state.analysisResult?.labels ?: emptyList(),
                    colors = state.analysisResult?.colors ?: emptyList(),
                    imageUrl = originalUrl,
                    cutoutUrl = cutoutUrl,
                    warmthScore = state.warmthScore,
                    waterproof = state.waterproof,
                    userNotes = state.userNotes,
                    confidence = state.analysisResult?.confidence ?: 0f
                )

                wardrobeRepository.addItem(userId, item).getOrThrow()

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saved = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save: ${e.message}"
                )
            }
        }
    }

    fun resetScan() {
        _uiState.value = ScanUiState()
    }

    override fun onCleared() {
        super.onCleared()
        clothingAnalyzer.close()
    }
}
