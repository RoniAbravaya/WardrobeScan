package com.wardrobescan.app.util

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Analytics @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) {
    // --- Scan ---

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

    // --- Wardrobe items ---

    fun itemAdded(category: String, season: String) {
        firebaseAnalytics.logEvent("item_added") {
            param("category", category)
            param("season", season)
        }
    }

    fun itemDeleted(category: String) {
        firebaseAnalytics.logEvent("item_deleted") {
            param("category", category)
        }
    }

    fun itemViewed(itemId: String, category: String) {
        firebaseAnalytics.logEvent("item_viewed") {
            param("item_id", itemId)
            param("category", category)
        }
    }

    fun itemUpdated(itemId: String) {
        firebaseAnalytics.logEvent("item_updated") {
            param("item_id", itemId)
        }
    }

    fun wardrobeFilterChanged(category: String) {
        firebaseAnalytics.logEvent("wardrobe_filter_changed") {
            param("category", category)
        }
    }

    // --- Outfits ---

    fun outfitShown(occasion: String, itemCount: Int) {
        firebaseAnalytics.logEvent("outfit_shown") {
            param("occasion", occasion)
            param("item_count", itemCount.toLong())
        }
    }

    fun outfitGenerated(occasion: String, weatherSummary: String, suggestionCount: Int) {
        firebaseAnalytics.logEvent("outfit_generated") {
            param("occasion", occasion)
            param("weather_summary", weatherSummary)
            param("suggestion_count", suggestionCount.toLong())
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

    fun outfitDeleted(outfitId: String) {
        firebaseAnalytics.logEvent("outfit_deleted") {
            param("outfit_id", outfitId)
        }
    }

    // --- Auth ---

    fun userSignedIn(method: String) {
        firebaseAnalytics.logEvent("user_signed_in") {
            param("method", method)
        }
    }

    fun userSignedOut() {
        firebaseAnalytics.logEvent("user_signed_out") {}
    }
}
