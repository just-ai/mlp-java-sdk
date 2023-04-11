package client

import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.utils.JSON

fun main() {
    val clientSDK = MlpClientSDK()
    clientSDK.init()

    val payload = JSON.stringify("hello")
    val res = clientSDK.predict("just-ai", "test-action", payload)

    println(res)

    clientSDK.shutdown()
}