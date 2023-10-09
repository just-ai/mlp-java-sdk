package com.mlp.sdk.storage

import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext
import com.mlp.sdk.WithExecutionContext
import java.io.File
import java.io.FileNotFoundException

class LocalStorage(
    override val context: MlpExecutionContext = systemContext
) : Storage, WithExecutionContext {


    val baseDir = environment["MLP_STORAGE_DIR"] ?: "."

    override fun saveState(content: String, filePath: String) {
        val file = prepareFile(filePath)
        file.writeBytes(content.toByteArray())
    }

    override fun saveState(content: ByteArray, filePath: String) {
        val file = prepareFile(filePath)
        file.writeBytes(content)
    }

    override fun saveState(content: File, filePath: String) {
        if (!content.exists()) {
            logger.error("Save of source file ${content.absolutePath} is impossible, because it doesn't exist!")
            throw FileNotFoundException("Source file ${content.absolutePath} doesn't exist!")
        }

        val file = prepareFile(filePath)
        content.copyTo(file, true)
    }

    override fun loadState(path: String): String? {
        return loadStateBytes(path)?.let(::String)
    }

    override fun loadStateBytes(path: String): ByteArray? {
        val file = File(baseDir, path)
        return if (file.exists()) file.readBytes() else null
    }


    private fun prepareFile(filePath: String) =
        File(baseDir, filePath)
            .also { it.parentFile.mkdirs() }

    companion object {
        const val STORAGE_NAME = "local"
    }
}
