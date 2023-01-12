package com.mlp.sdk.storage

import io.minio.MinioClient

object StorageFactory {

    fun getStorage(): Storage {
        val storageType = System.getenv("MLP_STORAGE_TYPE")
        return when (storageType) {
            S3Storage.STORAGE_NAME -> getS3Service()
            LocalStorage.STORAGE_NAME -> getLocalStorage()
            else -> throw RuntimeException("Could not create storage for type: $storageType")
        }
    }

    fun getDefaultStorageDir(): String? = System.getenv("MLP_STORAGE_DIR")

    private fun getS3Service() = S3Storage(
        minioClient = createMinioClient(),
        bucketName = getPlatformBucket()
    )

    private fun getLocalStorage() = LocalStorage()


    private fun createMinioClient(): MinioClient {
        return MinioClient.builder()
            .endpoint(System.getenv("MLP_S3_ENDPOINT"))
            .credentials(System.getenv("MLP_S3_ACCESS_KEY"), System.getenv("MLP_S3_SECRET_KEY"))
            .build()
    }

    private fun getPlatformBucket(): String = System.getenv("MLP_S3_BUCKET")

}