package com.mlp.sdk.storage

import com.mlp.sdk.InstanceContext
import com.mlp.sdk.WithInstanceContext
import java.io.File
import java.io.FileNotFoundException

class LocalStorage(
    override val context: InstanceContext
) : Storage, WithInstanceContext {

    /**
     * @deprecated Use constructor with context instead.
     */
    @Deprecated("Use constructor with context instead", ReplaceWith("LocalStorage(context)"))
    constructor(): this(InstanceContext())

    val baseDir = environment["MLP_STORAGE_DIR"] ?: "."

    override fun saveState(content: String, filePath: String) {
        val f = File(baseDir, filePath)
        f.parentFile.mkdirs()
        f.writeBytes(content.toByteArray())
    }

    override fun loadState(path: String): String {
        return String(File(baseDir, path).readBytes())
    }

    override fun saveState(content: ByteArray, filePath: String) {
        val f = File(baseDir, filePath)
        f.parentFile.mkdirs()
        f.writeBytes(content)
    }

    override fun loadStateBytes(path: String): ByteArray? {
        val f = File(baseDir, path)
        return if (f.exists()) f.readBytes() else null
    }

    override fun saveState(content: File, filePath: String) {
        if (!content.exists())
            throw FileNotFoundException("Source file ${content.absolutePath} doesn't exist!")

        val f = File(baseDir, filePath)
        f.parentFile.mkdirs()
        content.copyTo(f, true)
    }

    companion object {
        const val STORAGE_NAME = "local"
    }
}
