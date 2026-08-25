package com.mlp.sdk.files

import java.io.ByteArrayInputStream
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TemporaryFileTest {

    @Test
    fun `temporary file is removed after successful upload`() {
        val payload = "fal-result".toByteArray()
        val stream = CloseTrackingInputStream(payload)
        lateinit var temporaryFile: File

        val result = withTemporaryFile(stream) { file ->
            temporaryFile = file
            assertTrue(file.exists())
            assertEquals(payload.toList(), file.readBytes().toList())
            "uploaded"
        }

        assertEquals("uploaded", result)
        assertFalse(temporaryFile.exists())
        assertTrue(stream.closed)
    }

    @Test
    fun `temporary file is removed when upload fails`() {
        val stream = CloseTrackingInputStream("fal-result".toByteArray())
        val uploadFailure = IllegalStateException("upload failed")
        lateinit var temporaryFile: File

        val actualFailure = assertThrows(IllegalStateException::class.java) {
            withTemporaryFile(stream) { file ->
                temporaryFile = file
                assertTrue(file.exists())
                throw uploadFailure
            }
        }

        assertSame(uploadFailure, actualFailure)
        assertFalse(temporaryFile.exists())
        assertTrue(stream.closed)
    }

    private class CloseTrackingInputStream(payload: ByteArray) : ByteArrayInputStream(payload) {
        var closed = false

        override fun close() {
            closed = true
            super.close()
        }
    }
}
