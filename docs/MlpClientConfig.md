### Описание класса MlpClientConfig

Класс `MlpClientConfig` представляет конфигурацию клиента для взаимодействия с сервисом MLP (Machine Learning Platform).

#### Поля класса

- **initialGateUrls**: Список начальных URL-адресов шлюзов gRPC.
- **restUrl**: URL-адрес для взаимодействия с REST API.
- **clientToken**: Токен клиента для аутентификации.
- **clientPredictTimeoutMs**: Таймаут для запроса предсказания (в миллисекундах).
- **shutdownConfig**: Конфигурация для корректного завершения работы клиента.
- **grpcSecure**: Флаг, указывающий на использование безопасного соединения gRPC.
- **maxBackoffSeconds**: Максимальное время для задержки повторных попыток подключения (в секундах).
- **clientApiGateUrl**: URL-адрес для взаимодействия с API шлюза.

#### Companion object

- **CLIENT_PREDICT_TIMEOUT_MS**: Значение по умолчанию для таймаута предсказания (в миллисекундах).
- **CLIENT_PREDICT_RETRY_MAX_ATTEMPTS**: Максимальное количество попыток повтора запроса предсказания.
- **CLIENT_PREDICT_RETRY_BACKOFF_MS**: Время задержки перед повторной попыткой запроса предсказания (в миллисекундах).
- **CLIENT_PREDICT_RETRYABLE_ERROR_CODES**: Список кодов ошибок, при которых можно повторить запрос предсказания.
- **GRACEFUL_SHUTDOWN_CLIENT_MS**: Время для корректного завершения работы клиента (в миллисекундах).
- **GRPC_SECURE**: Флаг, указывающий на использование безопасного соединения gRPC.
- **MAX_BACKOFF_SECONDS**: Максимальное время для задержки повторных попыток подключения (в секундах).

#### Методы

- **loadClientConfig(configPath: String?): MlpClientConfig**

  Загружает конфигурацию клиента из системных переменных окружения.

- **loadClientConfig(configPath: String?, environment: Environment): MlpClientConfig**

  Загружает конфигурацию клиента из файла или из системных переменных окружения с помощью объекта `Environment`.

### Дополнительные классы

1. **ClientPredictRetryConfig**

   Конфигурация для повторных попыток запроса предсказания.

    - **maxAttempts**: Максимальное количество попыток повтора запроса.
    - **backoffMs**: Время задержки перед повторной попыткой (в миллисекундах).
    - **retryableErrorCodes**: Список кодов ошибок, при которых можно повторить запрос.

2. **ClientShutdownConfig**

   Конфигурация для корректного завершения работы клиента.

    - **clientMs**: Время для корректного завершения работы клиента (в миллисекундах).

### Использование

```kotlin
// Загрузка конфигурации клиента из системных переменных окружения
val clientConfig = MlpClientConfig.loadClientConfig()

// Использование конфигурации клиента для создания экземпляра клиента MLP
val mlpClient = MlpApiClient(
    clientConfig.clientToken,
    clientConfig.clientApiGateUrl ?: error("Missing clientApiGateUrl in config"),
    billingToken = clientConfig.billingToken
)
```

### Важно

Перед использованием необходимо установить значения необходимых параметров в конфигурации или в системных переменных окружения.