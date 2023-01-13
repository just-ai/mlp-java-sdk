package com.mlp.sdk.storage

interface Storage {
    fun saveState(content: String, filePath: String)
    fun loadState(path: String): String?
}