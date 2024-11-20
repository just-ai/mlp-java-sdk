package com.mlp.sdk.files

import com.mlp.sdk.MlpExecutionContext
import com.mlp.sdk.MlpExecutionContext.Companion.systemContext

object FilesAccessorFactory {
    fun getAccessor(
        context: MlpExecutionContext = systemContext,
    ): FilesAccessor {
        val accessor = FilesAccessor(
            context.environment.getOrThrow("MLP_FILES_ENDPOINT"),
            context.environment.getOrThrow("MLP_SERVICE_TOKEN"),
            context.environment["MLP_FILES_MOUNT_PATH"],
            context.environment["MLP_FILES_BACKEND_NAME"]
        )
        return accessor
    }
}
