# Релиз и публикация в RuStore

Единый гайд: что лежит в этой папке, как выпустить новую версию и как опубликовать её в RuStore.

## Содержимое папки

- **CHANGELOG_TEMPLATE.md** — шаблон для описания изменений новой версии.
- **vX.Y.Z/** — материалы конкретной версии:
  - **CHANGELOG.md** — что нового. Читается GitHub Actions для описания GitHub Release и публикации в RuStore.
  - **short_description.txt** — краткое описание (до 80 символов), для карточки в магазине.
  - **full_description.txt** — полное описание возможностей.
  - **icon.png** — иконка 512×512px (до 1 МБ), генерируется `prepare_app_icons.py`.
  - **screenshots/** — скриншоты для магазина (опционально).

> Описания и иконка хранятся внутри версионной папки, чтобы каждый релиз был самодостаточным. При новой версии скопируйте файлы из предыдущей и обновите.

## ⚠️ Предусловие: keystore

Любая release-сборка подписывается production keystore. Подробности и резервное копирование — [keystores/README.md](../keystores/README.md).

- **Локально**: `keystore.properties` должен лежать в корне проекта (gitignored).
- **В CI**: настроены секреты `KEYSTORE_BASE64` и `KEYSTORE_PROPERTIES`.

Без keystore release-сборка падает с понятным сообщением — это специально, чтобы не выпустить APK с debug-ключом и не сломать обновления в RuStore.

## Быстрый выпуск (Makefile + GitHub Actions)

```bash
# 1. Сгенерировать новую иконку (опционально)
make icon PROMPT="Ваше описание иконки"
make copy-icon

# 2. Увеличить версию
make bump-version

# 3. Подготовить материалы версии
mkdir -p for_release/v0.0.X
cp for_release/v0.0.1/short_description.txt for_release/v0.0.X/
cp for_release/v0.0.1/full_description.txt  for_release/v0.0.X/
nano for_release/v0.0.X/CHANGELOG.md   # используйте CHANGELOG_TEMPLATE.md как шаблон
# при необходимости обновите short/full description под новые фичи

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

### Шаг 1: Подготовка иконки приложения

Генерация (если не подходит старая):
```bash
cd icon-generator
uv run main.py generate -p "Ваш промпт для иконки"
```

Конвертация в нужные размеры:
```bash
# Из директории icon-generator
uv run prepare_app_icons.py output/TIMESTAMP_generated_icon.png
```

Создаёт иконки во всех плотностях: mdpi 48, hdpi 72, xhdpi 96, xxhdpi 144, xxxhdpi 192.

Проверка:
```bash
cd ..
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew clean assembleRelease
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/release/app-release.apk
```

### Шаг 2: Обновление версии

В `app/build.gradle.kts`:
```kotlin
defaultConfig {
    versionCode = 1        // Увеличьте на 1 при каждом релизе
    versionName = "0.0.1"  // Semantic versioning
}
```

`make bump-version` делает это автоматически (инкремент patch). Для minor/major (например `0.1.0`) правьте вручную.

### Шаг 3: Сборка Release APK

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# keystore.properties должен лежать в корне проекта, иначе сборка упадёт
ls keystore.properties || echo "❌ Создайте keystore.properties по инструкции в keystores/README.md"

./gradlew clean
./gradlew assembleRelease
# Результат: app/build/outputs/apk/release/app-release.apk
```

### Шаг 4: Проверка APK

```bash
# Установка на устройство
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/release/app-release.apk

# Информация о версии
~/Library/Android/sdk/build-tools/*/aapt dump badging app/build/outputs/apk/release/app-release.apk | grep version
```

Проверьте, что обновление ставится поверх предыдущей версии (а не требует переустановки).

### Шаг 5: Публикация в RuStore

**Автоматически (рекомендуется):** Actions → Publish to RuStore → Run workflow. Параметры:
- **Version** — версия (например, `0.1.0`)
- **APK path** — по умолчанию `for_release/v{VERSION}/PartyInPocket-v{VERSION}.apk`
- **Submit for review** — отправить на модерацию сразу (по умолчанию да)
- **Priority update** — приоритет обновления 0–5

Workflow создаёт черновик версии, загружает APK, читает changelog из `for_release/v{VERSION}/CHANGELOG.md` и отправляет на модерацию. Требует секрет `RUSTORE_KEY` в Settings → Secrets → Actions.

**Вручную:**
```bash
cp app/build/outputs/apk/release/app-release.apk PartyInPocket-v0.0.1.apk
# Загрузите APK, иконку (icon.png) и описания (short/full) в RuStore через консоль разработчика
```

## Скриншоты

Для публикации нужны скриншоты приложения:
- Минимум 2 скриншота
- Рекомендуемый размер: 1080×1920 или 1080×2340
- Формат: PNG или JPG

Складывайте в `for_release/vX.Y.Z/screenshots/`.

## Troubleshooting

**"Failed to find Build Tools"**
```bash
~/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager "build-tools;34.0.0"
```

**"JAVA_HOME is not set"**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

## Checklist перед релизом

- [ ] `keystore.properties` на месте (локально) или секреты `KEYSTORE_*` настроены (в CI)
- [ ] `make test` зелёный
- [ ] Новая иконка установлена и проверена (если меняли)
- [ ] Версия обновлена (`versionCode` и `versionName`)
- [ ] `for_release/vX.Y.Z/CHANGELOG.md` готов
- [ ] `short_description.txt` (≤80 символов) и `full_description.txt` обновлены под новые фичи
- [ ] APK собран и протестирован на устройстве (обновление поверх предыдущей версии проходит)
- [ ] Готов к загрузке в RuStore (через `rustore-publish.yml` или вручную)
