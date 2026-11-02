package com.mlp.sdk

import com.mlp.sdk.files.FilesAccessor
import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayInputStream
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpInputMessage

class FileHttpMessageConverterTest {
    private fun download(name: String, content: String): File =
        FileHttpMessageConverter().read(File::class.java, object : HttpInputMessage {
            override fun getHeaders() = HttpHeaders().apply {
                set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$name")
            }
            override fun getBody() = ByteArrayInputStream(content.toByteArray())
        })

    @Test
    fun `relative traversal cannot overwrite an existing file`() {
        val tempDirectory = FileUtils.getTempDirectory().canonicalFile
        val isolated = Files.createTempDirectory(tempDirectory.toPath(), "sdk-victim-").toFile()
        val victim = File(isolated, "victim.txt").apply { writeText("unchanged") }
        val name = "../${tempDirectory.name}/${victim.relativeTo(tempDirectory).path}"
        assertEquals(victim.canonicalFile, File(tempDirectory, name).canonicalFile)
        try {
            val result = download(name, "downloaded")
            try {
                assertEquals("unchanged", victim.readText())
                assertEquals(tempDirectory, result.canonicalFile.parentFile)
                assertEquals("downloaded", result.readText())
            } finally { result.delete() }
        } finally { isolated.deleteRecursively() }
    }

    @Test
    fun `same filename downloads remain independent`() {
        val name = "sdk-download-${System.nanoTime()}.txt"
        val first = download(name, "first")
        val second = download(name, "second")
        try {
            assertNotEquals(first.canonicalFile, second.canonicalFile)
            assertEquals("first", first.readText())
            assertEquals("second", second.readText())
        } finally { first.delete(); second.delete() }
    }

    @Test
    fun `API file read supplies original bytes to upload consumer`() {
        val payload = "synthetic-fal-upload".toByteArray()
        var requestToken: String? = null
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requestToken = exchange.requestHeaders.getFirst("MLP-API-KEY")
            exchange.responseHeaders.set("Content-Type", "application/octet-stream")
            exchange.responseHeaders.set("Content-Disposition", "attachment; filename=allowed-file.bin")
            exchange.sendResponseHeaders(200, payload.size.toLong())
            exchange.responseBody.use { it.write(payload) }
            exchange.close()
        }
        server.start()
        try {
            val accessor = FilesAccessor("http://127.0.0.1:${server.address.port}", "synthetic-token")
            val downloadsBefore = downloadedTempFiles()
            val uploaded = accessor.read("synthetic-file").use { it.readBytes() }
            assertArrayEquals(payload, uploaded)
            assertEquals(downloadsBefore, downloadedTempFiles())
            assertEquals("synthetic-token", requestToken)
        } finally { server.stop(0) }
    }

    private fun downloadedTempFiles(): Set<String> =
        FileUtils.getTempDirectory().list { _, name -> name.startsWith("mlp-download-") }.orEmpty().toSet()
}
