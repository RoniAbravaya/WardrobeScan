package com.wardrobescan.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wardrobescan.app.data.local.dao.ClothingItemDao
import com.wardrobescan.app.data.local.dao.OutfitDao
import com.wardrobescan.app.data.local.entity.ClothingItemEntity
import com.wardrobescan.app.data.local.entity.OutfitEntity

@Database(
    entities = [ClothingItemEntity::class, OutfitEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clothingItemDao(): ClothingItemDao
    abstract fun outfitDao(): OutfitDao
}
