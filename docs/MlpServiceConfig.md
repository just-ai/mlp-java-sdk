### Описание класса MlpServiceConfig

Класс `MlpServiceConfig` представляет собой конфигурацию для инициализации сервиса MLP (Machine Learning Platform).

#### Поля класса

- **initialGateUrls**: Список URL-адресов для инициализации сервиса.
- **connectionToken**: Токен соединения для аутентификации.
- **threadPoolSize**: Размер пула потоков для обработки запросов.
- **shutdownConfig**: Конфигурация для процесса грациозного завершения работы сервиса.
- **grpcConnectTimeoutMs**: Время ожидания подключения к gRPC (в миллисекундах).
- **grpcSecure**: Флаг, указывающий на необходимость использования безопасного соединения gRPC.
- **clientApiAuthToken**: Токен API клиента для аутентификации (необязательно).

#### Методы

1. **Конструктор**

```kotlin
public constructor(initialGateUrls: List<String>, connectionToken: String, threadPoolSize: Int, shutdownConfig: ActionShutdownConfig, grpcConnectTimeoutMs: Long, grpcSecure: Boolean, clientApiAuthToken: String?)
```

Создает экземпляр конфигурации сервиса MLP с указанными параметрами.

2. **Другие методы**

- **componentN()**: Методы-компоненты, возвращающие соответствующее значение из конфигурации (используется для деструктуризации объекта).

