package com.wardrobescan.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.wardrobescan.app.data.model.StyleProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StyleProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Cloud Function writes to: users/{userId}/styleProfile/data
    private fun profileDoc(userId: String) =
        firestore.collection("users").document(userId)
            .collection("styleProfile").document("data")

    // Main user document that holds consent fields
    private fun userDoc(userId: String) =
        firestore.collection("users").document(userId)

    /**
     * Real-time stream of the user's StyleProfile.
     * Emits null if the document does not exist yet (no items scanned or
     * Cloud Function hasn't run yet).
     */
    fun observeProfile(userId: String): Flow<StyleProfile?> = callbackFlow {
        val registration = profileDoc(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val profile = snapshot?.toObject(StyleProfile::class.java)
                trySend(profile)
            }
        awaitClose { registration.remove() }
    }

    /**
     * One-shot fetch of the latest StyleProfile (useful for analytics or
     * in-app features that don't need live updates).
     */
    suspend fun getProfile(userId: String): Result<StyleProfile?> {
        return try {
            val doc = profileDoc(userId).get().await()
            Result.success(doc.toObject(StyleProfile::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Records the user's marketing consent decision.
     *
     * Sets `marketingOptIn` and `marketingConsentAt` on the main user document.
     * The Cloud Function reads these fields before writing the StyleProfile —
     * if `marketingOptIn == false` the profile will not be generated.
     */
    suspend fun updateConsent(userId: String, optIn: Boolean): Result<Unit> {
        return try {
            val update = mapOf(
                "marketingOptIn" to optIn,
                "marketingConsentAt" to Timestamp.now()
            )
            userDoc(userId).set(update, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
