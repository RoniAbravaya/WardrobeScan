const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const storage = admin.storage();

// ---------------------------------------------------------------------------
// Persona label sets — derived from existing ML-extracted item labels
// ---------------------------------------------------------------------------
const CASUAL_LABELS = new Set([
    "hoodie", "jeans", "sneaker", "sneakers", "t-shirt", "tee", "denim",
    "sweatshirt", "tracksuit", "chinos", "polo", "leggings", "joggers",
    "sweatpants", "flannel", "canvas shoe",
]);
const PROFESSIONAL_LABELS = new Set([
    "blazer", "trousers", "dress shirt", "button-down", "tie", "suit",
    "loafer", "pencil skirt", "blouse", "oxford", "chino", "waistcoat",
    "slacks", "turtleneck", "brogues",
]);
const SPORTY_LABELS = new Set([
    "shorts", "leggings", "track", "sport", "athletic", "gym", "running",
    "yoga", "jersey", "cycling", "compression", "windbreaker", "trainer",
]);
const ELEGANT_LABELS = new Set([
    "gown", "evening dress", "cocktail", "formal", "silk", "heel", "heels",
    "clutch", "satin", "lace", "velvet", "bodycon", "maxi dress",
]);
const STREETWEAR_LABELS = new Set([
    "hoodie", "cargo", "cap", "graphic", "bomber", "chunky", "oversized",
    "beanie", "bucket hat", "puffer", "high-top",
]);

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Convert a hex colour string to HSV. Returns { h, s, v } with h in [0,360). */
function hexToHsv(hex) {
    const r = parseInt(hex.slice(1, 3), 16) / 255;
    const g = parseInt(hex.slice(3, 5), 16) / 255;
    const b = parseInt(hex.slice(5, 7), 16) / 255;
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    const d = max - min;
    let h = 0;
    if (d !== 0) {
        if (max === r) h = ((g - b) / d + 6) % 6;
        else if (max === g) h = (b - r) / d + 2;
        else h = (r - g) / d + 4;
        h *= 60;
    }
    const s = max === 0 ? 0 : d / max;
    return { h, s, v: max };
}

/**
 * Compute colour tone ("warm" | "cool" | "neutral" | "bold") from a list of
 * items. Each item has a `colors` array of { hex, name, percentage }.
 */
function computeColorTone(items) {
    const weights = { warm: 0, cool: 0, neutral: 0 };
    let totalSaturation = 0;
    let totalWeight = 0;
    let maxSaturation = 0;

    for (const item of items) {
        for (const color of (item.colors || [])) {
            if (!color.hex || color.hex.length < 7) continue;
            try {
                const { h, s } = hexToHsv(color.hex);
                const w = color.percentage || 1;
                totalWeight += w;
                totalSaturation += s * w;
                if (s > maxSaturation) maxSaturation = s;

                if (s < 0.2) {
                    weights.neutral += w;
                } else if ((h >= 0 && h < 60) || (h >= 300 && h <= 360)) {
                    weights.warm += w;
                } else if (h >= 180 && h < 270) {
                    weights.cool += w;
                } else {
                    // mixed — split equally
                    weights.warm += w * 0.5;
                    weights.cool += w * 0.5;
                }
            } catch (_) { /* skip malformed hex */ }
        }
    }

    if (totalWeight === 0) return "neutral";

    const avgSaturation = totalSaturation / totalWeight;
    const diversity = computeColorDiversity(items);
    if (avgSaturation > 0.55 && diversity > 0.6) return "bold";

    const winner = Object.entries(weights).sort((a, b) => b[1] - a[1])[0][0];
    return winner;
}

/** Colour diversity: ratio of distinct colour names to total colour references. */
function computeColorDiversity(items) {
    const seen = new Set();
    let total = 0;
    for (const item of items) {
        for (const color of (item.colors || [])) {
            if (color.name) {
                seen.add(color.name.toLowerCase());
                total++;
            }
        }
    }
    if (total === 0) return 0;
    return Math.min(seen.size / total, 1);
}

/**
 * Compute top-5 dominant colours weighted across all items.
 * Returns [{ hex, name, weightedPercentage }]
 */
function computeDominantColors(items) {
    const colorMap = {};
    for (const item of items) {
        for (const color of (item.colors || [])) {
            if (!color.hex || !color.name) continue;
            const key = color.name.toLowerCase();
            if (!colorMap[key]) colorMap[key] = { hex: color.hex, name: color.name, total: 0, count: 0 };
            colorMap[key].total += color.percentage || 0;
            colorMap[key].count++;
        }
    }
    const totalItems = items.length || 1;
    return Object.values(colorMap)
        .map(c => ({ hex: c.hex, name: c.name, weightedPercentage: c.total / totalItems }))
        .sort((a, b) => b.weightedPercentage - a.weightedPercentage)
        .slice(0, 5);
}

/** Count items by ClothingCategory enum string. */
function computeCategoryDistribution(items) {
    const dist = { TOP: 0, BOTTOM: 0, OUTERWEAR: 0, DRESS: 0, SHOES: 0, ACCESSORY: 0 };
    for (const item of items) {
        const cat = (item.category || "").toUpperCase();
        if (cat in dist) dist[cat]++;
    }
    return dist;
}

/**
 * Compute persona scores from items + outfits.
 * Returns { casual, professional, sporty, elegant, streetwear } summing to 1.0.
 */
function computePersonaScores(items, outfits) {
    const raw = { casual: 0, professional: 0, sporty: 0, elegant: 0, streetwear: 0 };

    for (const item of items) {
        const itemLabels = (item.labels || []).map(l => l.toLowerCase());
        for (const label of itemLabels) {
            if (CASUAL_LABELS.has(label)) raw.casual++;
            if (PROFESSIONAL_LABELS.has(label)) raw.professional++;
            if (SPORTY_LABELS.has(label)) { raw.sporty++; if (item.breathable) raw.sporty += 0.5; }
            if (ELEGANT_LABELS.has(label)) raw.elegant++;
            if (STREETWEAR_LABELS.has(label)) raw.streetwear++;
        }
        // Breathable + low warmth boosts sporty
        if (item.breathable && (item.warmthScore || 3) <= 2) raw.sporty += 0.5;
    }

    // Outfit occasion signals
    for (const outfit of outfits) {
        const occ = (outfit.occasion || "").toLowerCase();
        const weight = outfit.saved ? 1.5 : 1;
        if (occ === "casual") raw.casual += weight;
        if (occ === "work") raw.professional += weight;
        if (occ === "going_out") raw.elegant += weight * (outfit.rating >= 4 ? 1.5 : 1);
    }

    const total = Object.values(raw).reduce((a, b) => a + b, 0) || 1;
    const scores = {};
    for (const [k, v] of Object.entries(raw)) scores[k] = Math.round((v / total) * 1000) / 1000;
    return scores;
}

/** Derive occasion preference ratios from outfits. */
function computeOccasionPreferences(outfits) {
    const counts = { casual: 0, work: 0, going_out: 0 };
    for (const outfit of outfits) {
        const occ = (outfit.occasion || "").toLowerCase();
        if (occ in counts) counts[occ]++;
    }
    const total = Object.values(counts).reduce((a, b) => a + b, 0) || 1;
    const prefs = {};
    for (const [k, v] of Object.entries(counts)) prefs[k] = Math.round((v / total) * 1000) / 1000;
    return prefs;
}

/** Count items that apply to each season (including "all" items). */
function computeSeasonalCoverage(items) {
    const coverage = { spring: 0, summer: 0, fall: 0, winter: 0 };
    for (const item of items) {
        const season = (item.season || "all").toLowerCase();
        if (season === "all") {
            for (const key of Object.keys(coverage)) coverage[key]++;
        } else if (season in coverage) {
            coverage[season]++;
        }
    }
    return coverage;
}

/** Returns seasons where the user has fewer than SEASON_GAP_THRESHOLD items. */
const SEASON_GAP_THRESHOLD = 3;
function computeSeasonalGaps(seasonalCoverage) {
    return Object.entries(seasonalCoverage)
        .filter(([, count]) => count < SEASON_GAP_THRESHOLD)
        .map(([season]) => season);
}

/** Returns a list of { category, reason } objects for wardrobe gaps. */
function computeShoppingGaps(categoryDist, seasonalCoverage, items) {
    const gaps = [];
    if (categoryDist.SHOES < 2) gaps.push({ category: "SHOES", reason: "fewer_than_2_pairs" });
    if (categoryDist.ACCESSORY === 0) gaps.push({ category: "ACCESSORY", reason: "no_accessories" });

    const hasWinterItems = seasonalCoverage.winter >= 1;
    const hasWaterproofOuterwear = items.some(
        i => (i.category || "").toUpperCase() === "OUTERWEAR" && i.waterproof
    );
    if (categoryDist.OUTERWEAR === 0 && hasWinterItems) {
        gaps.push({ category: "OUTERWEAR", reason: "no_outerwear_for_winter" });
    } else if (hasWinterItems && !hasWaterproofOuterwear) {
        gaps.push({ category: "OUTERWEAR", reason: "no_waterproof_outerwear" });
    }

    for (const [season, count] of Object.entries(seasonalCoverage)) {
        if (count < SEASON_GAP_THRESHOLD) {
            gaps.push({ category: "SEASONAL", reason: `missing_${season}_wardrobe` });
        }
    }
    return gaps;
}

/**
 * A simple completeness score from 0–1 based on category balance and seasonal
 * coverage — no external data needed.
 */
function computeCompletenessScore(categoryDist, seasonalCoverage) {
    const IDEAL = { TOP: 5, BOTTOM: 4, OUTERWEAR: 2, DRESS: 1, SHOES: 2, ACCESSORY: 2 };
    let score = 0;
    let maxScore = 0;
    for (const [cat, ideal] of Object.entries(IDEAL)) {
        maxScore += 1;
        score += Math.min((categoryDist[cat] || 0) / ideal, 1);
    }
    // Seasonal coverage: each season with >= threshold items counts 0.25 extra
    for (const count of Object.values(seasonalCoverage)) {
        maxScore += 0.25;
        if (count >= SEASON_GAP_THRESHOLD) score += 0.25;
    }
    return Math.round((score / maxScore) * 100) / 100;
}

/** Derive a flat list of marketing segment strings. */
function computeMarketingSegments(primaryPersona, colorTone, seasonalGaps, categoryDist, avgOutfitRating) {
    const segments = [];
    if (primaryPersona) segments.push(`persona_${primaryPersona}`);
    if (colorTone) segments.push(`color_${colorTone}`);
    for (const season of seasonalGaps) segments.push(`missing_${season}_wardrobe`);
    if (categoryDist.SHOES < 2) segments.push("needs_footwear");
    if (categoryDist.OUTERWEAR === 0) segments.push("needs_outerwear");
    if (categoryDist.ACCESSORY === 0) segments.push("needs_accessories");
    if (avgOutfitRating >= 4) segments.push("high_engagement");
    else if (avgOutfitRating > 0 && avgOutfitRating < 2.5) segments.push("low_satisfaction");
    return segments;
}

// ---------------------------------------------------------------------------
// Core computation — called by both triggers
// ---------------------------------------------------------------------------
async function computeAndSaveProfile(userId) {
    const styleRef = db.collection("users").doc(userId).collection("styleProfile").doc("data");

    // --- Debounce: skip if profile was generated less than 5 minutes ago ---
    const existing = await styleRef.get();
    if (existing.exists) {
        const generatedAt = existing.data().generatedAt;
        if (generatedAt) {
            const ageMs = Date.now() - generatedAt.toMillis();
            if (ageMs < 5 * 60 * 1000) {
                console.log(`[generateStyleProfile] Skipping for ${userId} — refreshed ${ageMs}ms ago`);
                return null;
            }
        }
    }

    // --- Consent gate: only write if user has opted in ---
    const userDoc = await db.collection("users").doc(userId).get();
    if (userDoc.exists) {
        const optIn = userDoc.data().marketingOptIn;
        if (optIn === false) {
            console.log(`[generateStyleProfile] Skipping for ${userId} — marketingOptIn is false`);
            return null;
        }
    }

    // --- Read wardrobe data ---
    const [itemsSnap, outfitsSnap] = await Promise.all([
        db.collection("users").doc(userId).collection("items").get(),
        db.collection("users").doc(userId).collection("outfits").get(),
    ]);

    const items = itemsSnap.docs.map(d => d.data());
    const outfits = outfitsSnap.docs.map(d => d.data());

    if (items.length === 0) {
        console.log(`[generateStyleProfile] No items yet for ${userId}, skipping`);
        return null;
    }

    // --- Compute all signals ---
    const categoryDist = computeCategoryDistribution(items);
    const dominantCategory = Object.entries(categoryDist).sort((a, b) => b[1] - a[1])[0]?.[0] || "";

    const personaScores = computePersonaScores(items, outfits);
    const sortedPersonas = Object.entries(personaScores).sort((a, b) => b[1] - a[1]);
    const primaryPersona = sortedPersonas[0]?.[0] || "";
    const secondaryPersona = sortedPersonas[1]?.[1] > 0.05 ? sortedPersonas[1][0] : null;

    const occasionPreferences = computeOccasionPreferences(outfits);
    const primaryOccasion = Object.entries(occasionPreferences).sort((a, b) => b[1] - a[1])[0]?.[0] || "";

    const seasonalCoverage = computeSeasonalCoverage(items);
    const seasonalGaps = computeSeasonalGaps(seasonalCoverage);

    const shoppingGaps = computeShoppingGaps(categoryDist, seasonalCoverage, items);
    const completenessScore = computeCompletenessScore(categoryDist, seasonalCoverage);

    const dominantColors = computeDominantColors(items);
    const colorTone = computeColorTone(items);
    const colorDiversity = computeColorDiversity(items);

    const ratedOutfits = outfits.filter(o => o.rating > 0);
    const avgOutfitRating = ratedOutfits.length > 0
        ? ratedOutfits.reduce((sum, o) => sum + o.rating, 0) / ratedOutfits.length
        : 0;
    const savedOutfits = outfits.filter(o => o.saved).length;
    const savedOutfitRate = outfits.length > 0 ? savedOutfits / outfits.length : 0;

    const marketingSegments = computeMarketingSegments(
        primaryPersona, colorTone, seasonalGaps, categoryDist, avgOutfitRating
    );

    // --- Write profile ---
    const profile = {
        generatedAt: admin.firestore.FieldValue.serverTimestamp(),
        schemaVersion: 1,
        itemCount: items.length,
        outfitCount: outfits.length,

        dominantColors,
        colorTone,
        colorDiversity: Math.round(colorDiversity * 1000) / 1000,

        categoryDistribution: categoryDist,
        dominantCategory,

        personaScores,
        primaryPersona,
        secondaryPersona,

        occasionPreferences,
        primaryOccasion,

        seasonalCoverage,
        seasonalGaps,

        completenessScore,
        shoppingGaps,

        avgOutfitRating: Math.round(avgOutfitRating * 100) / 100,
        savedOutfitRate: Math.round(savedOutfitRate * 1000) / 1000,

        marketingSegments,
    };

    await styleRef.set(profile, { merge: true });
    console.log(`[generateStyleProfile] Profile written for ${userId} — persona: ${primaryPersona}, segments: ${marketingSegments.join(", ")}`);
    return profile;
}

/**
 * Callable Cloud Function: refineClothingTags
 *
 * Optional refinement step that can send the clothing image to a
 * Cloud/Vertex AI model for improved classification.
 *
 * For MVP, this is a placeholder that demonstrates the architecture.
 * Replace the classification logic with actual Vertex AI / Cloud Vision calls.
 *
 * @param {string} data.itemId - The Firestore document ID of the clothing item
 * @param {string} data.userId - The user's UID
 */
exports.refineClothingTags = functions.https.onCall(async (data, context) => {
    // Verify authentication
    if (!context.auth) {
        throw new functions.https.HttpsError(
            "unauthenticated",
            "Must be authenticated to refine tags."
        );
    }

    const { itemId, userId } = data;

    if (!itemId || !userId) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "itemId and userId are required."
        );
    }

    // Verify the user owns this item
    if (context.auth.uid !== userId) {
        throw new functions.https.HttpsError(
            "permission-denied",
            "Cannot access another user's items."
        );
    }

    try {
        // Get the item from Firestore
        const itemRef = db
            .collection("users")
            .doc(userId)
            .collection("items")
            .doc(itemId);

        const itemDoc = await itemRef.get();

        if (!itemDoc.exists) {
            throw new functions.https.HttpsError("not-found", "Item not found.");
        }

        const item = itemDoc.data();

        // ============================================================
        // PLACEHOLDER: Replace this section with actual Vertex AI call
        // ============================================================
        //
        // Example with Vertex AI:
        //
        // const { PredictionServiceClient } = require('@google-cloud/aiplatform');
        // const client = new PredictionServiceClient();
        // const endpoint = `projects/${projectId}/locations/us-central1/endpoints/${endpointId}`;
        // const imageUrl = item.imageUrl;
        // const [response] = await client.predict({
        //   endpoint,
        //   instances: [{ content: imageUrl }],
        // });
        // const refinedLabels = response.predictions.map(p => p.label);
        //
        // ============================================================

        // For MVP, simulate refinement by adding a "refined" flag
        const refinedData = {
            refined: true,
            refinedAt: admin.firestore.FieldValue.serverTimestamp(),
            // In production, update labels and category from AI response:
            // labels: refinedLabels,
            // category: refinedCategory,
        };

        await itemRef.update(refinedData);

        return {
            success: true,
            message: "Tags refined successfully.",
            itemId: itemId,
        };
    } catch (error) {
        if (error instanceof functions.https.HttpsError) {
            throw error;
        }
        console.error("Error refining tags:", error);
        throw new functions.https.HttpsError(
            "internal",
            "Failed to refine tags."
        );
    }
});

// ---------------------------------------------------------------------------
// Firestore triggers: regenerate StyleProfile when items or outfits change
// ---------------------------------------------------------------------------

/**
 * Triggered whenever a clothing item is created, updated, or deleted.
 * Recomputes the user's StyleProfile document (with a 5-minute debounce).
 */
exports.generateStyleProfileOnItem = functions.firestore
    .document("users/{userId}/items/{itemId}")
    .onWrite(async (change, context) => {
        try {
            await computeAndSaveProfile(context.params.userId);
        } catch (err) {
            console.error("[generateStyleProfileOnItem] Error:", err);
        }
    });

/**
 * Triggered whenever an outfit is created, updated, or deleted.
 * Recomputes the user's StyleProfile document (with a 5-minute debounce).
 */
exports.generateStyleProfileOnOutfit = functions.firestore
    .document("users/{userId}/outfits/{outfitId}")
    .onWrite(async (change, context) => {
        try {
            await computeAndSaveProfile(context.params.userId);
        } catch (err) {
            console.error("[generateStyleProfileOnOutfit] Error:", err);
        }
    });
