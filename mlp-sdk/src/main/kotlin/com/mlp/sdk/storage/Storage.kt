package com.mlp.sdk.storage

import java.io.File

interface Storage {
    fun saveState(content: String, filePath: String)
    fun saveState(content: ByteArray, filePath: String)
    fun saveState(content: File, filePath: String)

    fun loadState(path: String): String?
    fun loadStateBytes(path: String): ByteArray?
}