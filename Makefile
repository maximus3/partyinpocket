.PHONY: help build build-release build-debug icon copy-icon bump-version install install-release install-debug clean info test

# Переменные
JAVA_HOME := /Applications/Android Studio.app/Contents/jbr/Contents/Home
ADB := ~/Library/Android/sdk/platform-tools/adb
BUILD_TOOLS := ~/Library/Android/sdk/build-tools/*
GRADLE := ./gradlew
ICON_GEN_DIR := icon-generator
LATEST_ICON := $(shell ls -t $(ICON_GEN_DIR)/output/*_generated_icon.png 2>/dev/null | head -1)

help:
	@echo "🎮 Party in Pocket - Makefile команды"
	@echo ""
	@echo "Сборка:"
	@echo "  make build          - Собрать release и debug APK"
	@echo "  make build-release  - Собрать только release APK"
	@echo "  make build-debug    - Собрать только debug APK"
	@echo ""
	@echo "Иконка:"
	@echo "  make icon           - Сгенерировать новую иконку (без копирования)"
	@echo "  make icon PROMPT='текст' - Сгенерировать с кастомным промптом"
	@echo "  make copy-icon      - Скопировать последнюю иконку в приложение"
	@echo ""
	@echo "Версия:"
	@echo "  make bump-version   - Увеличить версию приложения"
	@echo "  make info           - Показать текущую версию"
	@echo ""
	@echo "Установка:"
	@echo "  make install        - Установить release APK на устройство"
	@echo "  make install-debug  - Установить debug APK на устройство"
	@echo ""
	@echo "Тесты:"
	@echo "  make test           - Запустить unit-тесты"
	@echo ""
	@echo "Утилиты:"
	@echo "  make clean          - Очистить build директории"

# Сборка
build: build-release build-debug
	@echo "✅ Сборка завершена"

build-release:
	@echo "🔨 Сборка release APK..."
	@export JAVA_HOME="$(JAVA_HOME)" && $(GRADLE) assembleRelease
	@echo "✅ Release APK: app/build/outputs/apk/release/app-release.apk"

build-debug:
	@echo "🔨 Сборка debug APK..."
	@export JAVA_HOME="$(JAVA_HOME)" && $(GRADLE) assembleDebug
	@echo "✅ Debug APK: app/build/outputs/apk/debug/app-debug.apk"

# Иконка
icon:
	@echo "🎨 Генерация иконки..."
	@cd $(ICON_GEN_DIR) && \
		if [ -n "$(PROMPT)" ]; then \
			uv run main.py generate -p "$(PROMPT)"; \
		else \
			uv run main.py generate; \
		fi
	@echo "✅ Иконка сгенерирована в $(ICON_GEN_DIR)/output/"

copy-icon:
	@if [ -z "$(LATEST_ICON)" ]; then \
		echo "❌ Не найдена сгенерированная иконка в $(ICON_GEN_DIR)/output/"; \
		echo "   Запустите: make icon"; \
		exit 1; \
	fi
	@echo "📋 Копирование иконки: $(LATEST_ICON)"
	@cd $(ICON_GEN_DIR) && uv run prepare_app_icons.py ../$(LATEST_ICON)
	@echo "✅ Иконка скопирована в приложение"

# Версия
bump-version:
	@echo "📈 Увеличение версии..."
	@python3 <<EOF\
	import re\
	import sys\
	\
	with open("app/build.gradle.kts", "r") as f:\
	    content = f.read()\
	\
	version_code_match = re.search(r"versionCode = (\d+)", content)\
	version_name_match = re.search(r'versionName = "(\d+)\.(\d+)\.(\d+)"', content)\
	\
	if not version_code_match or not version_name_match:\
	    print("❌ Не удалось найти версию в build.gradle.kts")\
	    sys.exit(1)\
	\
	old_code = int(version_code_match.group(1))\
	new_code = old_code + 1\
	\
	major, minor, patch = map(int, version_name_match.groups())\
	old_version = f"{major}.{minor}.{patch}"\
	patch += 1\
	new_version = f"{major}.{minor}.{patch}"\
	\
	content = re.sub(r"versionCode = \d+", f"versionCode = {new_code}", content)\
	content = re.sub(r'versionName = "\d+\.\d+\.\d+"', f'versionName = "{new_version}"', content)\
	\
	with open("app/build.gradle.kts", "w") as f:\
	    f.write(content)\
	\
	print(f"✅ Версия обновлена: {old_code} -> {new_code}, v{old_version} -> v{new_version}")\
	EOF

info:
	@echo "ℹ️  Информация о проекте:"
	@grep -A 5 "defaultConfig" app/build.gradle.kts | grep -E "(versionCode|versionName)" || echo "Версия не найдена"
	@echo ""
	@if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then \
		echo "📦 Release APK:"; \
		$(BUILD_TOOLS)/aapt dump badging app/build/outputs/apk/release/app-release.apk 2>/dev/null | grep -E "(package|version)" | head -2; \
	fi

# Установка
install: install-release

install-release:
	@if [ ! -f "app/build/outputs/apk/release/app-release.apk" ]; then \
		echo "❌ Release APK не найден. Запустите: make build-release"; \
		exit 1; \
	fi
	@echo "📱 Установка release APK..."
	@$(ADB) install -r app/build/outputs/apk/release/app-release.apk

install-debug:
	@if [ ! -f "app/build/outputs/apk/debug/app-debug.apk" ]; then \
		echo "❌ Debug APK не найден. Запустите: make build-debug"; \
		exit 1; \
	fi
	@echo "📱 Установка debug APK..."
	@$(ADB) install -r app/build/outputs/apk/debug/app-debug.apk

# Тесты
test:
	@echo "🧪 Запуск unit-тестов..."
	@export JAVA_HOME="$(JAVA_HOME)" && $(GRADLE) testDebugUnitTest
	@echo "✅ Тесты пройдены"
	@echo "📊 Отчёт: app/build/reports/tests/testDebugUnitTest/index.html"

# Утилиты
clean:
	@echo "🧹 Очистка..."
	@export JAVA_HOME="$(JAVA_HOME)" && $(GRADLE) clean
	@echo "✅ Очистка завершена"
