package com.platform.mpl.fit

import com.fasterxml.jackson.databind.ObjectMapper
import com.platform.mpl.sdk.Payload
import com.platform.mpl.sdk.PlatformAction
import com.platform.mpl.sdk.PlatformActionSDK
import com.platform.mpl.sdk.PlatformResponse

fun main() {
    val action = FitTestAction()
    val actionSDK = PlatformActionSDK(action)

    actionSDK.start()

    actionSDK.blockUntilShutdown()
}

class FitTestAction: PlatformAction() {
    override fun fit(train: Payload, targets: Payload, config: Payload?): PlatformResponse {
        val objectMapper = ObjectMapper()

        val trainData = objectMapper.readValue(train.data, TrainData::class.java)
        val targetData = objectMapper.readValue(targets.data, TargetData::class.java)
        val configData = objectMapper.readValue(config?.data, ConfigData::class.java)

        val result = processFitData(trainData, targetData, configData)

        s3Client.save(result)

        return Payload(
            dataType = "text/plain",
            data = "ok"
        )
    }

    private fun processFitData(trainData: TrainData?, targetData: TargetData?, config: ConfigData): FitProcessData {
        return trainData!!.texts.mapIndexed { i, data ->
            data to targetData!!.items[i]
        }.toMap().let {
            FitProcessData(it)
        }
    }
}

object s3Client {
    fun save(any: Any) = "ok"
}

data class FitProcessData(
    val processedData: Map<String, Item?>
)

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