### Описание объекта `JSON`

Объект `JSON` использует библиотеку Jackson для работы с JSON-данными. Он предоставляет удобный и гибкий интерфейс для обработки JSON-структур в приложениях, позволяя легко выполнять операции парсинга, сериализации и манипулирования данными в формате JSON.

#### Методы

- **parse(json: String): JsonNode**: Парсит JSON-строку в объект типа `JsonNode`.

- **parseObject(json: String): ObjectNode**: Парсит JSON-строку в объект типа `ObjectNode`.

- **parseToMap(json: String): Map<String, String>**: Парсит JSON-строку в карту ключ-значение.

- **anyToObject(data: Any): ObjectNode**: Преобразует объект в `ObjectNode`.

- **parse(json: String): T**: Парсит JSON-строку в объект типа `T`.

- **parseList(json: String): List<T>**: Парсит JSON-строку в список объектов типа `T`.

- **parse(json: String, clazz: Class<T>): T**: Парсит JSON-строку в объект типа `T`.

- **parse(json: String, tr: TypeReference<T>): T**: Парсит JSON-строку в объект типа `T`, используя объект `TypeReference`.

- **parse(json: JsonNode): T**: Парсит `JsonNode` в объект типа `T`.

- **stringify(data: T): String**: Сериализует объект в JSON-строку.

- **toNode(data: Any): JsonNode**: Преобразует объект в `JsonNode`.

- **toObject(data: Any): ObjectNode**: Преобразует объект в `ObjectNode`.

- **objectNode(): ObjectNode**: Создает новый пустой `ObjectNode`.

- **escapeText(text: String): String**: Экранирует специальные символы в тексте JSON-строки.

- **any.asJson: String**: Расширение для любого объекта, возвращающее JSON-представление объекта в виде строки.


