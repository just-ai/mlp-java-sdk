package com.mlp.sdk.storage

import io.minio.GetObjectArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.apache.commons.io.IOUtils

class S3Storage(
    val minioClient: MinioClient,
    val bucketName: String
) : Storage {
    override fun saveState(content: String, filePath: String) {
        with(IOUtils.toInputStream(content, Charsets.UTF_8)) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(filePath)
                    .stream(this, this.available().toLong(), -1)
                    .build()
            )
        }
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