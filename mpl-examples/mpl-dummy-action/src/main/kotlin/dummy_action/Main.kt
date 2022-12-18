package dummy_action

import com.mpl.gate.ActionDescriptorProto
import com.mpl.sdk.MplAction
import com.mpl.sdk.MplActionSDK
import com.mpl.sdk.Payload
import org.slf4j.LoggerFactory

object MyService : MplAction() {
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

    val mpl = MplActionSDK(MyService)
    mpl.start()
    mpl.blockUntilShutdown()
}
