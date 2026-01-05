# Keystores для подписи APK

Эта папка содержит keystore файлы для подписи release версий приложения.

## Файлы

- `debug-backup.keystore` - Копия debug keystore (используется для версий 0.0.1, 0.0.2, 0.0.3)
- `partyinpocket-release.keystore` - Production keystore (для версий начиная с 0.0.4)

## ⚠️ ВАЖНО

- **НЕ УДАЛЯЙТЕ** эти файлы - без них невозможно будет выпускать обновления приложения
- **НЕ КОММИТЬТЕ** keystore файлы в Git (они в `.gitignore`)
- **ХРАНИТЕ РЕЗЕРВНЫЕ КОПИИ** в безопасном месте (например, в облаке или на внешнем диске)
- **ХРАНИТЕ ПАРОЛИ** в надежном месте (password manager или файл `keystore.properties` в безопасном хранилище)

## Локальное использование

Проект настроен на автоматическое использование production keystore:

1. Файл `keystore.properties` в корне проекта содержит пароли и путь к keystore
2. При сборке release APK автоматически используется production keystore из этого файла
3. Если `keystore.properties` отсутствует, используется debug keystore (для локальной разработки)

Формат `keystore.properties`:
```properties
storePassword=ВАШ_ПАРОЛЬ
keyPassword=ВАШ_ПАРОЛЬ
keyAlias=partyinpocket
storeFile=../keystores/partyinpocket-release.keystore
```

## GitHub Actions

Чтобы GitHub Actions могли автоматически подписывать APK:

### 1. Добавить `KEYSTORE_BASE64`

Выполните команду (она автоматически скопирует результат в буфер обмена):
```bash
base64 -i keystores/partyinpocket-release.keystore | pbcopy
```

Откройте `Settings` → `Secrets and variables` → `Actions` → `New repository secret`

Создайте секрет:
- **Name:** `KEYSTORE_BASE64`
- **Value:** вставьте из буфера обмена (Cmd+V)

### 2. Добавить `KEYSTORE_PROPERTIES`

Выполните команду (она автоматически скопирует содержимое файла в буфер обмена):
```bash
cat keystore.properties | pbcopy
```

Создайте второй секрет:
- **Name:** `KEYSTORE_PROPERTIES`
- **Value:** вставьте из буфера обмена (Cmd+V)

### Итого: 2 секрета

| Секрет | Что содержит |
|--------|--------------|
| `KEYSTORE_BASE64` | Бинарный файл keystore в base64 |
| `KEYSTORE_PROPERTIES` | Текстовый файл с паролями |

### Workflows уже настроены

`.github/workflows/release.yml` и `.github/workflows/build.yml` уже настроены на использование этих секретов.

## История версий

- **0.0.1 - 0.0.3**: Подписаны с `debug-backup.keystore`
  - Это обеспечивает совместимость обновлений между этими версиями
- **0.0.4+**: Подписаны с `partyinpocket-release.keystore`
  - Пользователям пришлось переустановить приложение для обновления с 0.0.3 на 0.0.4
  - Начиная с 0.0.4 все обновления работают корректно

## Создание резервной копии

Сделайте копию этих файлов в безопасное место:

```bash
# Скопируйте всю папку keystores
cp -r keystores ~/Backups/PartyInPocket-keystores-$(date +%Y%m%d)

# Также сохраните keystore.properties
cp keystore.properties ~/Backups/PartyInPocket-keystores-$(date +%Y%m%d)/
```

## Восстановление

Если вы потеряли keystore:

1. Восстановите файлы из резервной копии в папку `keystores/`
2. Восстановите `keystore.properties` в корень проекта
3. Проверьте сборку: `make build-release`
