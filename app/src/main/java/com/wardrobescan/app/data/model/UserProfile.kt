package com.wardrobescan.app.data.model

import com.google.firebase.Timestamp

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val useCelsius: Boolean = true,
    val city: String = "",
    val onboardingComplete: Boolean = false,

    /**
     * Whether the user has opted in to wardrobe-based marketing analysis.
     * Null means the consent prompt has not been shown yet.
     * The Cloud Function will not generate a StyleProfile unless this is true.
     */
    val marketingOptIn: Boolean? = null,

    /** Timestamp of the last consent decision (opt-in or opt-out). */
    val marketingConsentAt: Timestamp? = null
)
