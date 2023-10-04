package com.mlp.sdk.storage

import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.WithExecutionContext
import io.minio.MinioClient

class StorageFactory(
    override val context: MlpExecutionContext = systemContext
) : WithExecutionContext {


    fun getStorage(bucketName: String = getPlatformBucket()): Storage {
        return when (val storageType = environment["MLP_STORAGE_TYPE"]) {
            S3Storage.STORAGE_NAME -> getS3Service(bucketName)
            LocalStorage.STORAGE_NAME -> getLocalStorage()
            else -> error("Could not create storage for type: $storageType")
        }
    }

    fun getDefaultStorageDir(): String? = environment["MLP_STORAGE_DIR"]

    private fun getS3Service(bucketName: String) = S3Storage(
        minioClient = createMinioClient(),
        bucketName = bucketName
    )

    private fun getLocalStorage() = LocalStorage(context)

    private fun createMinioClient(): MinioClient {
        val endpoint = environment.getOrThrow("MLP_S3_ENDPOINT")
        val accessKey = environment.getOrThrow("MLP_S3_ACCESS_KEY")
        val secretKey = environment.getOrThrow("MLP_S3_SECRET_KEY")
        val region = environment["MLP_S3_REGION"] ?: "ru"

        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .region(region)
            .build()
    }

    private fun getPlatformBucket(): String = environment.getOrThrow("MLP_S3_BUCKET")
}

