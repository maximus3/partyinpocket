# Инструкция по сборке APK для RuStore

## ⚠️ Предусловие: keystore

Любая release-сборка подписывается production keystore. Подробности и резервное копирование — [keystores/README.md](keystores/README.md).

- **Локально**: `keystore.properties` должен лежать в корне проекта (gitignored).
- **В CI**: настроены секреты `KEYSTORE_BASE64` и `KEYSTORE_PROPERTIES`.

Без keystore release-сборка падает с понятным сообщением — это специально, чтобы не выпустить APK с debug-ключом и не сломать обновления в RuStore.

## Быстрый способ (через Makefile + GitHub Actions)

```bash
# 1. Сгенерировать новую иконку (опционально)
make icon PROMPT="Ваше описание иконки"
make copy-icon

# 2. Увеличить версию
make bump-version

# 3. Создать changelog
mkdir -p for_release/v0.0.X
nano for_release/v0.0.X/CHANGELOG.md
# Используйте for_release/CHANGELOG_TEMPLATE.md как шаблон

# 4. Закоммитить и создать тег (app/src/main/res/ — только если меняли иконку)
git add app/build.gradle.kts for_release/v0.0.X/
git commit -m "Bump version to 0.0.X"
git tag v0.0.X
git push origin main
git push origin v0.0.X

# 5. GitHub Actions release.yml автоматически:
#    - Соберёт release и debug APK с production-подписью
#    - Прочитает changelog из for_release/v0.0.X/CHANGELOG.md
#    - Создаст GitHub Release с файлами и описанием

# 6. (опционально) Запустить .github/workflows/rustore-publish.yml
#    Actions → Publish to RuStore → Run workflow → указать version=0.0.X
#    Workflow сам скачает APK из GitHub Release и загрузит в RuStore.
```

## Ручная сборка (без GitHub Actions)

```bash
# 1. Сгенерировать новую иконку (опционально)
make icon PROMPT="Ваше описание иконки"

# 2. Скопировать иконку в приложение (если генерировали новую)
make copy-icon

# 3. Увеличить версию
make bump-version

# 4. Собрать release APK (требует keystore.properties локально)
make build-release

# 5. Переименовать и загрузить в RuStore Console вручную
cp app/build/outputs/apk/release/app-release.apk PartyInPocket-v$(grep versionName app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/').apk
```

## Подробная инструкция (вручную)

## Шаг 1: Подготовка иконки приложения

### 1.1 Генерация иконки (если не подходит старая)
```bash
cd icon-generator
uv run main.py generate -p "Ваш промпт для иконки"
```

### 1.2 Конвертация в нужные размеры (если сгенерировали новую)
```bash
# Из директории icon-generator
uv run prepare_app_icons.py output/TIMESTAMP_generated_icon.png
```

Это автоматически создаст иконки во всех нужных размерах:
- mdpi: 48x48
- hdpi: 72x72
- xhdpi: 96x96
- xxhdpi: 144x144
- xxxhdpi: 192x192

### 1.3 Проверка
```bash
cd ..
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew clean assembleRelease
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/release/app-release.apk
```

Проверьте, как выглядит новая иконка на устройстве.

## Шаг 2: Обновление версии

В `app/build.gradle.kts`:

```kotlin
defaultConfig {
    applicationId = "com.m3games.partyinpocket"
    minSdk = 24
    targetSdk = 35
    versionCode = 1  // Увеличьте на 1 при каждом релизе
    versionName = "0.0.1"  // Следуйте semantic versioning
    // ...
}
```

## Шаг 3: Сборка Release APK

```bash
# Установка Java
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Проверка: keystore.properties должен лежать в корне проекта.
# Иначе сборка упадёт на этапе assembleRelease.
ls keystore.properties || echo "❌ Создайте keystore.properties по инструкции в keystores/README.md"

# Очистка предыдущих сборок
./gradlew clean

# Сборка release APK (оптимизированная версия, подписана production keystore)
./gradlew assembleRelease

# APK будет в:
# app/build/outputs/apk/release/app-release.apk
```

## Шаг 4: Проверка APK

### 4.1 Установка на устройство
```bash
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/release/app-release.apk
```

### 4.2 Информация об APK
```bash
~/Library/Android/sdk/build-tools/*/aapt dump badging app/build/outputs/apk/release/app-release.apk | grep version
```

## Шаг 5: Публикация в RuStore

1. Переименуйте APK:
```bash
cp app/build/outputs/apk/release/app-release.apk \
   PartyInPocket-v0.0.1.apk
```

2. Загрузите в RuStore вручную через консоль разработчика

## Troubleshooting

### Ошибка: "Failed to find Build Tools"
```bash
# Установите build-tools
~/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager "build-tools;34.0.0"
```

### Ошибка: "JAVA_HOME is not set"
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

## Checklist перед релизом

- [ ] `keystore.properties` на месте (локально) или секреты `KEYSTORE_*` настроены (в CI)
- [ ] `make test` зелёный
- [ ] Новая иконка установлена и проверена (если меняли)
- [ ] Версия обновлена (`versionCode` и `versionName` через `make bump-version`)
- [ ] Changelog положен в `for_release/v0.0.X/CHANGELOG.md`
- [ ] APK собран и протестирован на устройстве (поверх установленной предыдущей версии — обновление проходит)
- [ ] Готов к загрузке в RuStore (через `rustore-publish.yml` или вручную)
