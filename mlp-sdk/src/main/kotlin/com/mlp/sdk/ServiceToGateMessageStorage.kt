package com.mlp.sdk

import com.mlp.gate.ServiceToGateProto
import com.mlp.gate.ServiceToGateProtoOrBuilder
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicLong

class ServiceToGateMessageStorage {

    private val messagesBySequenceNumbers = ConcurrentSkipListMap<Long, ServiceToGateProto.Builder>()

    private val lastSentSequenceNumberRef = AtomicLong()

    val lastSentSequenceNumber: Long
        get() = lastSentSequenceNumberRef.get()

    fun setSequenceNumberAndStoreMessage(message: ServiceToGateProto.Builder): ServiceToGateProto.Builder {
        if (!message.isSequenced()) return message

//        message.sequenceNumber = lastSentSequenceNumberRef.incrementAndGet()
//
//        messagesBySequenceNumbers[message.sequenceNumber] = message

        return message
    }

    fun getAllStoredMessages(): Collection<ServiceToGateProto.Builder> =
        messagesBySequenceNumbers.values

    fun removeMessagesUntilSequenceNumber(sequenceNumber: Long) {
        messagesBySequenceNumbers.headMap(sequenceNumber + 1).clear()
    }

    private fun ServiceToGateProtoOrBuilder.isSequenced() = when (bodyCase) {
        ServiceToGateProto.BodyCase.PREDICT,
        ServiceToGateProto.BodyCase.PARTIALPREDICT,
        ServiceToGateProto.BodyCase.FIT,
        ServiceToGateProto.BodyCase.EXT,
        ServiceToGateProto.BodyCase.BATCH,
        ServiceToGateProto.BodyCase.ERROR,
        ServiceToGateProto.BodyCase.DEFERREDBILLINGCHARGE,
        ServiceToGateProto.BodyCase.DEFERREDBILLINGCHARGES -> true

        ServiceToGateProto.BodyCase.HEARTBEAT,
        ServiceToGateProto.BodyCase.STARTSERVING,
        ServiceToGateProto.BodyCase.STOPSERVING,
        ServiceToGateProto.BodyCase.STATUS,
        ServiceToGateProto.BodyCase.FITSTATUS,
        ServiceToGateProto.BodyCase.BODY_NOT_SET -> false

        null -> false
    }
}
