package com.mlp.sdk

import com.fasterxml.jackson.databind.ObjectMapper
import com.mlp.api.ApiClient
import com.mlp.sdk.utils.WithLogger
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayInputStream
import java.io.File

class MlpApiClient(
    apiToken: String,
    apiGateUrl: String,
    restTemplate: RestTemplate = getRestTemplate()
) : ApiClient(restTemplate), WithLogger {

    init {
        basePath = apiGateUrl
        addDefaultHeader("MLP-API-KEY", apiToken)
    }

    companion object {

        fun getInstance(apiToken: String?, apiGateUrl: String?): MlpApiClient {
            requireNotNull(apiToken) { "Api token is not set. Set it in environment variables, or manually in config" }
            requireNotNull(apiGateUrl) { "Api url is not set. Set it in environment variables, or manually in config" }
            return MlpApiClient(apiToken, apiGateUrl)
        }

        private fun getRestTemplate(): RestTemplate {
            val restTemplate = RestTemplate()

            restTemplate.messageConverters.add(0, FileHttpMessageConverter())

            val jacksonConverter = restTemplate.messageConverters.find {
                it is MappingJackson2HttpMessageConverter
            } as MappingJackson2HttpMessageConverter

            jacksonConverter.objectMapper = ObjectMapper()

            return restTemplate
        }
    }
}

private class FileHttpMessageConverter :
    AbstractHttpMessageConverter<File>(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL) {

    override fun getDefaultContentType(file: File): MediaType =
        MediaType.APPLICATION_OCTET_STREAM

    override fun readInternal(clazz: Class<out File>, inputMessage: HttpInputMessage): File {
        val fileName = inputMessage.headers.getValue(HttpHeaders.CONTENT_DISPOSITION)
            .first()
            .split("=")[1]

        val destination = File(fileName)
        destination.deleteOnExit()
        FileUtils.copyInputStreamToFile(inputMessage.body, destination)
        return destination
    }

    override fun supports(clazz: Class<*>) = File::class.java == clazz

    override fun writeInternal(file: File, outputMessage: HttpOutputMessage) {
        ByteArrayInputStream(file.readBytes()).use {
            IOUtils.copyLarge(it, outputMessage.body)
        }
    }
}