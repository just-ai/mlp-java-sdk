package com.mlp.sdk.storage

import com.mlp.sdk.Environment
import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import io.minio.MinioClient

object StorageFactory {

    fun getStorage(bucketName: String = getPlatformBucket(systemContext.environment)): Storage {
        return getStorage(systemContext, bucketName)
    }

    fun getStorage(
        context: MlpExecutionContext = systemContext,
        bucketName: String = getPlatformBucket(context.environment)
    ): Storage {
        return when (val storageType = context.environment["MLP_STORAGE_TYPE"]) {
            S3Storage.STORAGE_NAME -> getS3Service(bucketName, context.environment)
            LocalStorage.STORAGE_NAME -> getLocalStorage(context)
            else -> error("Could not create storage for type: $storageType")
        }
    }

    fun getDefaultStorageDir(context: MlpExecutionContext = systemContext): String? =
        context.environment["MLP_STORAGE_DIR"]

    private fun getS3Service(bucketName: String, environment: Environment) = S3Storage(
        minioClient = createMinioClient(environment),
        bucketName = bucketName
    )

    private fun getLocalStorage(context: MlpExecutionContext) =
        LocalStorage(context)

    private fun createMinioClient(environment: Environment): MinioClient {
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

    private fun getPlatformBucket(environment: Environment): String =
        environment.getOrThrow("MLP_S3_BUCKET")
}

