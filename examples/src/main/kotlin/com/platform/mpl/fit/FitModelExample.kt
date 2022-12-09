package com.platform.mpl.fit

import com.fasterxml.jackson.databind.ObjectMapper
import com.platform.mpl.sdk.Payload
import com.platform.mpl.sdk.PlatformAction
import com.platform.mpl.sdk.PlatformActionSDK
import com.platform.mpl.sdk.PlatformResponse
import com.platform.mpl.simple.CompositeModelExample

object FitModelExample {

    val objectMapper = ObjectMapper()

    @JvmStatic
    fun main(args: Array<String>) {
        val action = CompositeModelExample.CompositeTestAction()
        val actionSDK = PlatformActionSDK(action)

        actionSDK.start()

        actionSDK.gracefulShutdown()
    }

    class FitTestAction: PlatformAction() {
        override fun fit(train: Payload, targets: Payload, config: Payload?): PlatformResponse {
            val trainData = objectMapper.readValue(train.data, TrainData::class.java)
            val targetData = objectMapper.readValue(targets.data, TargetData::class.java)
            val configData = objectMapper.readValue(config?.data, ConfigData::class.java)
            // your fit logic here
            return Payload(
                dataType = "text/plain",
                data = "ok"
            )
        }
    }

    data class TrainData(
        val texts: List<String>
    )

    data class TargetData(
        val items: List<Item>
    )

    data class ConfigData(
        val accountId: String,
        val modelName: String,
        val apiToken: String,
        val batchSize: Int,
        val searchIndex: String
    )

    data class Item(
        val value: List<String>
    )
}