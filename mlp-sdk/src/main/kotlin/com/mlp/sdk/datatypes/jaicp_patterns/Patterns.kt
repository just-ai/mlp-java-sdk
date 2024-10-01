package com.mlp.sdk.datatypes.jaicp_patterns

import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.MlpClientHelper
import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.MlpRestClient
import com.mlp.sdk.WithExecutionContext
import com.mlp.sdk.utils.JSON

data class PatternData(
    val id: String,
    val pattern: String
)

data class PatternsFitData(
    val namedPatterns: List<PatternData>? = null,
    val patterns: List<PatternData>
)

data class PatternsFitConfigData(
    val markup: String? = null,
    val ner: List<String>? = null
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
 * - MLP_GRPC_HOSTS
 * - MLP_GRPC_SECURE
 */
class MlpPatterns(
    val account: String,
    val model: String,
    val baseAccount: String = "just-ai",
    val baseModel: String = "mlp-jaicp-patterns",
    override val context: MlpExecutionContext = systemContext
): MlpClientHelper, WithExecutionContext {

    override val grpcClient = MlpClientSDK(context = context)
    override val restClient = MlpRestClient(context = context)

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
