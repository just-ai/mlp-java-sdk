### Описание интерфейса и классов в SDK MLP

#### Интерфейс `MlpResponse`

Представляет собой общий интерфейс для различных типов ответов от сервиса MLP.

#### Объект `BillingUnitsThreadLocal`

Предоставляет механизм для хранения единиц биллинга в локальном потоке.

- **Методы:**
    - `clear()`: Очищает значение единиц биллинга из локального потока.
    - `setUnits(units: Long)`: Устанавливает значение единиц биллинга в локальном потоке.
    - `getUnits(): Long?`: Получает значение единиц биллинга из локального потока.

#### Класс `Payload`

Представляет данные в виде строки с указанием типа данных.

- **Поля:**
    - `dataType`: Тип данных.
    - `data`: Строка данных.

- **Конструкторы:**
    - `constructor(data: String)`: Создает объект Payload с указанными данными и пустым типом данных.

- **Сопутствующие объекты:**
    - `emptyPayload`: Пустой Payload, используется для инициализации.

#### Класс `RawPayload`

Представляет данные в виде строки с указанием типа данных и заголовков.

- **Поля:**
    - `dataType`: Тип данных.
    - `data`: Строка данных.
    - `headers`: Заголовки данных.

- **Методы:**
    - `asPayload`: Преобразует RawPayload в объект Payload.

#### Класс `MlpResponseException`

Представляет исключение как ответ от сервиса MLP.

- **Поля:**
    - `exception`: Исключение.

#### Класс `MlpPartialBinaryResponse`

Представляет частичный бинарный ответ от сервиса MLP.

### Использование

Пример использования классов и интерфейсов:

```kotlin
// Установка значения единиц биллинга в локальном потоке
BillingUnitsThreadLocal.setUnits(100)

// Получение значения единиц биллинга из локального потока
val units = BillingUnitsThreadLocal.getUnits()

// Создание объекта Payload
val payload = Payload("{"key": "value"}")

// Создание объекта RawPayload
val rawPayload = RawPayload("json", "{\"key\": \"value\"}", mapOf("Content-Type" to "application/json"))

// Преобразование RawPayload в Payload
val payloadFromRaw = rawPayload.asPayload

// Обработка исключения как MlpResponse
val exceptionResponse = MlpResponseException(Exception("An error occurred"))

// Создание объекта MlpPartialBinaryResponse
val partialBinaryResponse = MlpPartialBinaryResponse()
```


