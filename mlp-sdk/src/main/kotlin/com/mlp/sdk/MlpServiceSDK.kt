package com.mlp.sdk

import com.mlp.gate.DeferredBillingChargeProto
import com.mlp.gate.DeferredBillingParamsProto
import com.mlp.gate.PartialPredictResponseProto
import com.mlp.gate.PayloadProto
import com.mlp.gate.ServiceToGateProto
import com.mlp.sdk.datatypes.billing.DeferredBillingCharge
import com.mlp.sdk.datatypes.billing.DeferredBillingParams
import com.mlp.sdk.utils.BILLING_CURRENCY_TYPE_HEADER
import com.mlp.sdk.utils.CUSTOM_BILLING_DETAILS_HEADER
import com.mlp.sdk.utils.CUSTOM_BILLING_HEADER
import com.mlp.sdk.utils.CUSTOM_SELF_COST_HEADER
import com.mlp.sdk.utils.JSON
import com.mlp.sdk.utils.JSON.asJson
import java.io.File
import java.lang.Runtime.getRuntime
import java.lang.System.currentTimeMillis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking

class MlpServiceSDK(
    action: MlpService,
    initConfig: MlpServiceConfig? = null,
    dispatcher: CoroutineDispatcher? = null
) : WithExecutionContext, WithState() {

    init {
        if (action is MlpServiceBase<*, *, *, *, *>) {
            action.sdk = this
        }
    }

    /**
     * @param actionProvider Function that provides the MlpService, given an InstanceContext.
     * @param config Optional configuration for the MlpService; defaults to null.
     * @param dispatcher Optional CoroutineDispatcher for coroutine context; defaults to null.
     */
    constructor(
        actionProvider: () -> MlpService,
        config: MlpServiceConfig? = null,
        dispatcher: CoroutineDispatcher? = null
    ) : this(actionProvider(), config, dispatcher)

    override val context: MlpExecutionContext = action.context

    val config: MlpServiceConfig = initConfig ?: loadActionConfig(environment = environment)

    @Deprecated("Use accountId instead", ReplaceWith("accountId"))
    val ACCOUNT_ID
        get() = environment.getOrThrow("MLP_ACCOUNT_ID")

    @Deprecated("Use modelId instead", ReplaceWith("modelId"))
    val MODEL_ID
        get() = environment.getOrThrow("MLP_MODEL_ID")

    val accountId = environment["MLP_ACCOUNT_ID"]
    val modelId = environment["MLP_MODEL_ID"]
    val instanceId = environment["MLP_INSTANCE_ID"]

    private val taskExecutor = TaskExecutor(action, config, dispatcher, context)

    fun start() {
        check(state.notStarted) { "SDK already started" }
        state.starting()
        setShutdownHook()

        taskExecutor.connectorsPool =
            ConnectorsPool(config.connectionToken, taskExecutor, config, context)

        state.active()
        startupProbe()
    }

    fun blockUntilShutdown() {
        check(state.active) { "Action is not started" }
        state.awaitShutdown()
    }

    fun shutdownConnectorsPool() = runBlocking {
        taskExecutor.connectorsPool.gracefulShutdown()
    }

    fun getConnectorsPoolState() =
        taskExecutor.connectorsPool.state

    fun startConnectorsPool() {
        check(state.active) { "Action is not started" }
        check(taskExecutor.connectorsPool.state.isShutdownTypeState()) { "Connectors pool already started or starting" }

        taskExecutor.connectorsPool =
            ConnectorsPool(config.connectionToken, taskExecutor, config, context)
    }

    fun gracefulShutdown() {
        if (!state.active && !state.starting) {
            return
        }

        state.shuttingDown()

        runBlocking {
            taskExecutor.connectorsPool
                .gracefulShutdown()
        }

        taskExecutor.cancelAll()
        state.shutdown()
    }

    fun stop() {
        if (!state.active && !state.starting) {
            return
        }

        state.shuttingDown()

        runBlocking {
            taskExecutor.connectorsPool
                .shutdownNow()
        }

        taskExecutor.cancelAll()
        state.shutdown()
    }

    suspend fun send(connectorId: Long, toGateProto: ServiceToGateProto.Builder) {
        taskExecutor.connectorsPool.send(connectorId, toGateProto)
    }

    /**
     * Sends a deferred billing charge request.
     *
     * This method is used for background (deferred) billing of long-running ML operations.
     * Before use, deferred billing must be initialized by setting
     * appropriate headers in the first response (via BillingUnitsThreadLocal or DeferredBilling).
     *
     * @param billingRequestId Unique request ID that was set during initialization
     * @param amountInUnits Amount to charge in billing units (must be > 0)
     * @throws IllegalArgumentException if amountInUnits <= 0
     */
    @Deprecated("Replace with sendDeferredBillingCharges", ReplaceWith("sendDeferredBillingCharges"))
    suspend fun sendDeferredBillingCharge(
        billingRequestId: String,
        amountInUnits: Long
    ) {
        require(amountInUnits > 0) { "Amount must be positive, got: $amountInUnits" }

        val proto = ServiceToGateProto.newBuilder()
            .setDeferredBillingCharge(
                com.mlp.gate.DeferredBillingChargeRequestProto.newBuilder()
                    .setBillingRequestId(billingRequestId)
                    .setAmountInUnits(amountInUnits)
                    .build()
            )

        taskExecutor.connectorsPool.sendToAnyGate(proto)
    }

    suspend fun sendDeferredBillingCharges(
        params: DeferredBillingParams,
        charges: List<DeferredBillingCharge>,
    ) {
        val charges = charges.map { charge ->
            val builder = DeferredBillingChargeProto.newBuilder()
                .setIdempotencyKey(charge.idempotencyKey)
                .setAmount(charge.amount)

            if (charge.currency != null) {
                builder.currency = charge.currency
            }

            if (charge.llmModelName != null) {
                builder.llmModelName = charge.llmModelName
            }

            if (charge.billingDetails?.isNotEmpty() == true) {
                builder.setBillingDetails(charge.billingDetails.asJson)
            }

            builder.build()
        }

        val paramsBuilder = DeferredBillingParamsProto.newBuilder()

        if (params.callerAccountId != null) {
            paramsBuilder.callerAccountId = params.callerAccountId
        }
        if (params.apiKeyName != null) {
            paramsBuilder.apiKeyName = params.apiKeyName
        }
        if (params.billingKeyName != null) {
            paramsBuilder.billingKeyName = params.billingKeyName
        }
        if (params.billingAccountId != null) {
            paramsBuilder.billingAccountId = params.billingAccountId
        }
        if (params.billingUserId != null) {
            paramsBuilder.billingUserId = params.billingUserId
        }

        taskExecutor.connectorsPool.sendToAnyGate(
            ServiceToGateProto.newBuilder()
                .setDeferredBillingCharges(
                    com.mlp.gate.DeferredBillingChargesProto.newBuilder()
                        .addAllCharges(charges)
                        .setParams(paramsBuilder)
                )
        )
    }

    /**
     * Sends a partial (streaming) response for the given request.
     * Use this method to send intermediate results during long-running operations.
     *
     * @param requestId The request ID (from MDC "gateRequestId")
     * @param connectorId The connector ID (from MDC "connectorId")
     * @param payload The payload to send
     * @param isLast Whether this is the last response in the stream
     * @param price Optional billing units for this response
     * @param billingDetails Optional detailed billing breakdown
     * @param billingCurrencyType Optional billing currency type
     */
    suspend fun sendPartialResponse(
        requestId: Long,
        connectorId: Long,
        payload: PayloadInterface,
        isLast: Boolean,
        price: Long? = null,
        billingDetails: Map<String, Long>? = null,
        billingCurrencyType: String? = null,
        selfCost: Long? = null,
    ) {
        val payloadProto = when (payload) {
            is Payload -> PayloadProto.newBuilder()
                .setJson(payload.data)
                .setDataType(payload.dataType).build()

            is RawPayload -> PayloadProto.newBuilder()
                .setJson(payload.data)
                .setDataType(payload.dataType).build()

            is ProtobufPayload -> PayloadProto.newBuilder()
                .setProtobuf(payload.data)
                .setDataType(payload.dataType ?: "application/octet-stream")
                .build()
        }

        val headers = when (payload) {
            is RawPayload -> payload.headers
            else -> emptyMap()
        }

        val builder = ServiceToGateProto.newBuilder()
            .setRequestId(requestId)
            .setPartialPredict(
                PartialPredictResponseProto.newBuilder()
                    .setFinish(isLast)
                    .setData(payloadProto)
            )
            .putAllHeaders(headers)

        if (price != null) builder.putHeaders(CUSTOM_BILLING_HEADER, price.toString())
        if (billingDetails != null) builder.putHeaders(CUSTOM_BILLING_DETAILS_HEADER, JSON.stringify(billingDetails))
        if (billingCurrencyType != null) builder.putHeaders(BILLING_CURRENCY_TYPE_HEADER, billingCurrencyType)
        if (selfCost != null) builder.putHeaders(CUSTOM_SELF_COST_HEADER, selfCost.toString())

        send(connectorId, builder)
    }

    private fun setShutdownHook() {
        getRuntime().addShutdownHook(Thread {
            logger.info("Shutdown hook started")
            gracefulShutdown()
        })
    }

    private fun startupProbe() {
        File(STARTUP_PROBE_FILE_PATH)
            .writeText("${currentTimeMillis() / 1000}")
    }

    override fun toString() = SDK_COMPONENT_NAME

    companion object {
        const val SDK_COMPONENT_NAME = "MlpServiceSDK"
        const val STARTUP_PROBE_FILE_PATH = "/tmp/startup-probe"

        /** Process-level UUID, stable for the lifetime of the JVM. Used for reconnect detection in gateway. */
        val processInstanceUuid: String = java.util.UUID.randomUUID().toString()
    }
}

data class ModelInfo(
    val accountId: Long,
    val modelId: Long,
    val modelName: String,
)

const val SDK_VERSION = 1L
