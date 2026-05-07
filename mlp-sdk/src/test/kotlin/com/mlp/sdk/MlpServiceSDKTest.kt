package com.mlp.sdk

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MlpServiceSDKTest {

    @Test
    fun `processInstanceUuid should be stable within the same process`() {
        val uuid1 = MlpServiceSDK.processInstanceUuid
        val uuid2 = MlpServiceSDK.processInstanceUuid
        assertEquals(uuid1, uuid2)
        assertTrue(uuid1.isNotBlank())
    }
}
