package com.wardrobescan.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardrobescan.app.data.model.ClothingCategory
import com.wardrobescan.app.data.model.ClothingItem
import com.wardrobescan.app.data.repository.AuthRepository
import com.wardrobescan.app.data.repository.StorageRepository
import com.wardrobescan.app.data.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ItemDetailUiState(
    val item: ClothingItem? = null,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wardrobeRepository: WardrobeRepository,
    private val storageRepository: StorageRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val itemId: String = savedStateHandle.get<String>("itemId") ?: ""

    private val _uiState = MutableStateFlow(ItemDetailUiState())
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    init {
        loadItem()
    }

    private fun loadItem() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            val result = wardrobeRepository.getItem(userId, itemId)
            result.fold(
                onSuccess = { item ->
                    _uiState.value = _uiState.value.copy(item = item, isLoading = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message,
                        isLoading = false
                    )
                }
            )
        }
    }

    fun toggleEdit() {
        _uiState.value = _uiState.value.copy(isEditing = !_uiState.value.isEditing)
    }

    fun updateCategory(category: ClothingCategory) {
        _uiState.value = _uiState.value.copy(
            item = _uiState.value.item?.copy(category = category.name.lowercase())
        )
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(
            item = _uiState.value.item?.copy(userNotes = notes)
        )
    }

    fun updateWarmthScore(score: Int) {
        _uiState.value = _uiState.value.copy(
            item = _uiState.value.item?.copy(warmthScore = score)
        )
    }

    fun updateWaterproof(waterproof: Boolean) {
        _uiState.value = _uiState.value.copy(
            item = _uiState.value.item?.copy(waterproof = waterproof)
        )
    }

    fun saveChanges() {
        val userId = authRepository.currentUser?.uid ?: return
        val item = _uiState.value.item ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val result = wardrobeRepository.updateItem(userId, item)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                isEditing = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun deleteItem() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            try {
                _uiState.value.item?.imageUrl?.let { storageRepository.deleteImage(it) }
                _uiState.value.item?.cutoutUrl?.takeIf { it.isNotEmpty() }?.let {
                    storageRepository.deleteImage(it)
                }
                wardrobeRepository.deleteItem(userId, itemId).getOrThrow()
                _uiState.value = _uiState.value.copy(isDeleting = false, deleted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = "Delete failed: ${e.message}"
                )
            }
        }
    }
}
