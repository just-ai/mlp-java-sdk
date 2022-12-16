package dummy_client

import com.mpl.sdk.MplClientConfig
import com.mpl.sdk.MplClientSDK
import com.mpl.sdk.Payload

fun main() {
    val mpl = MplClientSDK(
        MplClientConfig(
            initialGateUrls = listOf("localhost:10601"),
            connectionToken = ""
        )
    )

    // single call
    val res = mpl.predict(
            "embedded-for-test", "dummy-action-python", "not-empty",
            Payload("mpl_gate.proto.gate_pb2.SimpleTextProto", "{\"text\":\"dummy_client\"}"))

    println("Response: ${res.data}")

    // speed measurement
    (1 .. 10).forEach {
        val s = System.currentTimeMillis()
        (1..10000).forEach {
            mpl.predict(
                    "embedded-for-test", "dummy-action-python", "not-empty", "{\"text\":\"dummy_client\"}")
        }
        val e = System.currentTimeMillis() - s
        println(e)
    }

    mpl.shutdown()
}
