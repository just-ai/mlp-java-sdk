package com.mlp.sdk.storage

import java.io.File

interface Storage {

    /**
     * Saves a string content to the specified file path.
     *
     * @param content The content to save.
     * @param filePath The path where the content should be saved.
     */
    fun saveState(content: String, filePath: String)

    /**
     * Saves byte array content to the specified file path.
     *
     * @param content The content to save as a byte array.
     * @param filePath The path where the content should be saved.
     */
    fun saveState(content: ByteArray, filePath: String)

    /**
     * Saves the content of a File to the specified file path.
     *
     * @param content The File whose content should be saved.
     * @param filePath The path where the content should be saved.
     */
    fun saveState(content: File, filePath: String)

    /**
     * Loads the content from a specified path as a string.
     *
     * @param path The path from where the content should be loaded.
     * @return The content as a string, or null if the path is not found.
     */
    fun loadState(path: String): String?

    /**
     * Loads the content from a specified path as a byte array.
     *
     * @param path The path from where the content should be loaded.
     * @return The content as a byte array, or null if the path is not found.
     */
    fun loadStateBytes(path: String): ByteArray?
}
