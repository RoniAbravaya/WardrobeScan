package com.wardrobescan.app.ui.viewmodel

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wardrobescan.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class SettingsUiState(
    val useCelsius: Boolean = true,
    val onboardingComplete: Boolean = false,
    val displayName: String = "",
    val email: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository
) : AndroidViewModel(application) {

    companion object {
        val USE_CELSIUS = booleanPreferencesKey("use_celsius")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    private val dataStore = application.dataStore

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            val user = authRepository.currentUser
            _uiState.value = SettingsUiState(
                useCelsius = prefs[USE_CELSIUS] ?: true,
                onboardingComplete = prefs[ONBOARDING_COMPLETE] ?: false,
                displayName = user?.displayName ?: "",
                email = user?.email ?: ""
            )
        }
    }

    fun toggleTemperatureUnit() {
        viewModelScope.launch {
            val newValue = !_uiState.value.useCelsius
            dataStore.edit { it[USE_CELSIUS] = newValue }
            _uiState.value = _uiState.value.copy(useCelsius = newValue)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            dataStore.edit { it[ONBOARDING_COMPLETE] = true }
            _uiState.value = _uiState.value.copy(onboardingComplete = true)
        }
    }

    fun isOnboardingComplete(): Boolean = _uiState.value.onboardingComplete

    fun signOut() {
        authRepository.signOut()
    }
}
