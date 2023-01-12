package dummy_action

import com.mlp.gate.ActionDescriptorProto
import com.mlp.sdk.MplAction
import com.mlp.sdk.MplActionSDK
import com.mlp.sdk.Payload
import org.slf4j.LoggerFactory

object MyService : MplAction {
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

    val mlp = MplActionSDK(MyService)
    mlp.start()
    mlp.blockUntilShutdown()
}
