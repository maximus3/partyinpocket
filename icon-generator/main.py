#!/usr/bin/env python3
"""
Генератор иконки для приложения "Вечеринка в кармане"
Использует OpenAI Image Generation API для создания изображения
"""

import argparse
import base64
import os
import sys
from datetime import datetime
from pathlib import Path

from dotenv import load_dotenv
from openai import OpenAI

# Загрузка переменных окружения из .env файла
load_dotenv()


def get_client():
    """Создает OpenAI клиента с настройками из .env"""
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        print("❌ Ошибка: Установите переменную окружения OPENAI_API_KEY")
        print("   export OPENAI_API_KEY='your-api-key-here'")
        sys.exit(1)

    base_url = os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1")

    return OpenAI(api_key=api_key, base_url=base_url)


def get_settings():
    """Получает настройки генерации из переменных окружения"""
    return {
        "model": os.getenv("IMAGE_MODEL", "dall-e-3"),
        "size": os.getenv("IMAGE_SIZE", "1024x1024"),
        "quality": os.getenv("IMAGE_QUALITY", "standard"),
        "moderation": os.getenv("IMAGE_MODERATION", "low"),
    }


def save_image(image_data, filename="icon.png"):
    """Сохраняет изображение из ответа API"""
    output_dir = Path(__file__).parent / "output"
    output_dir.mkdir(exist_ok=True)
    output_path = output_dir / filename

    # Автоматическое определение формата ответа
    has_b64_json = hasattr(image_data, "b64_json") and image_data.b64_json is not None
    has_url = hasattr(image_data, "url") and image_data.url is not None

    if has_b64_json:
        # Декодируем base64 и сохраняем как PNG
        image_base64 = image_data.b64_json
        image_bytes = base64.b64decode(image_base64)
        with open(output_path, "wb") as f:
            f.write(image_bytes)
        print(f"💾 Изображение сохранено: {output_path}")
        return str(output_path)
    elif has_url:
        image_url = image_data.url

        # Проверяем, является ли это настоящий HTTP(S) URL
        if image_url.startswith(("http://", "https://")):
            # Это настоящий URL - сохраняем в текстовый файл
            url_file = output_dir / "icon_url.txt"
            with open(url_file, "w") as f:
                f.write(image_url)
            print(f"🔗 URL: {image_url}")
            print(f"💾 URL сохранен в: {url_file}")
            print(f"\n📥 Скачайте изображение по ссылке или используйте:")
            print(f"   curl -o {output_path} '{image_url}'")
            return image_url

        # Иначе это base64 (с префиксом data: или без)
        try:
            # Если это data URI (data:image/png;base64,XXXXX), извлекаем base64 часть
            if image_url.startswith("data:"):
                # Находим base64 часть после "base64,"
                base64_start = image_url.find("base64,")
                if base64_start != -1:
                    image_base64 = image_url[base64_start + 7:]  # 7 = len("base64,")
                else:
                    # Нет "base64," в строке, пробуем декодировать всю строку после data:
                    image_base64 = image_url.split(",", 1)[1] if "," in image_url else image_url
            else:
                # Это чистый base64 без префикса
                image_base64 = image_url

            image_bytes = base64.b64decode(image_base64)
            with open(output_path, "wb") as f:
                f.write(image_bytes)
            print(f"💾 Изображение сохранено: {output_path}")
            return str(output_path)
        except Exception as e:
            print(f"❌ Ошибка декодирования base64: {e}")
            # Если не получилось декодировать, сохраним как текст для отладки
            error_file = output_dir / "error_response.txt"
            with open(error_file, "w") as f:
                f.write(image_url[:1000])  # Первые 1000 символов для отладки
            print(f"💾 Ответ сохранен для отладки: {error_file}")
            sys.exit(1)
    else:
        # Ошибка: неизвестный формат
        print(f"❌ Ошибка: API вернул неизвестный формат ответа")
        print(f"   Ожидалось: b64_json или url")
        print(f"   Доступные атрибуты: {dir(image_data)}")
        sys.exit(1)


def generate_icon(prompt=None, custom_settings=None):
    """Генерирует новую иконку приложения"""
    client = get_client()
    settings = get_settings()

    if custom_settings:
        settings.update(custom_settings)

    # Промпт по умолчанию для иконки приложения
    if not prompt:
        prompt = """
        A vibrant and playful mobile app icon for a party games app.
        Central element is a festive magician's top hat with colorful confetti
        and sparkles bursting out from it. The hat should be black with a purple band.
        Around it, add small playful elements like dice or cards floating in the air.
        Bright gradient background with warm party colors (pink, purple, orange).
        Modern flat design style with slight 3D effect and soft shadows.
        The overall mood is fun, energetic, and friendly.
        Square icon format with rounded corners, suitable for mobile app stores.
        Clean, minimalist composition that looks good at small sizes.
        IMPORTANT: The icon should fill the entire frame edge-to-edge with no white borders or margins.
        The background gradient should extend to all edges of the image.
        """

    print("🎨 Генерация новой иконки...")
    print(f"⚙️  Настройки:")
    print(f"   • API: {os.getenv('OPENAI_BASE_URL', 'https://api.openai.com/v1')}")
    print(f"   • Модель: {settings['model']}")
    print(f"   • Размер: {settings['size']}")
    print(f"   • Качество: {settings['quality']}")
    print(f"   • Модерация: {settings['moderation']}")
    print(f"\n📝 Промпт:\n{prompt.strip()}\n")

    try:
        # Создание параметров запроса
        request_params = {
            "model": settings["model"],
            "prompt": prompt,
            "size": settings["size"],
            "n": 1,
        }

        # Добавляем дополнительные параметры если они поддерживаются
        if settings.get("quality") and settings["quality"] not in ["", "None"]:
            request_params["quality"] = settings["quality"]
        if settings.get("moderation") and settings["moderation"] not in ["", "None"]:
            request_params["moderation"] = settings["moderation"]

        # Генерация изображения
        try:
            response = client.images.generate(**request_params)
        except Exception as api_error:
            # Если параметры не поддерживаются, пробуем без них
            print(f"⚠️  Предупреждение: {api_error}")
            print(f"🔄 Повторная попытка с базовыми параметрами...")
            request_params = {
                "model": settings["model"],
                "prompt": prompt,
                "size": settings["size"],
                "n": 1,
            }
            response = client.images.generate(**request_params)

        print(f"✅ Изображение сгенерировано!")

        # Сохранение с временной меткой
        timestamp = datetime.now().strftime("%Y%m%dT%H%M%S")
        filename = f"{timestamp}_generated_icon.png"
        result = save_image(response.data[0], filename)

        return result

    except Exception as e:
        print(f"❌ Ошибка при генерации: {e}")
        sys.exit(1)


def edit_icon(input_images, prompt, output_filename=None):
    """Редактирует существующие изображения"""
    client = get_client()
    settings = get_settings()

    print("✏️  Редактирование изображения...")
    print(f"⚙️  Настройки:")
    print(f"   • API: {os.getenv('OPENAI_BASE_URL', 'https://api.openai.com/v1')}")
    print(f"   • Модель: {settings['model']}")
    print(f"   • Размер: {settings['size']}")
    print(f"   • Качество: {settings['quality']}")
    print(f"   • Модерация: {settings['moderation']}")
    print(f"\n📝 Промпт:\n{prompt.strip()}\n")
    print(f"📁 Входные изображения:")
    for img_path in input_images:
        print(f"   • {img_path}")
    print()

    # Генерация имени файла, если не указано
    if output_filename is None:
        timestamp = datetime.now().strftime("%Y%m%dT%H%M%S")
        # Берем имя первого изображения без пути
        original_name = Path(input_images[0]).stem
        output_filename = f"{timestamp}_edit_{original_name}.png"

    try:
        # Открываем файлы изображений
        image_files = []
        for img_path in input_images:
            if not Path(img_path).exists():
                print(f"❌ Файл не найден: {img_path}")
                sys.exit(1)
            image_files.append(open(img_path, "rb"))

        # Создание параметров запроса
        # Для edit используем только базовые параметры (quality и moderation не поддерживаются)
        request_params = {
            "image": image_files if len(image_files) > 1 else image_files[0],
            "prompt": prompt,
            "size": settings["size"],
            "model": settings["model"],
            "n": 1,
        }

        # Редактирование изображения
        try:
            response = client.images.edit(**request_params)
        except Exception as api_error:
            # Если size/model не поддерживаются, пробуем с минимальным набором
            print(f"⚠️  Предупреждение: {api_error}")
            print(f"🔄 Повторная попытка с минимальными параметрами...")
            request_params = {
                "image": image_files if len(image_files) > 1 else image_files[0],
                "prompt": prompt,
                "n": 1,
            }
            response = client.images.edit(**request_params)

        print(f"✅ Изображение отредактировано!")

        # Сохранение
        result = save_image(response.data[0], output_filename)

        # Закрываем файлы
        for f in image_files:
            f.close()

        return result

    except Exception as e:
        print(f"❌ Ошибка при редактировании: {e}")
        # Закрываем файлы в случае ошибки
        for f in image_files:
            f.close()
        sys.exit(1)


def main():
    """Главная функция с CLI интерфейсом"""
    parser = argparse.ArgumentParser(
        description="Генератор иконки для приложения 'Вечеринка в кармане'"
    )

    subparsers = parser.add_subparsers(dest="command", help="Режим работы")

    # Команда generate
    generate_parser = subparsers.add_parser("generate", help="Сгенерировать новую иконку")
    generate_parser.add_argument(
        "--prompt", "-p", nargs="+", help="Промпт для генерации (необязательно, можно без кавычек)"
    )

    # Команда edit
    edit_parser = subparsers.add_parser("edit", help="Отредактировать существующую иконку")
    edit_parser.add_argument(
        "--images", nargs="+", required=True, help="Путь к изображениям для редактирования"
    )
    edit_parser.add_argument(
        "--prompt",
        "-p",
        nargs="+",
        required=True,
        help="Промпт для редактирования (обязательно, можно без кавычек)",
    )
    edit_parser.add_argument(
        "--output", "-o", type=str, default=None, help="Имя выходного файла (по умолчанию: TIMESTAMP_edit_ORIGINALNAME.png)"
    )

    args = parser.parse_args()

    print("=" * 70)
    print("  🎉 Генератор иконки для 'Вечеринка в кармане'")
    print("=" * 70)
    print()

    if args.command == "generate":
        # Объединяем слова промпта в одну строку
        prompt = " ".join(args.prompt) if args.prompt and isinstance(args.prompt, list) else args.prompt
        generate_icon(prompt=prompt)
    elif args.command == "edit":
        # Объединяем слова промпта в одну строку
        prompt = " ".join(args.prompt) if isinstance(args.prompt, list) else args.prompt
        edit_icon(args.images, prompt, args.output)
    else:
        # Если команда не указана, запускаем генерацию по умолчанию
        generate_icon()

    print()
    print("=" * 70)
    print("  ✨ Готово!")
    print("=" * 70)


if __name__ == "__main__":
    main()
