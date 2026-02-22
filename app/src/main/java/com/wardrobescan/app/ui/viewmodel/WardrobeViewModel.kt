package com.wardrobescan.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wardrobescan.app.data.model.ClothingCategory
import com.wardrobescan.app.data.repository.AuthRepository
import com.wardrobescan.app.domain.DeleteClothingItemUseCase
import com.wardrobescan.app.domain.GetPagedWardrobeItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import com.wardrobescan.app.data.model.ClothingItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WardrobeUiState(
    val selectedCategory: ClothingCategory? = null,
    val error: String? = null
)

@HiltViewModel
class WardrobeViewModel @Inject constructor(
    private val getPagedWardrobeItems: GetPagedWardrobeItemsUseCase,
    private val deleteClothingItem: DeleteClothingItemUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WardrobeUiState())
    val uiState: StateFlow<WardrobeUiState> = _uiState.asStateFlow()

    private val _categoryFilter = MutableStateFlow<ClothingCategory?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingItems: Flow<PagingData<ClothingItem>> = _categoryFilter
        .flatMapLatest { category ->
            val userId = authRepository.currentUser?.uid
                ?: return@flatMapLatest flowOf(PagingData.empty())
            getPagedWardrobeItems(userId, category?.name?.lowercase())
        }
        .cachedIn(viewModelScope)

    fun onCategoryFilter(category: ClothingCategory?) {
        _categoryFilter.value = category
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun deleteItem(itemId: String) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            deleteClothingItem(userId, itemId)
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
