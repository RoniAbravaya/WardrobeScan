package com.wardrobescan.app.domain

import com.wardrobescan.app.data.local.AppDatabase
import com.wardrobescan.app.data.repository.WardrobeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteClothingItemUseCase @Inject constructor(
    private val wardrobeRepository: WardrobeRepository,
    private val db: AppDatabase
) {
    suspend operator fun invoke(userId: String, itemId: String): Result<Unit> {
        val result = wardrobeRepository.deleteItem(userId, itemId)
        if (result.isSuccess) {
            db.clothingItemDao().deleteById(itemId)
        }
        return result
    }
}
