package com.wardrobescan.app.data.model

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val useCelsius: Boolean = true,
    val city: String = "",
    val onboardingComplete: Boolean = false
)
