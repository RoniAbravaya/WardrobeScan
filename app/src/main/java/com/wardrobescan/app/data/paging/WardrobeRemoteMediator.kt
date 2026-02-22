package com.wardrobescan.app.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.wardrobescan.app.data.local.AppDatabase
import com.wardrobescan.app.data.local.entity.ClothingItemEntity
import com.wardrobescan.app.data.model.ClothingItem
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalPagingApi::class)
class WardrobeRemoteMediator(
    private val userId: String,
    private val categoryFilter: String?,
    private val firestore: FirebaseFirestore,
    private val db: AppDatabase
) : RemoteMediator<Int, ClothingItemEntity>() {

    private var lastVisible: QueryDocumentSnapshot? = null

    private fun baseQuery(): Query {
        var q = firestore.collection("users")
            .document(userId)
            .collection("items")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
        if (categoryFilter != null) {
            q = q.whereEqualTo("category", categoryFilter)
        }
        return q
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ClothingItemEntity>
    ): MediatorResult {
        return try {
            val query = when (loadType) {
                LoadType.REFRESH -> {
                    lastVisible = null
                    baseQuery()
                }
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val anchor = lastVisible ?: return MediatorResult.Success(endOfPaginationReached = true)
                    baseQuery().startAfter(anchor)
                }
            }

            val snapshot = query.get().await()
            val documents = snapshot.documents

            if (loadType == LoadType.REFRESH) {
                db.clothingItemDao().deleteAllForUser(userId)
            }

            val entities = documents.mapNotNull { doc ->
                doc.toObject(ClothingItem::class.java)?.let { item ->
                    ClothingItemEntity.fromModel(item)
                }
            }
            db.clothingItemDao().upsertAll(entities)

            lastVisible = snapshot.documents.lastOrNull() as? QueryDocumentSnapshot

            MediatorResult.Success(endOfPaginationReached = documents.size < PAGE_SIZE)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    companion object {
        const val PAGE_SIZE = 20L
    }
}
