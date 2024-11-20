package com.mlp.sdk.files

import com.justai.caila.storage.api.ApiClient
import com.justai.caila.storage.api.client.FilesEndpointApi
import com.justai.caila.storage.api.client.model.FileOptions
import java.io.File
import java.io.InputStream
import java.util.UUID

class FilesAccessor(
    url: String,
    token: String,
    private val mountPath: String? = null,
    private val backendName: String? = null
) {

    private val client = ApiClient().apply {
        basePath = url
        addDefaultHeader("MLP-API-KEY", token)
    }
    private var filesApi = FilesEndpointApi(client)

    fun read(fileId: FileId, version: Int? = null): InputStream {
        if (onlyApi()) {
            return readByApi(fileId, version)
        }

        val fileRelativePath = filesApi.getFilePath(fileId, backendName, version)
        val file = File("$mountPath/$fileRelativePath")
        if (!file.exists()) {
            return readByApi(fileId, version)
        }

        return file.inputStream()
    }

    fun write(stream: InputStream, key: FileId? = null, options: FileOptions? = null): FileId {
        if (onlyApi()) return writeByApi(stream, key, options)

        val tempName = UUID.randomUUID().toString()
        val tempFile = File("${mountPath}/$tempName")
        writeToFile(tempFile, stream)

        return filesApi.registerFile(key, tempName, options).key
    }

    fun write(file: File, key: FileId? = null, options: FileOptions? = null): FileId {
        if (onlyApi()) return writeByApi(file, key, options)

        val tempName = UUID.randomUUID().toString()
        val tempFile = File("${mountPath}/$tempName")
        file.copyTo(tempFile)

        return filesApi.registerFile(key, tempName, options).key
    }

    private fun readByApi(fileId: FileId, version: Int?): InputStream {
        return filesApi.getFileContent(fileId, version).inputStream()
    }

    private fun writeByApi(file: File, key: FileId? = null, options: FileOptions? = null): FileId {
        return filesApi.uploadMultipartFile(
            file,
            key,
            options
        ).key
    }

    private fun writeByApi(stream: InputStream, key: FileId? = null, options: FileOptions? = null): FileId {
        val tempFile = File.createTempFile("mlp", "file")
        writeToFile(tempFile, stream)

        return writeByApi(tempFile, key, options)
    }

    private fun writeToFile(tempFile: File, stream: InputStream) {
        if (!tempFile.exists())
            tempFile.createNewFile()

        tempFile.outputStream().use {
            stream.copyTo(it)
        }
        stream.close()
    }

    private fun onlyApi() = mountPath == null || backendName == null
}

typealias FileId = String
