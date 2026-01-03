# Вечеринка в кармане (Party in Pocket)

## Общая информация

| Параметр | Значение |
|----------|----------|
| Название | Вечеринка в кармане |
| Package | `com.m3games.partyinpocket` |
| Технологии | Kotlin + Jetpack Compose + Material Design 3 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 (Android 14) |

---

## Архитектура

### Clean Architecture + MVVM

```
app/
├── data/                    # Слой данных
│   ├── repository/          # Реализации репозиториев
│   ├── local/               # Локальные источники (Room, DataStore)
│   └── model/               # Data-модели (Entity, DTO)
│
├── domain/                  # Бизнес-логика
│   ├── model/               # Domain-модели
│   ├── repository/          # Интерфейсы репозиториев
│   └── usecase/             # Use cases
│
├── presentation/            # UI слой
│   ├── navigation/          # Навигация
│   ├── theme/               # Material 3 тема
│   ├── components/          # Общие UI-компоненты
│   └── screens/             # Экраны по фичам
│       ├── home/            # Главный экран
│       └── hat/             # Игра "Шляпа"
│
├── di/                      # Dependency Injection (Hilt)
└── utils/                   # Утилиты
```

---

## Структура пакетов

```
com.m3games.partyinpocket/
├── PartyInPocketApp.kt              # Application class
├── MainActivity.kt                   # Single Activity
│
├── data/
│   ├── local/
│   │   ├── database/
│   │   │   ├── AppDatabase.kt
│   │   │   ├── dao/
│   │   │   │   └── GameHistoryDao.kt
│   │   │   └── entity/
│   │   │       └── GameHistoryEntity.kt
│   │   ├── datastore/
│   │   │   └── SettingsDataStore.kt
│   │   └── wordpacks/
│   │       └── WordPacksProvider.kt  # Предустановленные наборы слов
│   ├── repository/
│   │   ├── WordPackRepositoryImpl.kt
│   │   └── GameHistoryRepositoryImpl.kt
│   └── model/
│       └── WordPackDto.kt
│
├── domain/
│   ├── model/
│   │   ├── Game.kt                   # Enum игр (HAT, ALIAS, CROCODILE...)
│   │   ├── Team.kt
│   │   ├── WordPack.kt
│   │   ├── Word.kt
│   │   └── hat/
│   │       ├── HatGameState.kt
│   │       ├── HatRound.kt           # EXPLAIN, PANTOMIME, ASSOCIATION
│   │       └── HatTurnResult.kt
│   ├── repository/
│   │   ├── WordPackRepository.kt
│   │   └── GameHistoryRepository.kt
│   └── usecase/
│       └── hat/
│           ├── StartHatGameUseCase.kt
│           ├── GetNextWordUseCase.kt
│           ├── SubmitWordResultUseCase.kt
│           └── CalculateScoresUseCase.kt
│
├── presentation/
│   ├── navigation/
│   │   ├── NavGraph.kt
│   │   └── Screen.kt                 # Sealed class для роутов
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   └── Shape.kt
│   ├── components/
│   │   ├── GameCard.kt
│   │   ├── TeamChip.kt
│   │   ├── CountdownTimer.kt
│   │   └── AnimatedWordCard.kt
│   └── screens/
│       ├── home/
│       │   ├── HomeScreen.kt
│       │   └── HomeViewModel.kt
│       └── hat/
│           ├── setup/
│           │   ├── HatSetupScreen.kt
│           │   └── HatSetupViewModel.kt
│           ├── teams/
│           │   ├── TeamsScreen.kt
│           │   └── TeamsViewModel.kt
│           ├── game/
│           │   ├── HatGameScreen.kt
│           │   ├── HatGameViewModel.kt
│           │   └── components/
│           │       ├── WordCard.kt
│           │       ├── TimerDisplay.kt
│           │       └── ActionButtons.kt
│           ├── turnresult/
│           │   └── TurnResultScreen.kt
│           ├── roundresult/
│           │   └── RoundResultScreen.kt
│           └── finalresult/
│               └── FinalResultScreen.kt
│
├── di/
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   └── RepositoryModule.kt
│
└── utils/
    ├── Constants.kt
    └── Extensions.kt
```

---

## Навигация

```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")

    // Hat Game
    object HatSetup : Screen("hat/setup")
    object HatTeams : Screen("hat/teams")
    object HatGame : Screen("hat/game")
    object HatTurnResult : Screen("hat/turn_result")
    object HatRoundResult : Screen("hat/round_result")
    object HatFinalResult : Screen("hat/final_result")

    // Future games
    // object AliasSetup : Screen("alias/setup")
    // object CrocodileSetup : Screen("crocodile/setup")
}
```

### Флоу навигации для "Шляпы"

```
Home → HatSetup → HatTeams → HatGame ⟷ HatTurnResult
                                ↓
                         HatRoundResult (после каждого раунда)
                                ↓
                         HatFinalResult (после 3-го раунда)
                                ↓
                              Home
```

---

## Модели данных

### Игра

```kotlin
enum class Game(
    val titleRes: Int,
    val descriptionRes: Int,
    val iconRes: Int,
    val minPlayers: Int,
    val maxPlayers: Int
) {
    HAT(R.string.game_hat, R.string.game_hat_desc, R.drawable.ic_hat, 4, 20),
    // ALIAS(...),
    // CROCODILE(...),
    // SPY(...)
}
```

### Команда

```kotlin
data class Team(
    val id: Int,
    val name: String,
    val color: Color,
    val score: Int = 0
)
```

### Набор слов

```kotlin
data class WordPack(
    val id: String,
    val nameRes: Int,
    val descriptionRes: Int,
    val words: List<String>,
    val difficulty: Difficulty,
    val isPremium: Boolean = false
)

enum class Difficulty { EASY, MEDIUM, HARD }
```

### Состояние игры "Шляпа"

```kotlin
data class HatGameState(
    val teams: List<Team>,
    val allWords: List<String>,           // Все слова в игре
    val remainingWords: List<String>,     // Оставшиеся в текущем раунде
    val currentRound: HatRound,
    val currentTeamIndex: Int,
    val currentWord: String?,
    val turnTimeSeconds: Int,
    val guessedInTurn: List<String>,      // Угаданные за ход
    val skippedInTurn: List<String>,      // Пропущенные за ход
    val isTimerRunning: Boolean,
    val remainingTimeSeconds: Int
)

enum class HatRound(val titleRes: Int, val rulesRes: Int) {
    EXPLAIN(R.string.round_explain, R.string.rules_explain),
    PANTOMIME(R.string.round_pantomime, R.string.rules_pantomime),
    ASSOCIATION(R.string.round_association, R.string.rules_association)
}
```

---

## Предустановленные наборы слов

### Категории

| ID | Название | Кол-во слов | Сложность |
|----|----------|-------------|-----------|
| `general` | Общие | 200 | Easy |
| `animals` | Животные | 150 | Easy |
| `movies` | Фильмы и сериалы | 150 | Medium |
| `celebrities` | Знаменитости | 100 | Medium |
| `food` | Еда и напитки | 150 | Easy |
| `professions` | Профессии | 100 | Easy |
| `countries` | Страны и города | 150 | Medium |
| `actions` | Действия | 100 | Easy |

### Хранение

Слова хранятся в `assets/wordpacks/` в JSON-формате:

```json
{
  "id": "animals",
  "name": "Животные",
  "difficulty": "easy",
  "words": ["кошка", "собака", "слон", ...]
}
```

---

## Экраны

### 1. Home Screen

- Заголовок "Вечеринка в кармане"
- Сетка карточек с играми
- Каждая карточка: иконка, название, краткое описание
- Пока активна только "Шляпа", остальные с лейблом "Скоро"

### 2. Hat Setup Screen

**Настройки:**
- Количество команд: Slider (2-10), default: 2
- Количество слов: Slider (20-100), default: 40
- Время на ход: Slider (30-120 сек), default: 60
- Выбор набора слов: ChipGroup с категориями (мульти-выбор)

**Кнопка:** "Далее" → переход к командам

### 3. Teams Screen

- Список команд с полями для ввода названий
- Автогенерация названий ("Команда 1", "Команда 2"...)
- Выбор цвета для каждой команды
- Кнопка "Начать игру"

### 4. Hat Game Screen (основной игровой экран)

**Состояние "Готовность":**
- Показ: какая команда ходит, какой раунд
- Правила текущего раунда
- Большая кнопка "Старт"

**Состояние "Игра":**
- Таймер (большой, по центру сверху)
- Карточка со словом (анимированная)
- Две кнопки: "Угадали" (зелёная), "Пропустить" (серая)
- Счётчик: угадано/пропущено за ход

**Состояние "Время вышло":**
- Показ результатов хода
- Кнопка "Передать телефон"

### 5. Turn Result Screen

- Список угаданных слов (зелёные)
- Список пропущенных слов (серые)
- Очки за ход
- Кнопка "Следующая команда" или "Завершить раунд"

### 6. Round Result Screen

- Таблица очков всех команд
- Номер завершённого раунда
- Кнопка "Следующий раунд" или "Результаты"

### 7. Final Result Screen

- Пьедестал победителей (1, 2, 3 место)
- Полная таблица очков
- Конфетти анимация для победителя
- Кнопки: "Играть снова", "В меню"

---

## Зависимости (libs.versions.toml)

```toml
[versions]
kotlin = "1.9.22"
compose-bom = "2024.02.00"
compose-compiler = "1.5.8"
hilt = "2.50"
room = "2.6.1"
datastore = "1.0.0"
navigation = "2.7.7"
lifecycle = "2.7.0"
coroutines = "1.7.3"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.1.0" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# DataStore
datastore = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Lifecycle
lifecycle-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }

# Coroutines
coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# Serialization (для JSON)
kotlinx-serialization = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = "1.6.2" }

[plugins]
android-application = { id = "com.android.application", version = "8.2.2" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "1.9.22-1.0.16" }
```

---

## План реализации

### Фаза 1: Базовая структура
- [ ] Настройка Hilt DI
- [ ] Создание темы Material 3
- [ ] Настройка навигации
- [ ] Главный экран со списком игр

### Фаза 2: Игра "Шляпа" - настройка
- [ ] Экран настройки игры (HatSetupScreen)
- [ ] Экран команд (TeamsScreen)
- [ ] Загрузка наборов слов из assets
- [ ] Сохранение настроек в DataStore

### Фаза 3: Игра "Шляпа" - геймплей
- [ ] Игровой экран с таймером
- [ ] Логика отображения слов
- [ ] Обработка "угадал/пропустить"
- [ ] Переход между командами

### Фаза 4: Игра "Шляпа" - результаты
- [ ] Экран результатов хода
- [ ] Экран результатов раунда
- [ ] Экран финальных результатов
- [ ] Подсчёт очков

### Фаза 5: Полировка
- [ ] Анимации переходов
- [ ] Звуковые эффекты (опционально)
- [ ] Вибрация при окончании времени
- [ ] Сохранение истории игр (Room)

---

## Будущие фичи (не для MVP)

- [ ] Онлайн-режим
- [ ] Пользовательские наборы слов
- [ ] Генерация слов через AI
- [ ] Другие игры (Алиас, Крокодил, Шпион)
- [ ] Достижения и статистика
- [ ] Темная/светлая тема
- [ ] Локализация (EN)
