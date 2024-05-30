### Описание класса `TaskExecutor`

`TaskExecutor` представляет собой исполнитель задач, который обрабатывает запросы к сервису MLP.

#### Поля класса `TaskExecutor`

- **action**: Объект, реализующий интерфейс `MlpService`, который выполняет основную логику обработки запросов.
- **config**: Конфигурация сервиса MLP, используемая для настройки исполнителя задач.
- **context**: Контекст выполнения, включающий информацию о среде исполнения.

#### Методы класса `TaskExecutor`

- **predict(request: PredictRequestProto, requestId: Long, connectorId: Long, tracker: TimeTracker)**: Обрабатывает запрос на предсказание.
- **fit(request: FitRequestProto, requestId: Long, connectorId: Long)**: Обрабатывает запрос на обучение модели.
- **ext(request: ExtendedRequestProto, requestId: Long, connectorId: Long)**: Обрабатывает расширенный запрос.
- **batch(request: BatchRequestProto, requestId: Long, connectorId: Long)**: Обрабатывает пакетный запрос.
- **enableNewTasks(id: Long)**: Включает обработку новых задач для коннектора с указанным идентификатором.
- **cancelAll()**: Отменяет все активные задачи.
- **cancelAll(connectorId: Long)**: Отменяет все задачи конкретного коннектора.
- **gracefulShutdownAll(connectorId: Long)**: Осуществляет плавное завершение всех задач конкретного коннектора.

#### Вспомогательные методы класса `TaskExecutor`

- **launchAndStore(requestId: Long, connectorId: Long, block: suspend () -> Unit)**: Запускает задачу и сохраняет ее в контейнере задач.
- **toString()**: Возвращает строковое представление объекта.


