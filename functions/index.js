const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const storage = admin.storage();

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
