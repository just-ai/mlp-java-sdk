# MlpClientException

`MlpClientException` - это класс для обработки исключений в части приложения Mlp SDK.

## Конструкторы

- **MlpClientException(errorCode: String, errorMessage: String, args: Map<String, String>, requestId: String?)**

  Создает новый экземпляр исключения с указанными параметрами.

    - `errorCode`: Строковое значение кода ошибки.
    - `errorMessage`: Строковое значение сообщения об ошибке.
    - `args`: Map с аргументами, связанными с ошибкой.
    - `requestId`: Опциональная строка, представляющая идентификатор запроса, связанного с исключением.

## Свойства

- `errorCode`: Строковое значение кода ошибки.
- `errorMessage`: Строковое значение сообщения об ошибке.
- `args`: Map с аргументами, связанными с ошибкой.
- `requestId`: Опциональная строка, представляющая идентификатор запроса, связанного с исключением.

## Использование

```kotlin
try {
    // Код, где может возникнуть исключение
} catch (ex: MlpClientException) {
    // Обработка исключения
    println("Код ошибки: ${ex.errorCode}")
    println("Сообщение об ошибке: ${ex.errorMessage}")
    println("Аргументы ошибки: ${ex.args}")
    println("Идентификатор запроса: ${ex.requestId}")
}
```

