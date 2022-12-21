package com.mpl.sdk.storage

import io.minio.MinioClient
import java.lang.RuntimeException

object StorageFactory {

    fun getStorage(): Storage = when(System.getenv("MPL_STORAGE_TYPE")) {
        S3Storage.STORAGE_NAME -> getS3Service()
        LocalStorage.STORAGE_NAME -> getLocalStorage()
        else -> throw RuntimeException("Could not create storage")
    }

    private fun getS3Service() = S3Storage(
        minioClient = createMinioClient(),
        bucketName = getPlatformBucket()
    )

    private fun getLocalStorage() = LocalStorage()

    fun getDefaultStorageDir(): String? = System.getenv("MPL_STORAGE_DIR")

    private fun createMinioClient(): MinioClient {
        return MinioClient.builder()
            .endpoint(System.getenv("MPL_S3_ENDPOINT"))
            .credentials(System.getenv("MPL_S3_ACCESS_KEY"), System.getenv("MPL_S3_SECRET_KEY"))
            .build()
    }

    private fun getPlatformBucket(): String = System.getenv("MPL_S3_BUCKET")

}