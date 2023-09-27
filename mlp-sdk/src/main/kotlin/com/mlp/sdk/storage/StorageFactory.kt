package com.mlp.sdk.storage

import com.mlp.sdk.Environment
import com.mlp.sdk.WithEnvironment
import io.minio.MinioClient

class StorageFactory(
    override val environment: Environment
) : WithEnvironment {

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

    private fun getLocalStorage() = LocalStorage(environment)

    private fun createMinioClient(): MinioClient {
        val endpoint = environment.getNotNull("MLP_S3_ENDPOINT")
        val accessKey = environment.getNotNull("MLP_S3_ACCESS_KEY")
        val secretKey = environment.getNotNull("MLP_S3_SECRET_KEY")
        val region = environment["MLP_S3_REGION"] ?: "ru"

        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .region(region)
            .build()
    }

    private fun getPlatformBucket(): String = environment.getNotNull("MLP_S3_BUCKET")
}

