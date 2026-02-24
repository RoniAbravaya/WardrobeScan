package com.wardrobescan.app.util

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Analytics @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) {
    fun scanStarted() {
        firebaseAnalytics.logEvent("scan_started") {}
    }

    fun scanSuccess(category: String, confidence: Float) {
        firebaseAnalytics.logEvent("scan_success") {
            param("category", category)
            param("confidence", confidence.toDouble())
        }
    }

    fun scanManualFix(originalCategory: String, fixedCategory: String) {
        firebaseAnalytics.logEvent("scan_manual_fix") {
            param("original_category", originalCategory)
            param("fixed_category", fixedCategory)
        }
    }

    fun outfitShown(occasion: String, itemCount: Int) {
        firebaseAnalytics.logEvent("outfit_shown") {
            param("occasion", occasion)
            param("item_count", itemCount.toLong())
        }
    }

    fun outfitSaved(outfitId: String) {
        firebaseAnalytics.logEvent("outfit_saved") {
            param("outfit_id", outfitId)
        }
    }

    fun outfitLiked(outfitId: String, rating: Int) {
        firebaseAnalytics.logEvent("outfit_liked") {
            param("outfit_id", outfitId)
            param("rating", rating.toLong())
        }
    }

    /** User responded to the marketing consent prompt. */
    fun marketingConsentGiven(optIn: Boolean) {
        firebaseAnalytics.logEvent("marketing_consent_given") {
            param("opt_in", if (optIn) 1L else 0L)
        }
    }

    /** User's StyleProfile was read by the app (e.g. for a future in-app personalisation feature). */
    fun styleProfileViewed(primaryPersona: String, segmentCount: Int) {
        firebaseAnalytics.logEvent("style_profile_viewed") {
            param("primary_persona", primaryPersona)
            param("segment_count", segmentCount.toLong())
        }
    }
}
