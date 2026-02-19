package com.wardrobescan.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardrobescan.app.data.model.ClothingItem
import com.wardrobescan.app.data.model.Outfit
import com.wardrobescan.app.data.repository.AuthRepository
import com.wardrobescan.app.data.repository.OutfitRepository
import com.wardrobescan.app.data.repository.WardrobeRepository
import com.wardrobescan.app.util.Analytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OutfitUiState(
    val savedOutfits: List<Outfit> = emptyList(),
    val allOutfits: List<Outfit> = emptyList(),
    val wardrobeItems: Map<String, ClothingItem> = emptyMap(),
    val showSavedOnly: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class OutfitViewModel @Inject constructor(
    private val outfitRepository: OutfitRepository,
    private val wardrobeRepository: WardrobeRepository,
    private val authRepository: AuthRepository,
    private val analytics: Analytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(OutfitUiState())
    val uiState: StateFlow<OutfitUiState> = _uiState.asStateFlow()

    init {
        loadOutfits()
    }

    private fun loadOutfits() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            combine(
                outfitRepository.observeOutfits(userId),
                wardrobeRepository.observeItems(userId)
            ) { outfits, items ->
                val itemMap = items.associateBy { it.id }
                outfits to itemMap
            }.collect { (outfits, itemMap) ->
                _uiState.value = _uiState.value.copy(
                    allOutfits = outfits,
                    savedOutfits = outfits.filter { it.saved },
                    wardrobeItems = itemMap,
                    isLoading = false
                )
            }
        }
    }

    fun toggleSavedFilter() {
        _uiState.value = _uiState.value.copy(showSavedOnly = !_uiState.value.showSavedOnly)
    }

    fun saveOutfit(outfit: Outfit) {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            val updated = outfit.copy(saved = true)
            if (outfit.id.isEmpty()) {
                val result = outfitRepository.saveOutfit(userId, updated)
                result.onSuccess { id -> analytics.outfitSaved(id) }
            } else {
                outfitRepository.updateOutfit(userId, updated)
                analytics.outfitSaved(outfit.id)
            }
        }
    }

    fun rateOutfit(outfit: Outfit, rating: Int) {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            val updated = outfit.copy(rating = rating)
            outfitRepository.updateOutfit(userId, updated)
            analytics.outfitLiked(outfit.id, rating)
        }
    }

    fun deleteOutfit(outfitId: String) {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            outfitRepository.deleteOutfit(userId, outfitId)
        }
    }
}
