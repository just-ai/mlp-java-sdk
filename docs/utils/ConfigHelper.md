### Описание объекта `ConfigHelper`

Объект `ConfigHelper` предоставляет удобные методы для загрузки конфигурационных параметров из различных источников, таких как файлы свойств, ресурсы, системные переменные окружения и системные свойства.

#### Методы объекта `ConfigHelper`

- **loadProperties(configPath: String? = null): Map<String, String>**

  Загружает свойства из конфигурационных файлов и других источников, используя системные переменные окружения по умолчанию.

- **loadProperties(configPath: String? = null, environment: Environment): Map<String, String>**

  Загружает свойства из конфигурационных файлов и других источников, принимая во внимание указанный объект `Environment`.

#### Дополнительные методы

- **loadFromPropsFile(file: String?): Map<String, String>**

  Загружает свойства из указанного файла свойств.

- **loadFromResource(resource: String): Map<String, String>**

  Загружает свойства из ресурса.

- **loadFromSystemProps(): Map<String, String>**

  Загружает свойства из системных свойств.

- **loadFromEnv(): Map<String, String>**

  Загружает свойства из системных переменных окружения.

### Использование

```kotlin
// Загрузка свойств из всех доступных источников с использованием системных переменных окружения по умолчанию
val properties = ConfigHelper.loadProperties()

// Загрузка свойств, учитывая указанный объект Environment
val environment = Environment()
val propertiesWithEnvironment = ConfigHelper.loadProperties(environment = environment)
```


