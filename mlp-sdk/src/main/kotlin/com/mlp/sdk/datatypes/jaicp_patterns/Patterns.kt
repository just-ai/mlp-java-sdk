package com.mlp.sdk.datatypes.jaicp_patterns

import com.mlp.api.client.model.CreateOrUpdateDatasetInfoData
import com.mlp.api.client.model.FitRequestData
import com.mlp.api.client.model.JobStatusData
import com.mlp.api.client.model.ModelInfoPK
import com.mlp.sdk.MlpClientHelper
import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.MlpException
import com.mlp.sdk.MlpRestClient
import com.mlp.sdk.utils.JSON
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption

data class PatternData(
    val id: String,
    val pattern: String
)

data class PatternsFitData(
    val namedPatterns: List<PatternData>? = null,
    val patterns: List<PatternData>
)

data class PatternsRequestData(
    val text: String,
    val activate: List<String>? = null,
    val returnAllMatches: Boolean = false
)

data class PatternsResponseData(
    val patternId: String,
    val score: Double
)

/**
 * Для использования класса необходимо задать env-переменные:
 * - MLP_CLIENT_TOKEN
 * - MLP_REST_URL
 * - MLP_GRPC_HOST
 * - MLP_GRPC_SECURE
 */
class MlpPatterns(val account: String, val model: String,
                  val baseAccount: String = "just-ai", val baseModel: String = "mlp-jaicp-patterns"
): MlpClientHelper {
    override val log = LoggerFactory.getLogger(this.javaClass)

    override val grpcClient = MlpClientSDK()
    override val restClient = MlpRestClient()

    fun prepare(patterns: PatternsFitData) {
        val modelId = ensureDerivedModel(account, model, baseAccount, baseModel)
        val dataset = ensureDataset(account, model, JSON.stringify(patterns), "json/any")

        fit(modelId, dataset)
    }

    fun match(text: String): PatternsResponseData {
        val res = grpcClient.predictBlocking(account, model, JSON.stringify(PatternsRequestData(text=text)))
        return JSON.parse(res, PatternsResponseData::class.java)
    }

}
