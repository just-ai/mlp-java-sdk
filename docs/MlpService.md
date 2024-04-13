### Описание класса MlpService

Класс `MlpService` представляет собой абстрактный сервис MLP (Machine Learning Platform), предоставляющий функционал для работы с моделями машинного обучения.

#### Поля класса

- **context**: Объект класса `MlpExecutionContext`, предоставляющий контекст выполнения для сервиса.

#### Методы

1. **batch()**

```kotlin
public open fun batch(requests: List<Payload>, config: Payload?): List<MlpResponse>
```

Выполняет пакетную обработку запросов на выполнение моделей машинного обучения.

    - `requests`: Список объектов класса `Payload`, содержащих данные для предсказания.
    - `config`: Необязательный параметр, объект класса `Payload`, содержащий конфигурацию запроса.
    
    Возвращает список объектов класса `MlpResponse`, содержащих результаты выполнения запросов.

2. **ext()**

```kotlin
public open fun ext(methodName: String, params: Map<String, Payload>): MlpResponse
```

Выполняет расширенный метод сервиса MLP.

    - `methodName`: Имя метода, который требуется выполнить.
    - `params`: Карта параметров запроса, где ключ - имя параметра, значение - объект класса `Payload`.
    
    Возвращает объект класса `MlpResponse`, содержащий результат выполнения расширенного метода.

3. **fit()**

```kotlin
public open fun fit(train: Payload, targets: Payload?, config: Payload?, modelDir: String, previousModelDir: String?, targetServiceInfo: ServiceInfoProto, dataset: DatasetInfoProto): MlpResponse
```

Производит обучение модели машинного обучения.

    - `train`: Объект класса `Payload`, содержащий данные для обучения модели.
    - `targets`: Необязательный параметр, объект класса `Payload`, содержащий целевые значения.
    - `config`: Необязательный параметр, объект класса `Payload`, содержащий конфигурацию запроса.
    - `modelDir`: Директория для сохранения обученной модели.
    - `previousModelDir`: Необязательный параметр, предыдущая директория с моделью.
    - `targetServiceInfo`: Объект класса `ServiceInfoProto`, содержащий информацию о целевом сервисе.
    - `dataset`: Объект класса `DatasetInfoProto`, содержащий информацию о датасете.
    
    Возвращает объект класса `MlpResponse`, содержащий результаты обучения модели.

4. **getDescriptor()**

```kotlin
public open fun getDescriptor(): ServiceDescriptorProto
```

Получает дескриптор сервиса.

    Возвращает объект класса `ServiceDescriptorProto`, содержащий описание сервиса.

5. **predict()**

```kotlin
public open fun predict(req: Payload): MlpResponse
```

Выполняет предсказание с использованием модели машинного обучения.

    - `req`: Объект класса `Payload`, содержащий данные для предсказания.
    
    Возвращает объект класса `MlpResponse`, содержащий результат предсказания.

6. **predict()**

```kotlin
public open fun predict(req: Payload, config: Payload?): MlpResponse
```

Выполняет предсказание с использованием модели машинного обучения с указанием конфигурации.

    - `req`: Объект класса `Payload`, содержащий данные для предсказания.
    - `config`: Необязательный параметр, объект класса `Payload`, содержащий конфигурацию запроса.
    
    Возвращает объект класса `MlpResponse`, содержащий результат предсказания.

Это основные методы класса `MlpService`, предоставляющие функциональность для работы с моделями машинного обучения в рамках сервиса MLP.