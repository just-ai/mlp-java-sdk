### ModelType

Перечисление, представляющее различные типы моделей для использования в AI Proxy.

#### Значения:
- `GPT_4`: Модель GPT-4.
- `GPT_4_32K`: Модель GPT-4 с контекстом размером 32K.
- `GPT_3_5_TURBO`: Модель GPT-3.5 Turbo.
- `GPT_3_5_TURBO_16K`: Модель GPT-3.5 Turbo с контекстом размером 16K.
- `TEXT_DAVINCI_003`: Модель Davinci для текста.
- `TEXT_DAVINCI_002`: Модель Davinci для текста.
- `TEXT_DAVINCI_001`: Модель Davinci для текста.
- `TEXT_CURIE_001`: Модель Curie для текста.
- `TEXT_BABBAGE_001`: Модель Babbage для текста.
- `TEXT_ADA_001`: Модель Ada для текста.
- `DAVINCI`: Модель Davinci.
- `CURIE`: Модель Curie.
- `BABBAGE`: Модель Babbage.
- `ADA`: Модель Ada.
- `CODE_DAVINCI_002`: Модель Davinci для кода.
- `CODE_DAVINCI_001`: Модель Davinci для кода.
- `CODE_CUSHMAN_002`: Модель Cushman для кода.
- `CODE_CUSHMAN_001`: Модель Cushman для кода.
- `DAVINCI_CODEX`: Модель Davinci Codex.
- `CUSHMAN_CODEX`: Модель Cushman Codex.
- `TEXT_DAVINCI_EDIT_001`: Модель Davinci для редактирования текста.
- `CODE_DAVINCI_EDIT_001`: Модель Davinci для редактирования кода.
- `TEXT_EMBEDDING_ADA_002`: Модель Ada для встраивания текста.
- `TEXT_SIMILARITY_DAVINCI_001`: Модель Davinci для сравнения текста.
- `TEXT_SIMILARITY_CURIE_001`: Модель Curie для сравнения текста.
- `TEXT_SIMILARITY_BABBAGE_001`: Модель Babbage для сравнения текста.
- `TEXT_SIMILARITY_ADA_001`: Модель Ada для сравнения текста.
- `TEXT_SEARCH_DAVINCI_DOC_001`: Модель Davinci для поиска текста.
- `TEXT_SEARCH_CURIE_DOC_001`: Модель Curie для поиска текста.
- `TEXT_SEARCH_BABBAGE_DOC_001`: Модель Babbage для поиска текста.
- `TEXT_SEARCH_ADA_DOC_001`: Модель Ada для поиска текста.
- `CODE_SEARCH_BABBAGE_CODE_001`: Модель Babbage для поиска кода.
- `CODE_SEARCH_ADA_CODE_001`: Модель Ada для поиска кода.

#### Свойства:
- `modelName`: Название модели.
- `maxContextLength`: Максимальная длина контекста.

#### Методы:
- `fromName(name: String)`: Получает тип модели по ее имени.
