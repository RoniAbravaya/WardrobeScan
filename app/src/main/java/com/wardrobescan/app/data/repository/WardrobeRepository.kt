package com.wardrobescan.app.data.repository

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.wardrobescan.app.data.model.ClothingItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WardrobeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val crashlytics: FirebaseCrashlytics
) {
    private fun itemsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("items")

    fun observeItems(userId: String): Flow<List<ClothingItem>> = callbackFlow {
        val registration = itemsCollection(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(ClothingItem::class.java) ?: emptyList()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    fun observeItemsByCategory(userId: String, category: String): Flow<List<ClothingItem>> = callbackFlow {
        val registration = itemsCollection(userId)
            .whereEqualTo("category", category)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(ClothingItem::class.java) ?: emptyList()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    suspend fun addItem(userId: String, item: ClothingItem): Result<String> {
        return try {
            val docRef = itemsCollection(userId).add(item.copy(userId = userId)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            crashlytics.recordException(e)
            Result.failure(e)
        }
    }

    suspend fun updateItem(userId: String, item: ClothingItem): Result<Unit> {
        return try {
            itemsCollection(userId).document(item.id).set(item).await()
            Result.success(Unit)
        } catch (e: Exception) {
            crashlytics.recordException(e)
            Result.failure(e)
        }
    }

    suspend fun deleteItem(userId: String, itemId: String): Result<Unit> {
        return try {
            itemsCollection(userId).document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            crashlytics.recordException(e)
            Result.failure(e)
        }
    }

    suspend fun getItem(userId: String, itemId: String): Result<ClothingItem> {
        return try {
            val doc = itemsCollection(userId).document(itemId).get().await()
            val item = doc.toObject(ClothingItem::class.java)
            if (item != null) Result.success(item) else Result.failure(Exception("Item not found"))
        } catch (e: Exception) {
            crashlytics.recordException(e)
            Result.failure(e)
        }
    }
}
