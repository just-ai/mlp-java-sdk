### Описание классов в SDK MLP

#### Класс `MlpServiceBase<F, FC, P, C, R>`

Абстрактный класс, предоставляющий базовую функциональность для сервисов MLP, включая методы `fit` и `predict`.

- **Параметры типов:**
    - `F`: тип данных для обучения.
    - `FC`: тип конфигурации обучения.
    - `P`: тип запроса для предсказания.
    - `C`: тип конфигурации для предсказания.
    - `R`: тип ответа от предсказания.

- **Поля:**
    - `fitDataExample`: пример данных для обучения.
    - `fitConfigExample`: пример конфигурации обучения.
    - `predictRequestExample`: пример запроса для предсказания.
    - `predictConfigExample`: пример конфигурации для предсказания.
    - `predictResponseExample`: пример ответа от предсказания.

- **Методы:**
    - `getDescriptor()`: возвращает описание сервиса.
    - `fit()`: выполняет обучение модели.
    - `predict()`: выполняет предсказание.

#### Абстрактный класс `MlpFitServiceBase<F, FC>`

Предоставляет базовую функциональность для сервисов MLP, специализированных на обучении моделей.

- **Параметры типов:**
    - `F`: тип данных для обучения.
    - `FC`: тип конфигурации обучения.

#### Абстрактный класс `MlpPredictServiceBase<P, R>`

Предоставляет базовую функциональность для сервисов MLP, специализированных на выполнении предсказаний без конфигурации.

- **Параметры типов:**
    - `P`: тип запроса для предсказания.
    - `R`: тип ответа от предсказания.

#### Абстрактный класс `MlpPredictWithConfigServiceBase<P, C, R>`

Предоставляет базовую функциональность для сервисов MLP, специализированных на выполнении предсказаний с конфигурацией.

- **Параметры типов:**
    - `P`: тип запроса для предсказания.
    - `C`: тип конфигурации для предсказания.
    - `R`: тип ответа от предсказания.

#### Класс `MlpRestClient`

Клиент для взаимодействия с REST API сервиса MLP.

- **Поля:**
    - `restUrl`: URL-адрес REST API сервиса MLP.
    - `clientToken`: токен клиента для аутентификации.
    - `billingToken`: токен для биллинга.
    - `context`: контекст выполнения MLP.

- **Методы:**
    - `ACCOUNT_ID`: возвращает идентификатор аккаунта (устаревший).
    - `MODEL_ID`: возвращает идентификатор модели (устаревший).

#### Класс `MlpExecutionContext`

Хранит настройки для конкретного экземпляра SDK MLP.

- **Поля:**
    - `environment`: доступ к переменным среды.
    - `loggerFactory`: фабрика логгеров.

- **Сопутствующий объект:**
    - `systemContext`: контекст выполнения системы по умолчанию.

#### Класс `Environment`

Предоставляет доступ к переменным среды.

- **Поля:**
    - `envsOverride`: переопределение переменных среды.

- **Методы:**
    - `get(name: String)`: возвращает значение переменной среды по имени.
    - `getOrThrow(name: String)`: возвращает значение переменной среды по имени или выбрасывает исключение, если переменная не найдена.

### Использование

Пример использования SDK MLP:

```kotlin
// Создание клиента MLP REST API
val restClient = MlpRestClient()

// Создание сервиса для обучения моделей
val fitService = object : MlpFitServiceBase<Data, Config>(fitDataExample, fitConfigExample) {
    override fun fit(data: Data, config: Config?, modelDir: String, previousModelDir: String?, targetServiceInfo: ServiceInfoProto, dataset: DatasetInfoProto) {
        // Реализация метода обучения
    }
}

// Создание сервиса для выполнения предсказаний без конфигурации
val predictService = object : MlpPredictServiceBase<Request, Response>(predictRequestExample, predictResponseExample) {
    override fun predict(request: Request): Response {
        // Реализация метода предсказания
    }
}

// Создание сервиса для выполнения предсказаний с конфигурацией
val predictWithConfigService = object : MlpPredictWithConfigServiceBase<Request, Config, Response>(predictRequestExample, predictConfigExample, predictResponseExample) {
    override fun predict(request: Request, config: Config?): Response {
        // Реализация метода предсказания с конфигурацией
    }
}
```

