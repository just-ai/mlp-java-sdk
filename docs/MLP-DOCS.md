## Документация MLP Java SDK

### 1. [Запуск приложения](#запуск-приложения)
### 2. [Общие классы]()

- [Connector](Connector.md)
- [ConnectorPool](ConnectorPool.md)
- [MlpApiClient](MlpApiClient.md)
- [MlpClientConfig](MlpClientConfig.md)
- [MlpClientException](MlpClientException.md)
- [MlpClientHelper](MlpClientHelper.md)
- [MlpClientSDK](MlpClientSDK.md)
- [MlpExecutionContext](MlpExecutionContext.md)
- [MlpService](MlpService.md)
- [MlpServiceBase](MlpServiceBase.md)
- [MlpServiceConfig](MlpServiceConfig.md)
- [MlpServiceSDK](MlpServiceSDK.md)
- [MlpService vs MlpServiceSDK](MlpService%20vs%20MlpServiceSDK.md)
- [Payload](Payload.md)
- [State](State.md)
- [TaskExecutor](TaskExecutor.md)
- [TimeTracker](TimeTracker.md)

## 3. [Utils](utils)

- [ConfigHelper](utils/ConfigHelper.md)
- [JSON](utils/JSON.md)
- [JobsContainer](utils/JobsContainer.md)

## 4. [Storage](storage)

- [LocalStorage](storage/LocalStorage.md)
- [S3Storage](storage/S3Storage.md)
- [Storage](storage/Storage.md)
- [StorageFactory](storage/StorageFactory.md)

## 5. [Data Types](datatypes)

### [AIProxy](datatypes/aiproxy)

1. [AIProxy](datatypes/aiproxy/AIProxy.md)
2. [Audio](datatypes/aiproxy/Audio.md)
3. [Completion](datatypes/aiproxy/Completion.md)
4. [Edit](aiproxy/Edit.md)
5. [Embeddings](aiproxy/Embeddings.md)
6. [Image](aiproxy/Image.md)
7. [ModelType](aiproxy/ModelType.md)
8. [ModerationRequest](aiproxy/ModerationRequest.md)
9. [Usage](aiproxy/Usage.md)

### [ASR (Automatic Speech Recognition)](datatypes/asr)

- [Kaldi](datatypes/asr/Kaldi.md)

### [Chat](datatypes/chat)

- [Chat](datatypes/chat/Chat.md)

### [ChatGPT](datatypes/chatgtp)

- [GtpLike](datatypes/chatgtp/GtpLike.md)

### [Datasets](datatypes/datasets)

- [TextsAndLabels](datatypes/datasets/TextsAndLabels.md)

### [JAICP Patterns](datatypes/jaicp_patterns)

- [PatternData](datatypes/jaicp_patterns/PatternData.md)

### [Llama](datatypes/llama)

- [Llama](datatypes/llama/Llama.md)

### [TaskZoo](datatypes/taskzoo)

- [TaskZoo](datatypes/taskzoo/TaskZoo.md)

### [TTS (Text-to-Speech)](datatypes/tts)

- [Aimvoice](datatypes/tts/Aimvoice.md)


## Запуск приложения 

### 1. Содержание функции main

```kotlin
fun main() {
    // Создается экземпляр MlpServiceSDK с переданным в конструкторе экземпляром ML сервиса
    val actionSDK = MlpServiceSDK({ YourMlpService(MlpExecutionContext.systemContext) })

    // SDK запускается при помощи метода start
    actionSDK.start()

    // Метод blockUntilShutdown() ожидает, пока сервис, запущенный с помощью actionSDK.start(), не завершится или не будет остановлен
    // Это обычно используется для того, чтобы гарантировать, что приложение не завершится до того, как сервис завершит свою работу или не будет остановлен вручную
    actionSDK.blockUntilShutdown()
}
```

### 2. Для работы с MlpServiceSDK необходимы аргументы
- Объект класса MlpService
- Опционально MlpServiceConfig

### 3. Реализация MlpService
Чтобы реализовать MlpService, необходимо создать его подкласс. В приведенном ниже примере показано, как это можно сделать:

```kotlin
class YourMlpService : MlpService() {
    // Здесь мы наследуемся от MlpService
    // Вы можете переопределить методы, которые вы хотите, чтобы они использовались в вашем MlpService
}
```

### 4. Инициализация контекста окружения

Также в подклассе мы определяем контекст окружения (параметры нашей среды разработки). Делается это через создание контекста внутри класса, который наследуется от MlpService
```
override val context: MlpExecutionContext = MlpExecutionContext.systemContext // Пример инициализации контекста
```
Здесь мы инициализируем контекст в виде объекта класса ```MlpExecutionContext```. Этот объект инициализируется с помощью systemContext, который, предварительно настроен и содержит системные параметры и конфигурации, необходимые для выполнения операций в рамках вашего приложения.

Например, переменные ```MLP_ACCOUNT_ID, MLP_MODEL_ID, MLP_INSTANCE_ID```, и другие, являются переменными окружения, которые предоставляют информацию о нашем аккаунте, модели, идентификаторе экземпляра и т. д.
Приложение может использовать эти параметры как часть контекста выполнения для получения доступа к этой информации в различных частях кода без явного указания этих значений в коде. Таким образом, ```MlpExecutionContext``` может быть инициализирован с использованием этих переменных окружения, предоставляя доступ к общим данным приложения в удобной и единообразной форме.


