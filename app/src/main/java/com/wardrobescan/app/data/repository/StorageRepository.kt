package com.wardrobescan.app.data.repository

import android.net.Uri
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage,
    private val crashlytics: FirebaseCrashlytics
) {
    suspend fun uploadOriginalImage(userId: String, imageUri: Uri): Result<String> {
        return uploadImage(userId, "originals", imageUri)
    }

    suspend fun uploadCutoutImage(userId: String, imageBytes: ByteArray): Result<String> {
        return try {
            val fileName = "${UUID.randomUUID()}.png"
            val ref = storage.reference
                .child("users/$userId/cutouts/$fileName")
            ref.putBytes(imageBytes).await()
            val downloadUrl = ref.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            crashlytics.recordException(e)
            Result.failure(e)
        }
    }

    private suspend fun uploadImage(userId: String, folder: String, imageUri: Uri): Result<String> {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val ref = storage.reference
                .child("users/$userId/$folder/$fileName")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            crashlytics.recordException(e)
            Result.failure(e)
        }
    }

    suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return try {
            storage.getReferenceFromUrl(imageUrl).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            crashlytics.recordException(e)
            Result.failure(e)
        }
    }
}
