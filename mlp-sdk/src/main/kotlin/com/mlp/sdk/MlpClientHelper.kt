package com.mlp.sdk

import com.mlp.api.client.model.CreateOrUpdateDatasetInfoData
import com.mlp.api.client.model.FitRequestData
import com.mlp.api.client.model.JobStatusData
import com.mlp.api.client.model.ModelInfoPK
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import org.slf4j.Logger
import java.io.File
import java.lang.Thread.*
import java.nio.file.Files
import java.nio.file.StandardOpenOption

interface MlpClientHelper: WithExecutionContext {

    @Deprecated("Use logger instead", ReplaceWith("logger"))
    val log: Logger
        get() = logger

    val grpcClient: MlpClientSDK
    val restClient: MlpRestClient

    override val context: MlpExecutionContext
        get() = systemContext

    private fun createTempFileContent(content: String): File =
        File.createTempFile("tempFile", "").apply {
            deleteOnExit()
            Files.write(toPath(), content.toByteArray(), StandardOpenOption.WRITE)
        }

    fun ensureDataset(
        myAccountId: String,
        name: String,
        content: String,
        type: String
    ): Long {
        logger.debug("ensureDataset $name")

        val accountId = myAccountId.toString()
        val contentFile = createTempFileContent(content)

        val existingDataset = restClient.datasetApi
            .listDatasets(accountId, null)
            .find { it.name == name }

        if (existingDataset != null) {
            val oldDatasetId = existingDataset.id!!.datasetId

            val updateRequest = CreateOrUpdateDatasetInfoData()
                .id(existingDataset.id)
                .name(name)
                .dataType(type)
                .accessMode(CreateOrUpdateDatasetInfoData.AccessModeEnum.PRIVATE)
            restClient.datasetApi.updateDataset(accountId, oldDatasetId, updateRequest, null)

            restClient.datasetApi.uploadDatasetContent(accountId, oldDatasetId, contentFile, null)

            return oldDatasetId
        } else {
            val createdDataset = restClient.datasetApi.createDataset(
                accountId,
                name,
                null,
                type,
                CreateOrUpdateDatasetInfoData.AccessModeEnum.PRIVATE.value,
                null,
                contentFile
            )
            return createdDataset.id!!.datasetId
        }
    }

    fun ensureDerivedModel(myAccountId: String, modelName: String, baseModelAccountId: String, baseModelId: String): ModelInfoPK {
        val existingModel = kotlin.runCatching {
            logger.info("Looking for existing model $myAccountId/$modelName")
            restClient.modelApi.getModelInfo(myAccountId, modelName, null)
        }.getOrNull()

        return if (existingModel == null) {
            logger.debug("Creating derived model model $myAccountId/$modelName")
            val createdDerivedModel = restClient.modelApi.createDerivedModel(
                baseModelAccountId,
                baseModelId,
                modelName,
                false,
                null
            )
            logger.info("Model was created with an id: ${createdDerivedModel.id.accountId}/${createdDerivedModel.id.modelId}")
            createdDerivedModel.id
        } else {
            existingModel.id
        }
    }

    fun waitForJobDone(initialJobStatus: JobStatusData) {
        var jobStatus = initialJobStatus
        val start = System.currentTimeMillis()
        val TIMEOUT = 10 * 60_000 // 10m
        while (!jobStatus.done && (System.currentTimeMillis() - start) < TIMEOUT) {
            jobStatus = restClient.jobApi.jobStatus(jobStatus.accountId.toString(), jobStatus.jobId, null)
            sleep(100)
        }
        logger.info("fit is done: $jobStatus")

        if (!jobStatus.done) {
            throw MlpException("Timeout waiting to fit underlying model")
        }
        if (jobStatus.error == true) {
            throw MlpException("Error when calling fit on underlying model: ${jobStatus.errorMessage}")
        }
    }


    fun fit(model: ModelInfoPK, datasetId: Long) {
        val jobStatus = restClient.processApi.fit(
            model.accountId.toString(),
            model.modelId.toString(),
            FitRequestData().datasetId(datasetId),
            null
        )

        waitForJobDone(jobStatus)
    }

}
