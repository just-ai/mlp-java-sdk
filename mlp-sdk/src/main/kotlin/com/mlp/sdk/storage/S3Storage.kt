package com.mlp.sdk.storage

import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.WithExecutionContext
import io.minio.GetObjectArgs
import io.minio.MinioClient
import io.minio.ObjectWriteResponse
import io.minio.PutObjectArgs
import io.minio.errors.ErrorResponseException
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import org.apache.commons.io.IOUtils.toInputStream

class S3Storage(
    val minioClient: MinioClient,
    val bucketName: String,
    override val context: MlpExecutionContext = systemContext
) : Storage, WithExecutionContext {


    override fun saveState(content: String, filePath: String) {
        with(toInputStream(content, Charsets.UTF_8)) {
            saveToS3(this, filePath)
        }
    }

    override fun saveState(content: ByteArray, filePath: String) {
        with(ByteArrayInputStream(content)) {
            saveToS3(this, filePath)
        }
    }

    override fun saveState(content: File, filePath: String) {
        with(FileInputStream(content)) {
            saveToS3(this, filePath)
        }
    }

    override fun loadState(path: String): String? {
        return loadStateBytes(path)?.let { String(it) }
    }

    override fun loadStateBytes(path: String): ByteArray? {
        try {
            val responseData = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(path)
                    .build()
            ).readAllBytes()

            if (responseData.isEmpty()) {
                return null
            }
            return responseData
        } catch (e: ErrorResponseException) {
            if (e.errorResponse().code() != "NoSuchKey") {
                logger.error("Failed to read from bucket $bucketName by path $path", e)
                throw e
            }
            return null
        } catch (e: Exception) {
            logger.error("Failed to read from bucket $bucketName by path $path", e)
            throw e
        }
    }

    private fun saveToS3(inputStream: InputStream, filePath: String): ObjectWriteResponse? =
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(filePath)
                    .stream(inputStream, inputStream.available().toLong(), -1)
                    .build()
            )
        } catch (e: Exception) {
            logger.error("Failed to save to $filePath in bucket $bucketName", e)
            throw e
        }

    companion object {
        const val STORAGE_NAME = "s3"
    }
}

