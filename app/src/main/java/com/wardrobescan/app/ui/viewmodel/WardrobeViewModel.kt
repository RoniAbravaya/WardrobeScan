package com.wardrobescan.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardrobescan.app.data.model.ClothingCategory
import com.wardrobescan.app.data.model.ClothingItem
import com.wardrobescan.app.data.repository.AuthRepository
import com.wardrobescan.app.data.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WardrobeUiState(
    val items: List<ClothingItem> = emptyList(),
    val filteredItems: List<ClothingItem> = emptyList(),
    val selectedCategory: ClothingCategory? = null,
    val selectedColor: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class WardrobeViewModel @Inject constructor(
    private val wardrobeRepository: WardrobeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WardrobeUiState())
    val uiState: StateFlow<WardrobeUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            wardrobeRepository.observeItems(userId).collect { items ->
                _uiState.value = _uiState.value.copy(
                    items = items,
                    isLoading = false
                )
                applyFilters()
            }
        }
    }

    fun onCategoryFilter(category: ClothingCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilters()
    }

    fun onColorFilter(color: String?) {
        _uiState.value = _uiState.value.copy(selectedColor = color)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = state.items

        state.selectedCategory?.let { category ->
            filtered = filtered.filter { it.categoryEnum == category }
        }

        state.selectedColor?.let { color ->
            filtered = filtered.filter { item ->
                item.colors.any { it.name.equals(color, ignoreCase = true) }
            }
        }

        _uiState.value = _uiState.value.copy(filteredItems = filtered)
    }

    suspend fun deleteItem(itemId: String): Result<Unit> {
        val userId = authRepository.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return wardrobeRepository.deleteItem(userId, itemId)
    }
}
