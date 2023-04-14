package com.mlp.sdk

import com.fasterxml.jackson.databind.ObjectMapper
import com.mlp.api.ApiClient
import com.mlp.sdk.utils.WithLogger
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate

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

            val jacksonConverter = restTemplate.messageConverters.find {
                it is MappingJackson2HttpMessageConverter
            } as MappingJackson2HttpMessageConverter

            jacksonConverter.objectMapper = ObjectMapper()

            return restTemplate
        }
    }
}