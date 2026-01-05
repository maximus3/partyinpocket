# Генератор иконки приложения

Скрипт для генерации иконки приложения "Вечеринка в кармане" с помощью OpenAI Image Generation API (DALL-E 3/2 или другие модели).

## Быстрый старт

### 1. Настройка
```bash
cp .env.example .env
# Отредактируйте .env и вставьте свой API ключ
```

### 2. Генерация новой иконки
```bash
# С промптом по умолчанию
uv run main.py

# С кастомным промптом (кавычки необязательны)
uv run main.py generate -p Ваше описание иконки

# Или с кавычками
uv run main.py generate -p "Ваше описание иконки"
```

### 3. Редактирование иконки
```bash
# Изменить существующую иконку (кавычки необязательны)
uv run main.py edit --images icon.png --prompt Что изменить
# Результат: output/20260105T123501_edit_icon.png

# С кавычками тоже работает
uv run main.py edit --images icon.png -p "Что изменить"

# Объединить несколько изображений
uv run main.py edit --images image1.png image2.png -p Объедини картинки
# Результат: output/20260105T123501_edit_image1.png
```

### 4. Результаты
- **Формат определяется автоматически** - скрипт сам распознает, вернул ли API URL или base64
- **URL (OpenAI)**: URL сохраняется в `output/icon_url.txt`, затем скачайте изображение
- **Base64 (AI Tunnel)**: Файл автоматически сохраняется с временной меткой:
  - Генерация: `output/20260105T123501_generated_icon.png`
  - Редактирование: `output/20260105T123501_edit_original_name.png`

---

## Требования

- Python 3.12+
- uv (установлен)
- OpenAI API ключ или AI Tunnel ключ

## Установка

Зависимости уже установлены через uv:

```bash
uv add openai python-dotenv pillow
```

## Подготовка иконок для Android приложения

После генерации иконки используйте скрипт `prepare_app_icons.py` для создания всех нужных размеров:

```bash
uv run prepare_app_icons.py output/20260105T123501_generated_icon.png
```

Это автоматически создаст иконки во всех размерах (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi) и сохранит их в `../app/src/main/res/mipmap-*`

## Подробное использование

### Получение API ключа

**OpenAI:** https://platform.openai.com/api-keys

**AI Tunnel (русский аналог):** https://aitunnel.ru/

### Настройка .env файла

```bash
cp .env.example .env
```

Отредактируйте `.env`:

**Для OpenAI:**
```bash
OPENAI_API_KEY=sk-your-openai-key
OPENAI_BASE_URL=https://api.openai.com/v1
IMAGE_MODEL=dall-e-3
IMAGE_SIZE=1024x1024
IMAGE_QUALITY=standard
```

**Для AI Tunnel:**
```bash
OPENAI_API_KEY=sk-aitunnel-xxx
OPENAI_BASE_URL=https://api.aitunnel.ru/v1/
IMAGE_MODEL=gpt-image-1
IMAGE_SIZE=1024x1536
IMAGE_QUALITY=medium
IMAGE_MODERATION=low
```

### Режимы работы

#### Режим 1: Генерация новой иконки

Сгенерировать с промптом по умолчанию:
```bash
uv run main.py generate
```

С кастомным промптом (кавычки необязательны):
```bash
uv run main.py generate --prompt Яркая иконка с шляпой и конфетти
```

Короткий вариант:
```bash
uv run main.py generate -p Ваш промпт
```

С кавычками тоже работает:
```bash
uv run main.py generate -p "Ваш промпт"
```

#### Режим 2: Редактирование существующей иконки

Отредактировать одно изображение (кавычки необязательны):
```bash
uv run main.py edit --images output/icon.png --prompt Добавь больше ярких цветов
# Результат: output/20260105T123501_edit_icon.png
```

Объединить несколько изображений:
```bash
uv run main.py edit --images image1.png image2.png --prompt Объедини эти картинки в одну
# Результат: output/20260105T123501_edit_image1.png
```

С кастомным именем выходного файла:
```bash
uv run main.py edit --images icon.png -p Сделай фон темнее -o dark_icon.png
# Результат: output/dark_icon.png
```

### Результаты работы

**При использовании OpenAI (возвращает URL):**
- URL изображения будет выведен в консоль
- URL сохранится в `output/icon_url.txt`
- Скачайте: `curl -o output/20260105T123501_generated_icon.png 'URL'`

**При использовании AI Tunnel (возвращает base64):**
- Изображение автоматически сохранится с временной меткой:
  - Генерация: `output/20260105T123501_generated_icon.png`
  - Редактирование: `output/20260105T123501_edit_original_name.png`

## Настройка

### Настройки генерации (через .env)

Все параметры настраиваются в файле `.env`:

```bash
# API Endpoint
OPENAI_BASE_URL=https://api.openai.com/v1    # или https://api.aitunnel.ru/v1/

# Модель
IMAGE_MODEL=dall-e-3                         # dall-e-3, dall-e-2, gpt-image-1, и т.д.

# Размер изображения
IMAGE_SIZE=1024x1024                         # зависит от модели

# Качество
IMAGE_QUALITY=standard                       # standard, hd, medium и т.д.

# Модерация (для AI Tunnel и подобных)
IMAGE_MODERATION=low                         # low, medium, high
```

**Доступные размеры:**
- DALL-E 3: `1024x1024`, `1792x1024`, `1024x1792`
- DALL-E 2: `256x256`, `512x512`, `1024x1024`
- AI Tunnel: `1024x1536`, `1536x1024`, `1024x1024`, и другие

### Настройка промпта

Промпт можно изменить в файле `main.py` в функции `generate_icon()`.

Текущий промпт генерирует:
- Черную шляпу цилиндр с фиолетовой лентой
- Разноцветное конфетти и искры
- Кости или карты вокруг
- Яркий градиентный фон (розовый, фиолетовый, оранжевый)
- Современный плоский дизайн

## Примеры команд

### Генерация
```bash
# Генерация с настройками по умолчанию
uv run main.py

# Или явно указать режим
uv run main.py generate

# С кастомным промптом (кавычки необязательны)
uv run main.py generate -p Веселая иконка с шляпой, конфетти и яркими цветами. Стиль минимализм
```

### Редактирование
```bash
# Изменить одну картинку (кавычки необязательны)
uv run main.py edit --images icon.png -p Сделай цвета ярче и насыщеннее
# Результат: output/20260105T123501_edit_icon.png

# Объединить две картинки
uv run main.py edit --images hat.png confetti.png -p Объедини шляпу и конфетти на одном изображении
# Результат: output/20260105T123501_edit_hat.png

# С кастомным именем выходного файла
uv run main.py edit --images icon.png -p Добавь эффект свечения -o glowing_icon.png
# Результат: output/glowing_icon.png
```

### Справка
```bash
# Общая справка
uv run main.py --help

# Справка по генерации
uv run main.py generate --help

# Справка по редактированию
uv run main.py edit --help
```

## Стоимость

**OpenAI:**
- DALL-E 3 Standard (1024x1024): ~$0.04 за изображение
- DALL-E 3 HD (1024x1024): ~$0.08 за изображение
- DALL-E 2 (1024x1024): ~$0.02 за изображение

**AI Tunnel:** Проверьте актуальные цены на https://aitunnel.ru/

## Примечания

- По умолчанию используется модель `dall-e-3`
- Качество по умолчанию: `standard` (для более высокого качества используйте `hd`)
- Размер по умолчанию: `1024x1024` (рекомендуется для иконок приложений)
- Все настройки легко изменяются через файл `.env`
- Поддерживается работа с любыми OpenAI-совместимыми API
- **Имена файлов включают временную метку** для автоматического версионирования:
  - Генерация: `20260105T123501_generated_icon.png`
  - Редактирование: `20260105T123501_edit_original_name.png`
  - Можно указать кастомное имя через `-o` параметр
- **Промпты без кавычек**: Можно писать промпты без кавычек - все слова после `--prompt` автоматически объединяются
  - `uv run main.py generate -p Ваш промпт без кавычек` работает
  - `uv run main.py generate -p "Ваш промпт"` тоже работает

### Совместимость с разными API

Скрипт автоматически адаптируется к разным API:
- **Формат ответа** определяется автоматически (URL или base64)
- **Base64 в поле URL** - некоторые API (например, Gemini через AI Tunnel) возвращают base64 в поле `url` вместо ссылки. Скрипт автоматически определяет это и декодирует изображение
- **Неподдерживаемые параметры** автоматически исключаются при ошибке
- **Fallback** к базовым параметрам, если расширенные не работают

Это значит, что скрипт будет работать с большинством OpenAI-совместимых API "из коробки".
