package com.wardrobescan.app.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.wardrobescan.app.data.local.entity.ClothingItemEntity

@Dao
interface ClothingItemDao {

    @Query("SELECT * FROM clothing_items WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun pagingSource(userId: String): PagingSource<Int, ClothingItemEntity>

    @Query(
        "SELECT * FROM clothing_items WHERE userId = :userId AND category = :category " +
            "ORDER BY createdAtMillis DESC"
    )
    fun pagingSourceByCategory(userId: String, category: String): PagingSource<Int, ClothingItemEntity>

    @Query("SELECT * FROM clothing_items WHERE userId = :userId ORDER BY createdAtMillis DESC")
    suspend fun getAll(userId: String): List<ClothingItemEntity>

    @Query("SELECT * FROM clothing_items WHERE id = :id")
    suspend fun getById(id: String): ClothingItemEntity?

    @Upsert
    suspend fun upsertAll(items: List<ClothingItemEntity>)

    @Upsert
    suspend fun upsert(item: ClothingItemEntity)

    @Query("DELETE FROM clothing_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM clothing_items WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
