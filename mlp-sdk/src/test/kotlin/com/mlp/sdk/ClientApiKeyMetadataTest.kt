package com.mlp.sdk

import com.mlp.gate.AsrRequestProto
import com.mlp.gate.ClientAsrRequestProto
import com.mlp.gate.ClientAsrResponseProto
import com.mlp.gate.ClientRequestProto
import com.mlp.gate.ClientResponseProto
import com.mlp.gate.GateGrpc
import com.mlp.gate.PartialPredictResponseProto
import com.mlp.gate.PayloadProto
import com.mlp.gate.PredictResponseProto
import io.grpc.Metadata
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.stub.StreamObserver
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClientApiKeyMetadataTest {

    private val calls = CopyOnWriteArrayList<Pair<String, String?>>()
    private val bodyTokens = CopyOnWriteArrayList<String>()

    private val gate = object : GateGrpc.GateImplBase() {
        override fun process(request: ClientRequestProto, responseObserver: StreamObserver<ClientResponseProto>) {
            bodyTokens += request.authToken
            responseObserver.onNext(predictResponse())
            responseObserver.onCompleted()
        }

        override fun processResponseStream(request: ClientRequestProto, responseObserver: StreamObserver<ClientResponseProto>) {
            bodyTokens += request.authToken
            responseObserver.onNext(predictResponse())
            responseObserver.onCompleted()
        }

        override fun processStream(responseObserver: StreamObserver<ClientResponseProto>) =
            object : StreamObserver<ClientRequestProto> {
                override fun onNext(value: ClientRequestProto) {
                    bodyTokens += value.authToken
                    responseObserver.onNext(
                        ClientResponseProto.newBuilder()
                            .setPartialPredict(PartialPredictResponseProto.newBuilder().setData(PayloadProto.newBuilder().setJson("{}")))
                            .build()
                    )
                }

                override fun onError(t: Throwable) = Unit
                override fun onCompleted() = responseObserver.onCompleted()
            }

        override fun processRecognition(responseObserver: StreamObserver<ClientAsrResponseProto>) =
            object : StreamObserver<ClientAsrRequestProto> {
                override fun onNext(value: ClientAsrRequestProto) {
                    bodyTokens += value.authToken
                }

                override fun onError(t: Throwable) = Unit
                override fun onCompleted() = responseObserver.onCompleted()
            }
    }

    private val headerRecorder = object : ServerInterceptor {
        override fun <ReqT, RespT> interceptCall(
            call: ServerCall<ReqT, RespT>,
            headers: Metadata,
            next: ServerCallHandler<ReqT, RespT>,
        ): ServerCall.Listener<ReqT> {
            calls += call.methodDescriptor.bareMethodName!! to headers.get(MlpClientSDK.API_KEY_HEADER)
            return next.startCall(call, headers)
        }
    }

    private val server: Server = ServerBuilder.forPort(0)
        .addService(ServerInterceptors.intercept(gate, headerRecorder))
        .build()
        .start()

    private val sdk = MlpClientSDK(
        MlpClientConfig(
            initialGateUrls = listOf("localhost:${server.port}"),
            restUrl = null,
            clientToken = "default-token",
            grpcSecure = false,
        )
    )

    @AfterEach
    fun tearDown() {
        sdk.shutdown()
        server.shutdownNow()
    }

    @Test
    fun `every client call sends its token in metadata and in the body`() {
        runBlocking {
            sdk.predict("account", "model", "{}")
            sdk.predictStream("account", "model", Payload("", "{}"), authToken = "explicit-token").toList()
            sdk.streamPredict("account", "model", flowOf(PayloadWithConfig(Payload("", "{}"), null))).toList()
            sdk.asrPredictProto("account", "model", flowOf(AsrRequestProto.getDefaultInstance()), authToken = "asr-token").toList()
        }

        assertEquals(
            listOf(
                "process" to "default-token",
                "processResponseStream" to "explicit-token",
                "processStream" to "default-token",
                "processRecognition" to "asr-token",
            ),
            calls.toList(),
        )
        assertEquals(listOf("default-token", "explicit-token", "default-token", "asr-token"), bodyTokens.toList())
    }

    private fun predictResponse(): ClientResponseProto = ClientResponseProto.newBuilder()
        .setPredict(PredictResponseProto.newBuilder().setData(PayloadProto.newBuilder().setJson("{}")))
        .build()
}
