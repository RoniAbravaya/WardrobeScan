package com.wardrobescan.app.domain

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.google.firebase.firestore.FirebaseFirestore
import com.wardrobescan.app.data.local.AppDatabase
import com.wardrobescan.app.data.model.ClothingItem
import com.wardrobescan.app.data.paging.WardrobeRemoteMediator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetPagedWardrobeItemsUseCase @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val db: AppDatabase
) {
    @OptIn(ExperimentalPagingApi::class)
    operator fun invoke(userId: String, categoryFilter: String? = null): Flow<PagingData<ClothingItem>> {
        val mediator = WardrobeRemoteMediator(
            userId = userId,
            categoryFilter = categoryFilter,
            firestore = firestore,
            db = db
        )
        val pagingSourceFactory = if (categoryFilter != null) {
            { db.clothingItemDao().pagingSourceByCategory(userId, categoryFilter) }
        } else {
            { db.clothingItemDao().pagingSource(userId) }
        }
        return Pager(
            config = PagingConfig(
                pageSize = WardrobeRemoteMediator.PAGE_SIZE.toInt(),
                enablePlaceholders = false
            ),
            remoteMediator = mediator,
            pagingSourceFactory = pagingSourceFactory
        ).flow.map { pagingData -> pagingData.map { it.toModel() } }
    }
}
