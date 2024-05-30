### Объект `StorageFactory`

Объект `StorageFactory` предоставляет методы для создания экземпляров классов, реализующих интерфейс `Storage`, в зависимости от указанных параметров и настроек окружения.

#### Методы

- **getStorage(bucketName: String = getPlatformBucket(systemContext.environment)): Storage**: Возвращает экземпляр класса `Storage` в зависимости от типа хранилища, указанного в настройках окружения. Если тип хранилища не указан, выбрасывается исключение.

- **getStorage(context: MlpExecutionContext = systemContext, bucketName: String = getPlatformBucket(context.environment)): Storage**: Возвращает экземпляр класса `Storage` на основе контекста выполнения и имени бакета. Если тип хранилища не указан, выбрасывается исключение.

- **getDefaultStorageDir(context: MlpExecutionContext = systemContext): String?**: Возвращает путь к каталогу по умолчанию для хранения данных.

#### Приватные вспомогательные методы

- **getS3Service(bucketName: String, environment: Environment)**: Возвращает экземпляр класса `S3Storage` для работы с хранилищем S3.

- **getLocalStorage(context: MlpExecutionContext)**: Возвращает экземпляр класса `LocalStorage` для работы с локальным хранилищем.

- **createMinioClient(environment: Environment)**: Создает и настраивает клиент Minio для работы с хранилищем S3 на основе параметров окружения.

- **getPlatformBucket(environment: Environment)**: Возвращает имя бакета для хранения данных из параметров окружения. Если имя бакета не указано, выбрасывается исключение.