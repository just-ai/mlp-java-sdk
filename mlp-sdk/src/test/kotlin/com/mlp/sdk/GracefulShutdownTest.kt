package com.mlp.sdk

import com.mlp.gate.GateGrpc
import com.mlp.gate.GateToServiceProto
import com.mlp.gate.HeartBeatProto
import com.mlp.gate.PayloadProto
import com.mlp.gate.PredictRequestProto
import com.mlp.gate.ServiceDescriptorProto
import com.mlp.gate.ServiceToGateProto
import com.mlp.gate.StopServingProto
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit.SECONDS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

/**
 * Остановка инстанса не должна терять активные запросы (CAILA-7105).
 *
 * Проверяется наблюдаемое гейтом поведение поверх настоящего gRPC-соединения: порядок кадров
 * в стриме и момент half-close. Заглушка гейта повторяет его контракт — на stopServing отвечает
 * своим stopServing и держит вызов открытым, пока клиент не закроет свою половину.
 */
class GracefulShutdownTest {

    /** Критерии 1 и 5: обычный predict досылает ответ, stopServing уходит раньше half-close. */
    @Test
    fun `in-flight predict is answered before the stream is half-closed`() {
        val service = TestService { _ -> delay(2000); Payload("application/json", """{"ok":true}""") }

        harness(service, actionConnectorMs = 10_000).use { h ->
            val stream = h.gate.stream(0)
            stream.sendPredict(requestId = 1)
            h.awaitTrue("predict is started") { service.started.isNotEmpty() }
            Thread.sleep(300)

            h.sdk.gracefulShutdown()
            h.awaitTrue("gate has received half-close") { stream.indexOfClientCompleted() >= 0 }

            val stopServing = stream.indexOfStopServing()
            val predictResponse = stream.indexOfPredictResponse(requestId = 1)
            val halfClose = stream.indexOfClientCompleted()

            assertTrue(stopServing >= 0, "gate has not received stopServing: ${stream.describe()}")
            assertTrue(predictResponse >= 0, "gate has not received predict response: ${stream.describe()}")
            assertTrue(
                stopServing < predictResponse,
                "stopServing must be sent before the response: ${stream.describe()}"
            )
            assertTrue(
                predictResponse < halfClose,
                "response must be sent before half-close: ${stream.describe()}"
            )
            assertEquals(listOf(1L), service.finished.toList())
        }
    }

    /** Критерии 2 и 5: все кадры стрима, включая финальный, доходят до half-close. */
    @Test
    fun `all stream frames are delivered before the stream is half-closed`() {
        val frames = 5
        val service = TestService { context ->
            repeat(frames) { i ->
                delay(200)
                sdk.sendPartialResponse(
                    requestId = context.gateRequestId,
                    connectorId = context.connectorId,
                    payload = Payload("application/json", """{"chunk":${i + 1}}"""),
                    isLast = i == frames - 1,
                )
            }
            MlpPartialBinaryResponse()
        }

        harness(service, actionConnectorMs = 10_000).use { h ->
            val stream = h.gate.stream(0)
            stream.sendPredict(requestId = 1)
            h.awaitTrue("predict is started") { service.started.isNotEmpty() }
            Thread.sleep(300)

            h.sdk.gracefulShutdown()
            h.awaitTrue("gate has received half-close") { stream.indexOfClientCompleted() >= 0 }

            val partials = stream.messages().filter { it.hasPartialPredict() && it.requestId == 1L }
            assertEquals(frames, partials.size, "not all stream frames reached the gate: ${stream.describe()}")
            assertTrue(partials.last().partialPredict.finish, "last frame must be final: ${stream.describe()}")

            val halfClose = stream.indexOfClientCompleted()
            val lastFrame = stream.events.indexOfLast { it is GateEvent.Message && it.proto.hasPartialPredict() }
            assertTrue(lastFrame < halfClose, "frames must be sent before half-close: ${stream.describe()}")
            assertTrue(
                stream.indexOfStopServing() in 0 until lastFrame,
                "stopServing must be sent before the remaining frames: ${stream.describe()}"
            )
        }
    }

    /** Критерий 3: без активных запросов остановка не ждёт бюджет. */
    @Test
    fun `shutdown without in-flight requests returns immediately`() {
        val service = TestService { _ -> Payload("application/json", "{}") }

        harness(service, actionConnectorMs = 20_000).use { h ->
            val stream = h.gate.stream(0)

            val elapsed = measure { h.sdk.gracefulShutdown() }
            h.awaitTrue("gate has received half-close") { stream.indexOfClientCompleted() >= 0 }

            assertTrue(elapsed < 1000, "shutdown without requests took $elapsed ms: ${stream.describe()}")
            assertTrue(
                stream.indexOfStopServing() in 0 until stream.indexOfClientCompleted(),
                "stopServing must be sent before half-close: ${stream.describe()}"
            )
        }
    }

    /** Критерий 4: запрос дольше бюджета отменяется, стрим закрывается, остановка возвращается. */
    @Test
    fun `request longer than the budget is cancelled when the budget is over`() {
        val service = TestService { _ -> delay(5000); Payload("application/json", "{}") }

        harness(service, actionConnectorMs = 500).use { h ->
            val stream = h.gate.stream(0)
            stream.sendPredict(requestId = 1)
            h.awaitTrue("predict is started") { service.started.isNotEmpty() }

            val elapsed = measure { h.sdk.gracefulShutdown() }
            h.awaitTrue("gate has received half-close") { stream.indexOfClientCompleted() >= 0 }

            assertTrue(elapsed in 400L..2000L, "shutdown with 500 ms budget took $elapsed ms")
            assertTrue(service.finished.isEmpty(), "request must be cancelled, but it has finished")
            assertTrue(
                stream.indexOfPredictResponse(requestId = 1) < 0,
                "cancelled request must not produce a predict response: ${stream.describe()}"
            )
        }
    }

    /** Критерий 6: запрос, пришедший после начала остановки, в обработку не принимается. */
    @Test
    fun `requests arriving after shutdown has started are not accepted`() {
        val service = TestService { _ -> delay(1500); Payload("application/json", "{}") }

        harness(service, actionConnectorMs = 10_000).use { h ->
            val stream = h.gate.stream(0)
            stream.sendPredict(requestId = 1)
            h.awaitTrue("first predict is started") { service.started.isNotEmpty() }

            val shutdown = Thread { h.sdk.gracefulShutdown() }.apply { start() }
            h.awaitTrue("gate has received stopServing") { stream.indexOfStopServing() >= 0 }

            stream.sendPredict(requestId = 2)
            shutdown.join(30_000)
            h.awaitTrue("gate has received half-close") { stream.indexOfClientCompleted() >= 0 }

            assertEquals(listOf(1L), service.started.toList(), "late request must not reach the service")
            assertTrue(
                stream.messages().none { it.requestId == 2L },
                "late request must not be answered: ${stream.describe()}"
            )
            assertTrue(stream.indexOfPredictResponse(requestId = 1) >= 0, "in-flight request lost its response")
        }
    }

    /** Критерий 7: stopServing от гейта — half-close только после завершения активных запросов. */
    @Test
    fun `gate initiated stop serving waits for in-flight requests`() {
        val service = TestService { _ -> delay(1000); Payload("application/json", "{}") }

        harness(service, actionConnectorMs = 10_000).use { h ->
            val stream = h.gate.stream(0)
            stream.sendPredict(requestId = 1)
            h.awaitTrue("predict is started") { service.started.isNotEmpty() }

            stream.sendStopServing()
            h.awaitTrue("gate has received half-close") { stream.indexOfClientCompleted() >= 0 }

            val predictResponse = stream.indexOfPredictResponse(requestId = 1)
            assertTrue(predictResponse >= 0, "in-flight request lost its response: ${stream.describe()}")
            assertTrue(
                predictResponse < stream.indexOfClientCompleted(),
                "half-close must happen after the response: ${stream.describe()}"
            )
            assertFalse(
                stream.messages().any { it.hasStopServing() },
                "SDK must not answer gate stopServing with its own: ${stream.describe()}"
            )
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun measure(block: () -> Unit): Long {
        val start = System.currentTimeMillis()
        block()
        return System.currentTimeMillis() - start
    }

    private fun harness(service: TestService, actionConnectorMs: Long) = Harness(service, actionConnectorMs)

    private class Harness(service: TestService, actionConnectorMs: Long) : AutoCloseable {
        val gate = FakeGate()
        val server: Server = ServerBuilder.forPort(0).addService(gate).build().start()
        val sdk: MlpServiceSDK

        init {
            val config = MlpServiceConfig(
                initialGateUrls = listOf("localhost:${server.port}"),
                connectionToken = "test-token",
                threadPoolSize = 4,
                shutdownConfig = ActionShutdownConfig(actionConnectorMs = actionConnectorMs),
                grpcSecure = false,
                ignoreClusterUpdates = true,
            )
            sdk = MlpServiceSDK(service, config, Dispatchers.IO)
            service.sdk = sdk
            sdk.start()
            awaitTrue("sdk is connected to the fake gate") {
                gate.streams.isNotEmpty() && gate.stream(0).messages().any { it.hasStartServing() }
            }
        }

        fun awaitTrue(what: String, timeoutMs: Long = 30_000, condition: () -> Boolean) {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                if (condition()) return
                Thread.sleep(20)
            }
            fail("timed out waiting for: $what")
        }

        override fun close() {
            runCatching { sdk.stop() }
            server.shutdownNow()
            server.awaitTermination(5, SECONDS)
        }
    }
}

private sealed class GateEvent {
    data class Message(val proto: ServiceToGateProto) : GateEvent()
    object ClientCompleted : GateEvent()
    data class ClientError(val error: Throwable) : GateEvent()
}

/** Одно gRPC-соединение processAsync со стороны заглушки гейта. */
private class FakeGateStream(private val responseObserver: StreamObserver<GateToServiceProto>) {

    val events = CopyOnWriteArrayList<GateEvent>()

    fun onClientMessage(proto: ServiceToGateProto) {
        events += GateEvent.Message(proto)
        // Контракт гейта: на stopServing сервиса отвечаем своим и держим вызов открытым.
        if (proto.hasStopServing()) {
            runCatching { sendStopServing() }
        }
    }

    fun sendPredict(requestId: Long, data: String = """{"in":1}""") = send(
        GateToServiceProto.newBuilder()
            .setRequestId(requestId)
            .setPredict(
                PredictRequestProto.newBuilder()
                    .setData(PayloadProto.newBuilder().setJson(data).setDataType("application/json"))
            )
            .build()
    )

    fun sendStopServing() = send(
        GateToServiceProto.newBuilder()
            .setStopServing(StopServingProto.getDefaultInstance())
            .build()
    )

    @Synchronized
    fun send(proto: GateToServiceProto) = responseObserver.onNext(proto)

    @Synchronized
    fun completeCall() = responseObserver.onCompleted()

    fun messages() = events.filterIsInstance<GateEvent.Message>().map { it.proto }

    fun indexOfStopServing() =
        events.indexOfFirst { it is GateEvent.Message && it.proto.hasStopServing() }

    fun indexOfPredictResponse(requestId: Long) =
        events.indexOfFirst { it is GateEvent.Message && it.proto.hasPredict() && it.proto.requestId == requestId }

    fun indexOfClientCompleted() = events.indexOfFirst { it is GateEvent.ClientCompleted }

    fun describe() = events.joinToString(prefix = "[", postfix = "]") {
        when (it) {
            is GateEvent.Message -> "${it.proto.bodyCase}(requestId=${it.proto.requestId})"
            is GateEvent.ClientCompleted -> "HALF_CLOSE"
            is GateEvent.ClientError -> "CLIENT_ERROR(${it.error.message})"
        }
    }
}

private class FakeGate : GateGrpc.GateImplBase() {

    val streams = CopyOnWriteArrayList<FakeGateStream>()

    fun stream(index: Int) = streams[index]

    override fun healthCheck(request: HeartBeatProto, responseObserver: StreamObserver<HeartBeatProto>) {
        responseObserver.onNext(HeartBeatProto.newBuilder().setStatus("Ok").build())
        responseObserver.onCompleted()
    }

    override fun processAsync(
        responseObserver: StreamObserver<GateToServiceProto>
    ): StreamObserver<ServiceToGateProto> {
        val stream = FakeGateStream(responseObserver)
        streams += stream

        return object : StreamObserver<ServiceToGateProto> {
            override fun onNext(value: ServiceToGateProto) = stream.onClientMessage(value)

            override fun onError(t: Throwable) {
                stream.events += GateEvent.ClientError(t)
            }

            override fun onCompleted() {
                stream.events += GateEvent.ClientCompleted
                runCatching { stream.completeCall() }
            }
        }
    }
}

private class TestService(
    private val behaviour: suspend TestService.(RequestContext) -> MlpResponse,
) : MlpService() {

    lateinit var sdk: MlpServiceSDK

    val started = CopyOnWriteArrayList<Long>()
    val finished = CopyOnWriteArrayList<Long>()

    override fun getDescriptor(): ServiceDescriptorProto =
        ServiceDescriptorProto.newBuilder().setName("graceful-shutdown-test").build()

    override suspend fun predict(req: Payload, config: Payload?, context: RequestContext): MlpResponse {
        started += context.gateRequestId
        val response = behaviour(context)
        finished += context.gateRequestId
        return response
    }
}
