package com.mlp.sdk.storage

import com.mlp.sdk.Environment
import io.minio.MinioClient

class StorageFactory(
    val environment: Environment
) {

    fun getStorage(bucketName: String = getPlatformBucket()): Storage {
        return when (val storageType = environment["MLP_STORAGE_TYPE"]) {
            S3Storage.STORAGE_NAME -> getS3Service(bucketName)
            LocalStorage.STORAGE_NAME -> getLocalStorage()
            else -> throw RuntimeException("Could not create storage for type: $storageType")
        }
    }

    fun getDefaultStorageDir(): String? = environment["MLP_STORAGE_DIR"]

    private fun getS3Service(bucketName: String) = S3Storage(
        minioClient = createMinioClient(),
        bucketName = bucketName
    )

    private fun getLocalStorage() = LocalStorage()


    private fun createMinioClient(): MinioClient {
        val region = environment["MLP_S3_REGION"]
        return MinioClient.builder()
            .endpoint(environment["MLP_S3_ENDPOINT"])
            .credentials(environment["MLP_S3_ACCESS_KEY"], environment["MLP_S3_SECRET_KEY"])
            .region(if (region.isNullOrEmpty()) "ru" else region)
            .build()
    }

    private fun getPlatformBucket(): String = environment["MLP_S3_BUCKET"] ?: error("") // TODO

}
