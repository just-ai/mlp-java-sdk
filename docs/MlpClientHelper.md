# MlpClientHelper

`MlpClientHelper` - это интерфейс, который предоставляет методы для работы с MLP SDK, облегчая взаимодействие с клиентской частью приложения.

## Методы

- **ensureDataset(myAccountId: String, name: String, content: String, type: String): Long**

  Гарантирует наличие набора данных с указанными параметрами.

    - `myAccountId`: Идентификатор учетной записи.
    - `name`: Название набора данных.
    - `content`: Содержимое набора данных.
    - `type`: Тип данных.

- **ensureDerivedModel(myAccountId: String, modelName: String, baseModelAccountId: String, baseModelId: String): ModelInfoPK**

  Гарантирует наличие производной модели с указанными параметрами.

    - `myAccountId`: Идентификатор учетной записи.
    - `modelName`: Название производной модели.
    - `baseModelAccountId`: Идентификатор учетной записи базовой модели.
    - `baseModelId`: Идентификатор базовой модели.

- **waitForJobDone(initialJobStatus: JobStatusData)**

  Ожидает завершения выполнения задания.

    - `initialJobStatus`: Исходное состояние задания.

- **fit(model: ModelInfoPK, datasetId: Long)**

  Выполняет подгонку модели к набору данных.

    - `model`: Информация о модели.
    - `datasetId`: Идентификатор набора данных.

## Свойства

- `grpcClient`: Клиент SDK для взаимодействия с gRPC.
- `restClient`: Клиент SDK для взаимодействия с REST API.

## Использование

```kotlin
// Пример использования интерфейса MlpClientHelper
class MyMlpClientHelper : MlpClientHelper {
    // Реализация методов интерфейса
}

// Создание экземпляра MlpClientHelper
val helper = MyMlpClientHelper()

// Использование методов
helper.ensureDataset("myAccountId", "datasetName", "datasetContent", "dataType")
helper.ensureDerivedModel("myAccountId", "modelName", "baseModelAccountId", "baseModelId")
// и т.д.
```



