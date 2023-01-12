package com.mlp.sdk.storage

import io.minio.GetObjectArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs

class S3Storage(
    val minioClient: MinioClient,
    val bucketName: String
): Storage {
    override fun saveState(content: String, path: String) {
        val inputStream = content.byteInputStream()
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(path)
                .stream(inputStream, inputStream.available().toLong(), -1)
                .build()
        )
    }

    override fun loadState(path: String): String? {
        val modelResponseBytes = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .`object`(path)
                .build()
        ).readAllBytes()
        if (modelResponseBytes.isEmpty()) {
            return null
        }
        return String(modelResponseBytes)
    }

    companion object {
        const val STORAGE_NAME = "s3"
    }
}