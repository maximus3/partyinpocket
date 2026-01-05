# Файлы для публикации в RuStore

Эта папка содержит все необходимые материалы для публикации приложения в магазинах.

## Содержимое

- **icon.png** - Иконка приложения 512x512px (до 1 МБ)
  - Автоматически генерируется скриптом `prepare_app_icons.py`

- **short_description.txt** - Краткое описание (до 80 символов)
  - Используется в карточке приложения в магазине

- **full_description.txt** - Полное описание
  - Подробное описание возможностей приложения
  - Копируется в раздел "Описание" при публикации

- **vX.Y.Z/** - Папки с версиями
  - **CHANGELOG.md** - Что нового в этой версии
  - Используется GitHub Actions для описания релиза

## Использование

При подготовке новой версии:

1. Сгенерируйте новую иконку (если нужно):
   ```bash
   make icon PROMPT="Описание иконки"
   make copy-icon  # Автоматически создаст icon.png
   ```

2. Создайте changelog для версии:
   ```bash
   mkdir -p for_release/v0.0.X
   nano for_release/v0.0.X/CHANGELOG.md
   # Опишите изменения в этой версии
   ```

3. Соберите release APK:
   ```bash
   make bump-version
   make build-release
   ```

4. Создайте тег и запушьте:
   ```bash
   git add app/build.gradle.kts
   git commit -m "Bump version to 0.0.X"
   git tag v0.0.X
   git push origin main
   git push origin v0.0.X
   ```

5. GitHub Actions автоматически создаст Release с:
   - APK файлами (release и debug)
   - Описанием из `for_release/vX.Y.Z/CHANGELOG.md`

6. Скачайте APK из GitHub Releases и загрузите в RuStore:
   - APK из GitHub Releases
   - Иконку из `for_release/icon.png`
   - Описания из txt файлов

## Скриншоты

Для публикации также понадобятся скриншоты приложения:
- Минимум 2 скриншота
- Рекомендуемый размер: 1080x1920 или 1080x2340
- Формат: PNG или JPG

TODO: Добавить скриншоты в эту папку
