package dummy_action

import com.mlp.gate.ActionDescriptorProto
import com.mlp.sdk.MlpService
import com.mlp.sdk.MlpServiceSDK
import com.mlp.sdk.Payload
import org.slf4j.LoggerFactory

object MyService : MlpService {
    override fun getDescriptor(): ActionDescriptorProto {
        return ActionDescriptorProto.newBuilder().build()
    }

    override fun predict(req: Payload): Payload {
        return Payload("text/plain", "\"response from action\"")
    }
}

fun main() {
    val log = LoggerFactory.getLogger("dummy_action")

    log.info("Starting dummy-action module")

    val mlp = MlpServiceSDK(MyService)
    mlp.start()
    mlp.blockUntilShutdown()
}
