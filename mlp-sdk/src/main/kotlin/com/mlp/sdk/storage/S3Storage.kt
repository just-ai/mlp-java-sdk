package com.mlp.sdk.storage

import io.minio.GetObjectArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.errors.ErrorResponseException
import org.apache.commons.io.IOUtils
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

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

    override fun saveState(content: ByteArray, filePath: String) {
        with(ByteArrayInputStream(content)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .`object`(filePath)
                            .stream(this, this.available().toLong(), -1)
                            .build()
            )
        }
    }

    override fun saveState(content: File, filePath: String) {
        with(FileInputStream(content)) {
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
        return loadStateBytes(path)?.let { String(it) }
    }

    override fun loadStateBytes(path: String): ByteArray? {
        try {
            val modelResponseBytes = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .`object`(path)
                            .build()
            ).readAllBytes()
            if (modelResponseBytes.isEmpty()) {
                return null
            }
            return modelResponseBytes
        } catch (e: ErrorResponseException) {
            if (e.errorResponse().code() != "NoSuchKey") {
                throw e
            }
            return null
        }
    }

    companion object {
        const val STORAGE_NAME = "s3"
    }
}