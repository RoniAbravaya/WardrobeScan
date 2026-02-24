package com.wardrobescan.app.data.model

import com.google.firebase.Timestamp

/**
 * Aggregated marketing profile computed server-side by the Cloud Function
 * `generateStyleProfileOnItem` / `generateStyleProfileOnOutfit`.
 *
 * Stored at: users/{userId}/styleProfile/data
 * Written only by Cloud Functions (admin SDK). Read-only for the client.
 */
data class StyleProfile(
    /** When the Cloud Function last wrote this document. */
    val generatedAt: Timestamp? = null,

    /** Schema version — increment if the field layout changes. */
    val schemaVersion: Int = 1,

    // -----------------------------------------------------------------------
    // Wardrobe size
    // -----------------------------------------------------------------------
    val itemCount: Int = 0,
    val outfitCount: Int = 0,

    // -----------------------------------------------------------------------
    // Colour analysis
    // -----------------------------------------------------------------------
    /** Top-5 colours weighted across all items (hex + name + weightedPercentage). */
    val dominantColors: List<DominantColor> = emptyList(),

    /** Overall palette tone: "warm" | "cool" | "neutral" | "bold" */
    val colorTone: String = "",

    /** 0.0 = monochrome wardrobe, 1.0 = maximally diverse colour mix. */
    val colorDiversity: Float = 0f,

    // -----------------------------------------------------------------------
    // Category distribution
    // -----------------------------------------------------------------------
    /** Count of items per ClothingCategory. Keys: TOP, BOTTOM, OUTERWEAR, DRESS, SHOES, ACCESSORY */
    val categoryDistribution: Map<String, Int> = emptyMap(),

    /** The category with the most items. */
    val dominantCategory: String = "",

    // -----------------------------------------------------------------------
    // Style persona
    // -----------------------------------------------------------------------
    /** Normalised scores (sum to 1.0) for casual / professional / sporty / elegant / streetwear. */
    val personaScores: Map<String, Float> = emptyMap(),

    val primaryPersona: String = "",
    val secondaryPersona: String? = null,

    // -----------------------------------------------------------------------
    // Occasion preferences
    // -----------------------------------------------------------------------
    /** Normalised ratios of outfit occasions: casual / work / going_out. */
    val occasionPreferences: Map<String, Float> = emptyMap(),
    val primaryOccasion: String = "",

    // -----------------------------------------------------------------------
    // Seasonal coverage
    // -----------------------------------------------------------------------
    /** Number of items applicable to each season. */
    val seasonalCoverage: Map<String, Int> = emptyMap(),

    /** Seasons where the user has fewer than the threshold number of items. */
    val seasonalGaps: List<String> = emptyList(),

    // -----------------------------------------------------------------------
    // Wardrobe completeness & shopping gaps
    // -----------------------------------------------------------------------
    /** 0.0–1.0 score based on category balance and seasonal coverage. */
    val completenessScore: Float = 0f,

    /** List of { category, reason } maps describing wardrobe gaps. */
    val shoppingGaps: List<Map<String, String>> = emptyList(),

    // -----------------------------------------------------------------------
    // Engagement signals
    // -----------------------------------------------------------------------
    val avgOutfitRating: Float = 0f,
    val savedOutfitRate: Float = 0f,

    // -----------------------------------------------------------------------
    // Marketing segments
    // -----------------------------------------------------------------------
    /**
     * Flat string tags for use in marketing platforms, e.g.:
     * ["persona_casual", "color_cool", "missing_summer_wardrobe", "needs_footwear"]
     */
    val marketingSegments: List<String> = emptyList()
)
