package com.mlp.sdk

import com.mlp.sdk.utils.WithLogger

/**
 * API for working with deferred billing (Background Billing).
 *
 * Allows sending charge requests for long-running ML operations
 * that are processed in the background.
 *
 * Usage example:
 * ```kotlin
 * class MyMLService : MlpPredictServiceBase<Request, Response>(...) {
 *     override suspend fun predict(req: Request): Response {
 *         val deferred = createDeferredBilling()
 *
 *         // Start background task
 *         CoroutineScope(Dispatchers.IO).launch {
 *             processLongTask(deferred.requestId)
 *         }
 *
 *         return Response("Processing started")
 *     }
 *
 *     private suspend fun processLongTask(billingId: String) {
 *         val deferred = DeferredBilling(billingId, sdk)
 *
 *         // ... processing ...
 *
 *         // Charge as needed
 *         deferred.charge(500)
 *
 *         // Charge with detailed breakdown
 *         deferred.charge(
 *             amountInUnits = 1500,
 *             billingDetails = mapOf("tokens" to 1000, "requests" to 500)
 *         )
 *     }
 * }
 * ```
 *
 * @param requestId Unique request ID for deferred billing
 * @param sdk MlpServiceSDK instance for sending messages
 */
class DeferredBilling(
    val requestId: String,
    private val sdk: MlpServiceSDK,
) : WithLogger {

    init {
        BillingUnitsThreadLocal.setDeferredBillingRequestId(requestId)
    }

    /**
     * Sends a charge request for the specified amount.
     *
     * Optionally, you can provide detailed breakdown of billing units.
     * The details will be sent via Z-custom-billing-details header for monitoring purposes,
     * while the total amount will be charged.
     *
     * @param amountInUnits Amount in billing units (must be > 0)
     * @param billingDetails Optional detailed breakdown of billing units (e.g., {"tokens": 1000, "requests": 500})
     * @throws IllegalArgumentException if amountInUnits <= 0
     */
    suspend fun charge(
        amountInUnits: Long,
        billingDetails: Map<String, Long>? = null,
    ) {
        require(amountInUnits >= 0) { "Amount must be positive or zero, got: $amountInUnits" }
        logger.debug("Charging deferred billing: requestId={}, amount={}", requestId, amountInUnits)

        billingDetails?.also { BillingUnitsThreadLocal.setDetailedUnits(it) }

        sdk.sendDeferredBillingCharge(requestId, amountInUnits)
    }
}
