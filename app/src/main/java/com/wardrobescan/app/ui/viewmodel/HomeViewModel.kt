package com.wardrobescan.app.ui.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.wardrobescan.app.data.model.ClothingItem
import com.wardrobescan.app.data.model.Occasion
import com.wardrobescan.app.data.model.Outfit
import com.wardrobescan.app.data.model.WeatherData
import com.wardrobescan.app.data.repository.AuthRepository
import com.wardrobescan.app.data.repository.WardrobeRepository
import com.wardrobescan.app.data.repository.WeatherRepository
import com.wardrobescan.app.domain.OutfitSuggestionEngine
import com.wardrobescan.app.util.Analytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val weather: WeatherData? = null,
    val suggestedOutfits: List<Outfit> = emptyList(),
    val wardrobeItems: List<ClothingItem> = emptyList(),
    val selectedOccasion: Occasion = Occasion.CASUAL,
    val error: String? = null,
    val locationPermissionNeeded: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val weatherRepository: WeatherRepository,
    private val wardrobeRepository: WardrobeRepository,
    private val authRepository: AuthRepository,
    private val outfitEngine: OutfitSuggestionEngine,
    private val analytics: Analytics
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            wardrobeRepository.observeItems(userId).collect { items ->
                _uiState.value = _uiState.value.copy(wardrobeItems = items)
                refreshOutfitSuggestions()
            }
        }

        fetchWeather()
    }

    @SuppressLint("MissingPermission")
    private fun fetchWeather() {
        val context = getApplication<Application>()
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _uiState.value = _uiState.value.copy(
                locationPermissionNeeded = true,
                isLoading = false
            )
            return
        }

        viewModelScope.launch {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val location = fusedClient.lastLocation.await()

                if (location != null) {
                    val result = weatherRepository.getWeather(location.latitude, location.longitude)
                    result.fold(
                        onSuccess = { weather ->
                            _uiState.value = _uiState.value.copy(
                                weather = weather,
                                isLoading = false
                            )
                            refreshOutfitSuggestions()
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                error = "Weather error: ${error.message}",
                                isLoading = false
                            )
                        }
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Could not get location",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Location error: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun onLocationPermissionGranted() {
        _uiState.value = _uiState.value.copy(locationPermissionNeeded = false)
        fetchWeather()
    }

    fun onOccasionSelected(occasion: Occasion) {
        _uiState.value = _uiState.value.copy(selectedOccasion = occasion)
        refreshOutfitSuggestions()
    }

    private fun refreshOutfitSuggestions() {
        val state = _uiState.value
        val weather = state.weather ?: return
        val items = state.wardrobeItems
        if (items.isEmpty()) return

        val outfits = outfitEngine.suggest(items, weather, state.selectedOccasion)
        _uiState.value = _uiState.value.copy(suggestedOutfits = outfits)

        outfits.forEach { outfit ->
            analytics.outfitShown(state.selectedOccasion.name, outfit.itemIds.size)
        }
    }

    fun refreshWeather() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        fetchWeather()
    }
}
