### Описание класса MlpApiClient

Класс `MlpApiClient` представляет клиент для взаимодействия с API сервиса MLP (Machine Learning Platform).

#### Поля класса

- **defaultApiToken**: Токен для аутентификации по умолчанию.
- **apiGateUrl**: URL-адрес шлюза API.
- **restTemplate**: Шаблон REST-запросов для взаимодействия с API.

#### Методы

1. **init {...}**

   Инициализирует поля класса и устанавливает базовый путь для API, а также добавляет заголовки авторизации.

2. **getInstance(defaultApiToken: String?, apiGateUrl: String?): MlpApiClient**

   Получает экземпляр клиента для взаимодействия с API.

3. **getRestTemplate(): RestTemplate**

   Возвращает настроенный шаблон REST-запросов.

#### Дополнительные классы

1. **FileHttpMessageConverter**

   Конвертер для обработки файловых данных в HTTP-сообщениях.

    - **getDefaultContentType(file: File): MediaType**: Возвращает тип контента по умолчанию.
    - **readInternal(clazz: Class<out File>, inputMessage: HttpInputMessage): File**: Считывает данные из HTTP-сообщения в файл.
    - **supports(clazz: Class<*>): Boolean**: Проверяет поддержку конвертации для заданного класса.
    - **writeInternal(file: File, outputMessage: HttpOutputMessage)**: Записывает файловые данные в HTTP-сообщение.

### Использование

```kotlin
// Создание экземпляра клиента MLP API
val defaultApiToken = "your-api-token"
val apiGateUrl = "https://api-gate-url.com"
val billingToken: String? = null // Опциональный токен для выставления счетов
val mlpApiClient = MlpApiClient.getInstance(defaultApiToken, apiGateUrl)

// Использование клиента для отправки запросов к API
val response = mlpApiClient.invokeServiceEndpoint(requestData)

// Обработка ответа
if (response.isSuccessful) {
    val responseBody = response.body()
    // Обработка тела ответа
} else {
    // Обработка ошибки
}
```
