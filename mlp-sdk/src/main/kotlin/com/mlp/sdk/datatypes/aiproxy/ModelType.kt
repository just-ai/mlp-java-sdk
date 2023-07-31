package com.mlp.sdk.datatypes.aiproxy

enum class ModelType(
    val modelName: String,
    val maxContextLength: Int
) {
    // chat
    GPT_4("gpt-4", 8192),
    GPT_4_32K("gpt-4-32k", 32768),
    GPT_3_5_TURBO("gpt-3.5-turbo", 4097),
    GPT_3_5_TURBO_16K("gpt-3.5-turbo-16k", 16384),

    // text
    TEXT_DAVINCI_003("text-davinci-003", 4097),
    TEXT_DAVINCI_002("text-davinci-002", 4097),
    TEXT_DAVINCI_001("text-davinci-001", 2049),
    TEXT_CURIE_001("text-curie-001", 2049),
    TEXT_BABBAGE_001("text-babbage-001", 2049),
    TEXT_ADA_001("text-ada-001", 2049),
    DAVINCI("davinci", 2049),
    CURIE("curie", 2049),
    BABBAGE("babbage", 2049),
    ADA("ada", 2049),

    // code
    CODE_DAVINCI_002("code-davinci-002", 8001),
    CODE_DAVINCI_001("code-davinci-001", 8001),
    CODE_CUSHMAN_002("code-cushman-002", 2048),
    CODE_CUSHMAN_001("code-cushman-001", 2048),
    DAVINCI_CODEX("davinci-codex", 4096),
    CUSHMAN_CODEX("cushman-codex", 2048),

    // edit
    TEXT_DAVINCI_EDIT_001("text-davinci-edit-001", 3000),
    CODE_DAVINCI_EDIT_001("code-davinci-edit-001", 3000),

    // embeddings
    TEXT_EMBEDDING_ADA_002("text-embedding-ada-002", 8191),

    // old embeddings
    TEXT_SIMILARITY_DAVINCI_001("text-similarity-davinci-001", 2046),
    TEXT_SIMILARITY_CURIE_001("text-similarity-curie-001", 2046),
    TEXT_SIMILARITY_BABBAGE_001("text-similarity-babbage-001", 2046),
    TEXT_SIMILARITY_ADA_001("text-similarity-ada-001", 2046),
    TEXT_SEARCH_DAVINCI_DOC_001("text-search-davinci-doc-001", 2046),
    TEXT_SEARCH_CURIE_DOC_001("text-search-curie-doc-001", 2046),
    TEXT_SEARCH_BABBAGE_DOC_001("text-search-babbage-doc-001", 2046),
    TEXT_SEARCH_ADA_DOC_001("text-search-ada-doc-001", 2046),
    CODE_SEARCH_BABBAGE_CODE_001("code-search-babbage-code-001", 2046),
    CODE_SEARCH_ADA_CODE_001("code-search-ada-code-001", 2046);

    companion object {

        private val nameToModelType: Map<String, ModelType> = ModelType.values()
            .associateBy { it.modelName }

        fun fromName(name: String) = nameToModelType[name]
    }
}
