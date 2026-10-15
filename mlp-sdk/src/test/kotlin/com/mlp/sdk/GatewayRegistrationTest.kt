package com.mlp.sdk

import com.mlp.gate.ApiErrorProto
import com.mlp.gate.GateGrpc
import com.mlp.gate.GateToServiceProto
import com.mlp.gate.HeartBeatProto
import com.mlp.gate.ServiceDescriptorProto
import com.mlp.gate.ServiceInfoProto
import com.mlp.gate.ServiceToGateProto
import com.mlp.gate.StopServingProto
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class GatewayRegistrationTest {

    @Test
    fun `rejected token is retried with growing pause instead of a tight loop`() {
        val gate = RegistrationGate(acceptToken = false)
        GateHarness(gate).use { h ->
            Thread.sleep(8_000)

            // Without backoff the connector reconnects about once a second.
            assertTrue(gate.startServingCount.get() in 2..4, "startServing attempts: ${gate.startServingCount.get()}")
            assertTrue(h.sdk.timeSinceGatewayContact() >= Duration.ofSeconds(8))
        }
    }

    @Test
    fun `legacy gate error code is also treated as rejected token`() {
        val gate = RegistrationGate(acceptToken = false, errorCode = "mlp.gate.instance_by_token_not_found")
        GateHarness(gate).use {
            Thread.sleep(8_000)

            assertTrue(gate.startServingCount.get() in 2..4, "startServing attempts: ${gate.startServingCount.get()}")
        }
    }

    @Test
    fun `accepted registration and heartbeats keep gateway contact fresh`() {
        val gate = RegistrationGate(acceptToken = true, serviceInfoDelayMs = 3_000)
        GateHarness(gate).use { h ->
            h.awaitTrue("startServing received") { gate.streams.isNotEmpty() && gate.startServingCount.get() == 1 }
            Thread.sleep(2_000)
            assertTrue(h.sdk.timeSinceGatewayContact() >= Duration.ofSeconds(2), "contact before serviceInfo")

            h.awaitTrue("serviceInfo refreshes contact") { h.sdk.timeSinceGatewayContact() < Duration.ofMillis(500) }

            Thread.sleep(1_500)
            gate.streams.last().send(
                GateToServiceProto.newBuilder()
                    .setHeartBeat(HeartBeatProto.newBuilder().setInterval(60_000))
                    .build()
            )
            h.awaitTrue("heartbeat refreshes contact") { h.sdk.timeSinceGatewayContact() < Duration.ofMillis(500) }
            assertEquals(1, gate.startServingCount.get())
        }
    }

    @Test
    fun `backoff doubles from one second up to thirty`() {
        assertEquals(
            listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L),
            (1..7).map { Connector.rejectedTokenBackoffMs(it) }
        )
    }

    private class GateHarness(gate: RegistrationGate) : AutoCloseable {
        private val server: Server = ServerBuilder.forPort(0).addService(gate).build().start()
        val sdk: MlpServiceSDK

        init {
            val config = MlpServiceConfig(
                initialGateUrls = listOf("localhost:${server.port}"),
                connectionToken = "1000062767.30021.test",
                threadPoolSize = 2,
                grpcSecure = false,
            )
            sdk = MlpServiceSDK(RegistrationService(), config, Dispatchers.IO)
            sdk.start()
        }

        fun awaitTrue(what: String, timeoutMs: Long = 10_000, condition: () -> Boolean) {
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

    /** Mirrors the gateway handshake: unknown token gets error, stopServing and a closed stream. */
    private class RegistrationGate(
        private val acceptToken: Boolean,
        private val errorCode: String = "mlp.gateway.instance_by_token_not_found",
        private val serviceInfoDelayMs: Long = 0,
    ) : GateGrpc.GateImplBase() {

        val startServingCount = AtomicInteger()
        val streams = CopyOnWriteArrayList<GateStream>()

        override fun healthCheck(request: HeartBeatProto, responseObserver: StreamObserver<HeartBeatProto>) {
            responseObserver.onNext(HeartBeatProto.newBuilder().setStatus("Ok").build())
            responseObserver.onCompleted()
        }

        override fun processAsync(responseObserver: StreamObserver<GateToServiceProto>): StreamObserver<ServiceToGateProto> {
            val stream = GateStream(responseObserver)
            streams += stream
            return object : StreamObserver<ServiceToGateProto> {
                override fun onNext(value: ServiceToGateProto) {
                    if (!value.hasStartServing()) return
                    startServingCount.incrementAndGet()
                    if (acceptToken) {
                        thread {
                            Thread.sleep(serviceInfoDelayMs)
                            stream.send(
                                GateToServiceProto.newBuilder()
                                    .setServiceInfo(ServiceInfoProto.newBuilder().setAccountId(1000062767L).setModelId(30021L))
                                    .build()
                            )
                        }
                    } else {
                        stream.send(
                            GateToServiceProto.newBuilder()
                                .setError(ApiErrorProto.newBuilder().setCode(errorCode).setMessage("Instance was not found"))
                                .build()
                        )
                        stream.send(GateToServiceProto.newBuilder().setStopServing(StopServingProto.getDefaultInstance()).build())
                        stream.complete()
                    }
                }

                override fun onError(t: Throwable) = Unit

                override fun onCompleted() = runCatching { stream.complete() }.let { }
            }
        }
    }

    private class GateStream(private val observer: StreamObserver<GateToServiceProto>) {
        private var completed = false

        @Synchronized
        fun send(proto: GateToServiceProto) {
            if (!completed) observer.onNext(proto)
        }

        @Synchronized
        fun complete() {
            if (completed) return
            completed = true
            observer.onCompleted()
        }
    }

    private class RegistrationService : MlpService() {
        override fun getDescriptor(): ServiceDescriptorProto =
            ServiceDescriptorProto.newBuilder().setName("gateway-registration-test").build()

        override suspend fun predict(req: Payload, config: Payload?, context: RequestContext): MlpResponse =
            Payload("application/json", "{}")
    }
}
