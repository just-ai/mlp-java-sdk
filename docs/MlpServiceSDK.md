### Описание класса MlpServiceSDK

Класс `MlpServiceSDK` представляет собой SDK (Software Development Kit) для взаимодействия с сервисом MLP (Machine Learning Platform).

#### Поля класса

- **SDK_COMPONENT_NAME**: Константа типа `String`, содержащая имя компонента SDK.
- **STARTUP_PROBE_FILE_PATH**: Константа типа `String`, указывающая путь к файлу запуска.

#### Методы

1. **Конструкторы**

```kotlin
public constructor(action: MlpService, initConfig: MlpServiceConfig?, dispatcher: CoroutineDispatcher?)
```

Создает экземпляр SDK для взаимодействия с сервисом MLP.

    - `action`: Объект класса `MlpService`, представляющий действия, выполняемые сервисом.
    - `initConfig`: Необязательный параметр, объект класса `MlpServiceConfig`, содержащий начальную конфигурацию.
    - `dispatcher`: Необязательный параметр, объект класса `CoroutineDispatcher`, представляющий диспетчер корутин.

```kotlin
public constructor(actionProvider: () -> MlpService, config: MlpServiceConfig?, dispatcher: CoroutineDispatcher?)
```

Альтернативный конструктор для создания экземпляра SDK с использованием провайдера действия.

    - `actionProvider`: Функция, возвращающая объект класса `MlpService`.
    - `config`: Необязательный параметр, объект класса `MlpServiceConfig`, содержащий начальную конфигурацию.
    - `dispatcher`: Необязательный параметр, объект класса `CoroutineDispatcher`, представляющий диспетчер корутин.

2. **Методы**

- **blockUntilShutdown()**: Блокирует вызывающий поток до завершения работы.
- **getConnectorsPoolState()**: Возвращает текущее состояние пула коннекторов.
- **gracefulShutdown()**: Производит грациозное завершение работы SDK.
- **send(connectorId, toGateProto)**: Отправляет данные в гейт сервиса.
    - `connectorId`: Идентификатор коннектора.
    - `toGateProto`: Объект класса `ServiceToGateProto`, содержащий данные для отправки.
- **setShutdownHook()**: Устанавливает хук завершения работы SDK.
- **shutdownConnectorsPool()**: Останавливает пул коннекторов.
- **start()**: Запускает SDK.
- **startConnectorsPool()**: Запускает пул коннекторов.
- **startupProbe()**: Проверяет запуск сервиса.
- **stop()**: Останавливает SDK.

3. **Другие методы**

- **toString()**: Предоставляет строковое представление объекта.

