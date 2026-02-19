package com.wardrobescan.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.wardrobescan.app.data.model.Outfit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutfitRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun outfitsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("outfits")

    fun observeOutfits(userId: String): Flow<List<Outfit>> = callbackFlow {
        val registration = outfitsCollection(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val outfits = snapshot?.toObjects(Outfit::class.java) ?: emptyList()
                trySend(outfits)
            }
        awaitClose { registration.remove() }
    }

    fun observeSavedOutfits(userId: String): Flow<List<Outfit>> = callbackFlow {
        val registration = outfitsCollection(userId)
            .whereEqualTo("saved", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val outfits = snapshot?.toObjects(Outfit::class.java) ?: emptyList()
                trySend(outfits)
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveOutfit(userId: String, outfit: Outfit): Result<String> {
        return try {
            val docRef = outfitsCollection(userId).add(outfit.copy(userId = userId)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOutfit(userId: String, outfit: Outfit): Result<Unit> {
        return try {
            outfitsCollection(userId).document(outfit.id).set(outfit).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOutfit(userId: String, outfitId: String): Result<Unit> {
        return try {
            outfitsCollection(userId).document(outfitId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
