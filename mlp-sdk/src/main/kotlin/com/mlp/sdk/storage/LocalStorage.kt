package com.mlp.sdk.storage

import java.io.File

class LocalStorage : Storage {
    val baseDir = System.getenv("MLP_STORAGE_DIR") ?: "."
    override fun saveState(content: String, filePath: String) {
        val f = File(baseDir, filePath)
        f.parentFile.mkdirs()
        f.writeBytes(content.toByteArray())
    }

    override fun loadState(path: String): String? {
        return String(File(baseDir, path).readBytes())
    }

    override fun saveState(content: ByteArray, filePath: String) {
        TODO("Not yet implemented")
    }

    override fun loadStateBytes(path: String): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun saveState(content: File, filePath: String) {
        TODO("Not yet implemented")
    }

    companion object {
        const val STORAGE_NAME = "local"
    }
}