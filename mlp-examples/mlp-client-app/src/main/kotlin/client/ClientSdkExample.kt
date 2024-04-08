package client

import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.Payload
import com.mlp.sdk.RawPayload
import com.mlp.sdk.utils.JSON
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val clientSDK = MlpClientSDK(context = systemContext)

    val payload = Payload(null, JSON.stringify("hello"))
    val res = clientSDK.predict("just-ai", "test-action", payload)

    println(res)

    clientSDK.shutdown()
}
