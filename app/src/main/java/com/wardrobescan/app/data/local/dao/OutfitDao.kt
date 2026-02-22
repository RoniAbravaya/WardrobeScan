package com.wardrobescan.app.data.local.dao

import androidx.room.*
import com.wardrobescan.app.data.local.entity.OutfitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutfitDao {

    @Query("SELECT * FROM outfits WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeAll(userId: String): Flow<List<OutfitEntity>>

    @Query(
        "SELECT * FROM outfits WHERE userId = :userId AND saved = 1 " +
            "ORDER BY createdAtMillis DESC"
    )
    fun observeSaved(userId: String): Flow<List<OutfitEntity>>

    @Upsert
    suspend fun upsertAll(outfits: List<OutfitEntity>)

    @Upsert
    suspend fun upsert(outfit: OutfitEntity)

    @Query("DELETE FROM outfits WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM outfits WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
