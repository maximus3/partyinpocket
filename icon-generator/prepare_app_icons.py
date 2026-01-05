#!/usr/bin/env python3
"""
Подготовка иконок для Android приложения
Конвертирует PNG иконку в нужные размеры и форматы
"""

import sys
from pathlib import Path

from PIL import Image


# Размеры иконок для разных плотностей экрана
ICON_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def prepare_icons(input_image_path: str, output_dir: str = "../app/src/main/res"):
    """
    Подготавливает иконки приложения из исходного изображения

    Args:
        input_image_path: Путь к исходной иконке (1024x1024 PNG)
        output_dir: Путь к директории res Android проекта
    """
    input_path = Path(input_image_path)
    if not input_path.exists():
        print(f"❌ Файл не найден: {input_image_path}")
        sys.exit(1)

    print(f"📸 Загрузка иконки: {input_path}")

    try:
        # Открываем исходное изображение
        img = Image.open(input_path)

        # Проверяем размер
        if img.size[0] != img.size[1]:
            print(f"⚠️  Предупреждение: изображение не квадратное ({img.size[0]}x{img.size[1]})")
            print(f"   Рекомендуется использовать квадратное изображение")

        print(f"📐 Размер исходного изображения: {img.size[0]}x{img.size[1]}")
        print(f"🎨 Режим: {img.mode}")

        output_base = Path(output_dir)
        if not output_base.exists():
            print(f"❌ Директория res не найдена: {output_dir}")
            print(f"   Убедитесь, что запускаете скрипт из правильной директории")
            sys.exit(1)

        # Генерируем иконки для каждой плотности
        print(f"\n🔨 Генерация иконок:")
        for density, size in ICON_SIZES.items():
            mipmap_dir = output_base / f"mipmap-{density}"
            mipmap_dir.mkdir(exist_ok=True)

            # Изменяем размер
            resized_img = img.resize((size, size), Image.Resampling.LANCZOS)

            # Сохраняем как WebP (используется в проекте)
            output_path = mipmap_dir / "ic_launcher.webp"
            resized_img.save(output_path, "WEBP", quality=95)

            # Также сохраняем round версию
            output_path_round = mipmap_dir / "ic_launcher_round.webp"
            resized_img.save(output_path_round, "WEBP", quality=95)

            print(f"   ✅ {density:8s} ({size}x{size}px) -> {output_path}")

        print(f"\n✨ Готово! Иконки сохранены в {output_base}")
        print(f"\n📝 Следующие шаги:")
        print(f"   1. Пересоберите проект: ./gradlew clean assembleDebug")
        print(f"   2. Установите на устройство и проверьте иконку")

    except Exception as e:
        print(f"❌ Ошибка при обработке изображения: {e}")
        sys.exit(1)


def main():
    """Главная функция"""
    if len(sys.argv) < 2:
        print("Использование: uv run prepare_app_icons.py <путь_к_иконке.png>")
        print("\nПример:")
        print("  uv run prepare_app_icons.py output/20260105T123501_generated_icon.png")
        sys.exit(1)

    input_image = sys.argv[1]

    print("=" * 70)
    print("  🎨 Подготовка иконок для Android приложения")
    print("=" * 70)
    print()

    prepare_icons(input_image)

    print()
    print("=" * 70)


if __name__ == "__main__":
    main()
