package com.mlp.sdk

import com.mlp.api.ApiClient
import com.mlp.sdk.utils.JSON
import java.io.ByteArrayInputStream
import java.io.File
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate

class MlpApiClient(
    defaultApiToken: String?,
    apiGateUrl: String,
    restTemplate: RestTemplate = getRestTemplate(),
    billingToken: String? = null
) : ApiClient(restTemplate) {

    init {
        basePath = apiGateUrl
        defaultApiToken?.let { addDefaultHeader("MLP-API-KEY", it) }
        billingToken?.let { addDefaultHeader("MLP-BILLING-KEY", it) }
    }

    companion object {

        fun getInstance(defaultApiToken: String?, apiGateUrl: String?): MlpApiClient {
            requireNotNull(apiGateUrl) { "Api url is not set. Set it in environment variables, or manually in config" }
            return MlpApiClient(defaultApiToken, apiGateUrl)
        }

        private fun getRestTemplate(): RestTemplate {
            val restTemplate = RestTemplate()

            restTemplate.messageConverters.add(0, FileHttpMessageConverter())

            val jacksonConverter = restTemplate.messageConverters.find {
                it is MappingJackson2HttpMessageConverter
            } as MappingJackson2HttpMessageConverter

            jacksonConverter.objectMapper = JSON.mapper

            return restTemplate
        }
    }
}

private class FileHttpMessageConverter :
    AbstractHttpMessageConverter<File>(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL) {

    override fun getDefaultContentType(file: File): MediaType =
        MediaType.APPLICATION_OCTET_STREAM

    override fun readInternal(clazz: Class<out File>, inputMessage: HttpInputMessage): File {
        val fileName = inputMessage.headers[CONTENT_DISPOSITION]
            ?.firstOrNull()
            ?.split("=")?.getOrNull(1)
            ?: throw IllegalArgumentException("Header $CONTENT_DISPOSITION not found")

        val destination = File(FileUtils.getTempDirectoryPath(), fileName)
        destination.deleteOnExit()
        FileUtils.copyInputStreamToFile(inputMessage.body, destination)
        return destination
    }

    override fun supports(clazz: Class<*>) =
        File::class.java == clazz

    override fun writeInternal(file: File, outputMessage: HttpOutputMessage) {
        ByteArrayInputStream(file.readBytes()).use {
            IOUtils.copyLarge(it, outputMessage.body)
        }
    }
}
