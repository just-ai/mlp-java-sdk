package com.mpl.sdk.utils

import io.minio.MinioClient

object S3Factory {

    fun getS3Service() = MinioStateWrapper(
        minioClient = createMinioClient(),
        bucketName = getPlatformBucket()
    )

    fun getDefaultStorageDir(): String = fromSystemProperties("MPL_STORAGE_DIR")

    private fun createMinioClient(): MinioClient {
        return MinioClient.builder()
            .endpoint(System.getenv("MPL_S3_ENDPOINT"))
            .credentials(System.getenv("MPL_S3_ACCESS_KEY"), System.getenv("MPL_S3_SECRET_KEY"))
            .build()
    }

    private fun getPlatformBucket(): String = fromSystemProperties("MPL_S3_BUCKET")

    private fun fromSystemProperties(s: String) = System.getProperty(s)

}