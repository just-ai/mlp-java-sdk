package com.mlp.sdk.storage

import java.io.File

class LocalStorage : Storage {
    override fun saveState(content: String, path: String) {
        File(path).writeBytes(content.toByteArray())
    }

    override fun loadState(path: String): String? {
        return String(File(path).readBytes())
    }

    companion object {
        const val STORAGE_NAME = "local"
    }
}