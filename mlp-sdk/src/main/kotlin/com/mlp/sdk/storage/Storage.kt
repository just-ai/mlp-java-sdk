package com.mlp.sdk.storage

interface Storage {
    fun saveState(content: String, path: String)
    fun loadState(path: String): String?
}