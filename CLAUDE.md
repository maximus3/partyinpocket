# Party in Pocket - Developer Guide

Android party games collection app with AI-powered word generation.

## Build & Run Commands

### Quick Commands (Makefile)

**Recommended way** - use Makefile for all operations:

```bash
# Show all available commands
make help

# Build release and debug APK
make build

# Build only release APK
make build-release

# Install release APK on device
make install

# Show current version info
make info

# Run unit tests
make test

# Clean build artifacts
make clean
```

### Icon Generation

```bash
# Generate new icon with default prompt
make icon

# Generate with custom prompt
make icon PROMPT="Your icon description"

# Copy generated icon to app resources (removes adaptive icons)
make copy-icon
```

### Version Management

```bash
# Increment version (0.0.1 -> 0.0.2)
make bump-version

# Check current version
make info
```

### Manual Commands (if needed)

#### Setup Environment
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

#### Build APK
```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

#### Install on Device
```bash
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/release/app-release.apk
```

#### View Logs
```bash
# All error logs
~/Library/Android/sdk/platform-tools/adb logcat -d "*:E"

# Specific tag filtering
~/Library/Android/sdk/platform-tools/adb logcat -d "WordGeneration:D" "*:S"
```

#### Gradle Tasks
```bash
./gradlew clean
./gradlew :app:assembleRelease
./gradlew dependencies
```

## Architecture

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **Architecture**: MVVM with manual dependency injection (no Hilt)
- **Navigation**: Navigation Compose with sealed class routes
- **State Management**: StateFlow for reactive updates
- **Async**: Coroutines + Flow
- **HTTP Client**: Ktor with Android engine
- **Serialization**: kotlinx.serialization
- **Persistence**: SharedPreferences (MVP approach, no Room/DataStore)

### Package Structure
```
com.m3games.partyinpocket/
├── data/                           # Data layer
│   ├── api/                        # API clients and models
│   │   ├── models/                 # Request/response DTOs
│   │   └── WordGenerationService   # OpenRouter API client
│   ├── wordpacks/                  # Word pack storage
│   └── SettingsRepository          # SharedPreferences wrapper
├── domain/                         # Business logic
│   └── model/                      # Domain models
│       ├── hat/                    # Hat game models
│       ├── AiSettings              # AI configuration
│       ├── Team                    # Team with scores per round
│       └── WordGenerationState     # Generation flow states
└── presentation/                   # UI layer
    ├── navigation/                 # Navigation graph & helpers
    ├── screens/                    # Feature screens
    │   ├── home/
    │   ├── hat/                    # Hat game screens + ViewModel
    │   └── settings/
    ├── components/                 # Reusable components
    └── theme/                      # Material3 theme
```

### Manual Dependency Injection Pattern
No DI framework is used. Dependencies are:
- Created at composition time: `remember { SettingsRepository(context) }`
- Shared via ViewModel composition: `val hatViewModel: HatViewModel = viewModel()`
- Passed as constructor parameters

**Example from NavGraph.kt:**
```kotlin
val settingsRepository = remember { SettingsRepository(context) }
val settingsViewModel = remember { SettingsViewModel(settingsRepository) }
val hatViewModel: HatViewModel = viewModel()
```

### Navigation Patterns

#### Screen Routes
Defined in `Screen` sealed class with string routes:
```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object HatSetup : Screen("hat_setup")
    // ...
}
```

#### Safe Navigation
**Always use safe navigation helpers** from `NavigationExtensions.kt`:
```kotlin
// Instead of popBackStack()
navController.safePopBackStack()

// Instead of navigate()
navController.safeNavigate(Screen.HatGame.route) {
    popUpTo(Screen.Home.route) { inclusive = false }
}
```

**Rationale**: Prevents crashes from rapid double-taps or navigation beyond stack bounds.

#### Navigation Checks
Before navigating, check current destination:
```kotlin
if (navController.currentDestination?.route == Screen.HatTurnResult.route) {
    navController.popBackStack(Screen.HatGame.route, inclusive = false)
}
```

### State Management Patterns

#### ViewModels with StateFlow
```kotlin
class HatViewModel : ViewModel() {
    private val _gameState = MutableStateFlow<HatGameState?>(null)
    val gameState: StateFlow<HatGameState?> = _gameState.asStateFlow()

    fun updateState() {
        _gameState.value = _gameState.value?.copy(/* changes */)
    }
}
```

#### Sealed Classes for States
Use sealed classes for complex state machines:
```kotlin
sealed class WordGenerationState {
    data object Idle : WordGenerationState()
    data class Loading(val attempt: Int, val generatedCount: Int) : WordGenerationState()
    data class Success(val words: List<String>) : WordGenerationState()
    data class PartialSuccess(/* ... */) : WordGenerationState()
    data class Error(val message: String) : WordGenerationState()
}
```

#### Composable State Collection
```kotlin
val gameState by viewModel.gameState.collectAsState()
val state = gameState ?: return  // Early return if null
```

### Game State Machine: Hat Game

#### Phase Flow
```
READY_TO_START -> PLAYING -> TURN_ENDED -> READY_TO_START (next team)
                           -> ROUND_ENDED -> READY_TO_START (next round)
                           -> GAME_FINISHED
```

#### Critical Game Logic

**1. Timer Management**
- Timer runs only during `HatGamePhase.PLAYING`
- Cancelled when phase changes or ViewModel is cleared
- Time countdown happens in coroutine with 1-second delay

**2. Time Preservation Between Rounds**
When a round ends during a team's turn, that team continues in the next round with **remaining time preserved**:
```kotlin
// In startTurn()
val timeToUse = if (state.remainingTimeSeconds > 0 &&
                     state.remainingTimeSeconds < _settings.value.turnDurationSeconds) {
    state.remainingTimeSeconds  // Keep saved time from previous round
} else {
    _settings.value.turnDurationSeconds  // Full duration for new teams
}
```

**3. Team Switching vs Round Ending**
```kotlin
// In nextTeam()
if (state.remainingWords.isEmpty()) {
    // Round ended - keep current team and time for next round
    _gameState.value = state.copy(
        phase = HatGamePhase.ROUND_ENDED,
        // remainingTimeSeconds preserved
    )
} else {
    // Switch to next team - reset time to full duration
    _gameState.value = state.copy(
        currentTeamIndex = state.nextTeamIndex,
        remainingTimeSeconds = _settings.value.turnDurationSeconds
    )
}
```

**4. Skip Mechanics**
- Skips are **per-team for entire game** (not per turn)
- Skipped words are **permanently removed** from all rounds
- Optional penalty points deducted when skipping

**5. Round Transitions**
- Round 1: Explain - describe with words
- Round 2: Pantomime - show with gestures
- Round 3: Association - one word association
- Same word pool used for all rounds

### API Integration: OpenRouter

#### Error Handling Pattern
**Always check status code before parsing response:**
```kotlin
val response: HttpResponse = client.post(settings.baseUrl) { /* ... */ }

if (!response.status.value.toString().startsWith("2")) {
    val errorResponse: OpenRouterError = response.body()
    val errorMessage = when (errorResponse.error.code) {
        401 -> "Неверный API токен. Проверьте настройки."
        403 -> "Превышен лимит API ключа. Проверьте лимиты на https://openrouter.ai/settings/keys"
        429 -> "Слишком много запросов. Попробуйте позже."
        else -> "Ошибка API: ${errorResponse.error.message}"
    }
    return Result.failure(Exception(errorMessage))
}

val successResponse: OpenRouterResponse = response.body()
```

#### Structured Output with JSON Schema
OpenRouter supports JSON Schema for guaranteed output format:
```kotlin
val responseFormat = ResponseFormat(
    type = "json_schema",
    jsonSchema = JsonSchema(
        name = "word_list",
        schema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("words", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject {
                        put("type", "string")
                    })
                })
            })
            put("required", buildJsonArray {
                add(JsonPrimitive("words"))  // Must use JsonPrimitive!
            })
        }
    )
)
```

#### Retry Logic with Progress Callbacks
```kotlin
suspend fun generateWordsWithRetry(
    theme: String,
    targetCount: Int,
    settings: AiSettings,
    maxAttempts: Int = 3,
    onProgress: (attempt: Int, currentCount: Int) -> Unit
): Result<Pair<List<String>, Boolean>>
```

### Color Storage

**Always use Int for color storage:**
```kotlin
data class Team(
    val id: Int,
    val name: String,
    val colorArgb: Int  // NOT Long!
)

// Convert Color to Int
val colorInt = Color.Red.toArgb()

// Convert Int to Color
val color = Color(team.colorArgb)
```

**Rationale**: `Color.toArgb()` returns `Int`, not `Long`. Using `Long` causes `IllegalArgumentException`.

### BackHandler Pattern

For custom back button behavior in Compose:
```kotlin
var showDialog by remember { mutableStateOf(false) }

// First back press shows dialog
BackHandler {
    showDialog = true
}

if (showDialog) {
    // Second back press closes dialog
    BackHandler {
        showDialog = false
    }

    AlertDialog(
        onDismissRequest = { showDialog = false },
        // ...
    )
}
```

## Word Pack System

### Structure
```kotlin
data class WordPack(
    val id: String,
    val name: String,
    val description: String,
    val words: List<String>
)
```

### Storage
- **Preset packs**: Hardcoded in `PresetWordPacks` object
- **Generated packs**: Stored in mutable list (temporary, lost on app restart)
- **Future**: Consider Room database for persistence

### Adding Packs
```kotlin
// Add generated pack
PresetWordPacks.addGeneratedPack(wordPack)

// Add to selected packs
val currentPacks = settings.selectedPacks.toMutableList()
currentPacks.add(packId)
settings.copy(selectedPacks = currentPacks)
```

## Settings Persistence

### SettingsRepository Pattern
```kotlin
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun saveAiSettings(settings: AiSettings) {
        prefs.edit {
            putString("ai_base_url", settings.baseUrl)
            putString("ai_model", settings.model)
            putString("ai_token", settings.token)
        }
    }

    fun getAiSettings(): AiSettings {
        return AiSettings(
            baseUrl = prefs.getString("ai_base_url", /* default */) ?: /* default */,
            // ...
        )
    }
}
```

### Flow Emission
```kotlin
val aiSettings: Flow<AiSettings> = flow {
    emit(repository.getAiSettings())
}
```

## Common Pitfalls

### 1. Don't Parse Error Responses as Success
❌ **Wrong:**
```kotlin
val response: OpenRouterResponse = client.post(url).body()
// Crashes if response contains error JSON
```

✅ **Correct:**
```kotlin
if (!response.status.value.toString().startsWith("2")) {
    val errorResponse: OpenRouterError = response.body()
    // Handle error
}
val successResponse: OpenRouterResponse = response.body()
```

### 2. Don't Use Long for Colors
❌ **Wrong:**
```kotlin
data class Team(val colorArgb: Long)
```

✅ **Correct:**
```kotlin
data class Team(val colorArgb: Int)
```

### 3. Don't Navigate Without Checking
❌ **Wrong:**
```kotlin
navController.popBackStack()
```

✅ **Correct:**
```kotlin
navController.safePopBackStack()
```

### 4. Don't Forget to Cancel Coroutines
❌ **Wrong:**
```kotlin
viewModelScope.launch {
    // Timer without cancellation
}
```

✅ **Correct:**
```kotlin
private var timerJob: Job? = null

fun startTimer() {
    timerJob?.cancel()
    timerJob = viewModelScope.launch { /* ... */ }
}

override fun onCleared() {
    timerJob?.cancel()
}
```

### 5. Don't Use JsonArray.add() with Strings
❌ **Wrong:**
```kotlin
put("required", buildJsonArray {
    add("words")  // Type mismatch!
})
```

✅ **Correct:**
```kotlin
put("required", buildJsonArray {
    add(JsonPrimitive("words"))
})
```

## Testing

### Unit Tests (JVM, no emulator required)

Tests live in `app/src/test/java/com/m3games/partyinpocket/`.

**Run all unit tests:**
```bash
make test
# or directly:
./gradlew testDebugUnitTest
```

**Test report:** `app/build/reports/tests/testDebugUnitTest/index.html`

Tests run automatically in CI (`.github/workflows/build.yml`) before APK build — failing tests block the build.

### What's covered

- **Domain models** (`app/src/test/.../domain/model/`):
  - `TeamTest` — score accumulation per round, color ARGB conversion (guards against `Long` regression)
  - `HatGameStateTest` — computed properties (`currentTeam`, `currentTeamSkipsLeft`, `nextTeamIndex`, `isRoundFinished`, `isGameFinished`)
  - `HatRoundTest` — round progression `EXPLAIN → PANTOMIME → ASSOCIATION → null`
- **Data layer** (`app/src/test/.../data/wordpacks/`):
  - `PresetWordPacksTest` — pack lookup, mutation of generated packs (singleton hygiene via `@After`)
- **ViewModels** (`app/src/test/.../presentation/screens/hat/`):
  - `HatViewModelTest` — full Hat game state machine: `startGame`, `guessWord`, `skipWord`, `nextTeam`, `nextRound`, `resetGame`, timer with `advanceTimeBy`, **time preservation between rounds**, settings updates, team management

### What's NOT covered yet

- **`WordGenerationService`** — Ktor HTTP. Needs MockEngine. Tracked as future work.
- **`SettingsRepository`** — needs Android `Context`/`SharedPreferences`. Use Robolectric or instrumented test when adding.
- **Compose screens** — instrumented tests in `app/src/androidTest/` (currently empty). Manual testing only.
- **AI-related flows in `HatViewModel`** (`startWordGeneration`, `continueWordGeneration`, `saveGeneratedWordPack`) — depend on `WordGenerationService`.

### Testing patterns

**ViewModels with coroutines (timer, viewModelScope):**

Use `MainDispatcherRule` from `app/src/test/.../util/MainDispatcherRule.kt` to swap `Dispatchers.Main`
with a `TestDispatcher`. Pass the same dispatcher to `runTest` so that `advanceTimeBy` controls
the same scheduler as `viewModelScope`:

```kotlin
@get:Rule
val mainDispatcherRule = MainDispatcherRule()

@Test
fun `timer decrements every second`() = runTest(mainDispatcherRule.testDispatcher) {
    viewModel.startTurn()
    runCurrent()                  // дать корутине таймера запуститься
    advanceTimeBy(1_000)          // пропустить 1 секунду
    runCurrent()                  // применить эффекты delay
    assertEquals(N - 1, viewModel.gameState.value!!.remainingTimeSeconds)
}
```

**Singletons with mutable state (`PresetWordPacks.generatedPacks`):**

Add a test pack in `@Before`, remove it in `@After`. Tests that touch `addGeneratedPack` must
clean up to avoid pollution across test runs.

```kotlin
@Before
fun setup() { PresetWordPacks.addGeneratedPack(testPack) }

@After
fun cleanup() { PresetWordPacks.removeGeneratedPack(testPack.id) }
```

**Determinism with shuffled word lists:**

`HatViewModel.startGame` does `selectedWords.shuffled().take(wordCount)`. To keep tests
deterministic, either:
- Use a test pack with `words.size == wordCount` and assert on `set` membership, not order
- Use `wordCount = 1` so `currentWord` is uniquely determined

### When adding a new game

Add tests **in parallel with implementation**:
1. **Domain models** of the new game → `domain/model/<game>/<Model>Test.kt`
2. **ViewModel** of the new game → `presentation/screens/<game>/<Game>ViewModelTest.kt`
3. Run `make test` before pushing — make sure shared code (`Team`, navigation helpers) hasn't broken Hat game.

### Manual testing (still required for UI/UX)

- Rapid navigation (double-tap back button) — see `safeNavigate`/`safePopBackStack` in `NavigationExtensions.kt`
- API error scenarios (invalid token, rate limits) — see `WordGenerationService` error mapping
- Use `~/Library/Android/sdk/platform-tools/adb logcat -d "*:E"` for crash diagnostics

## Release & Distribution

### Release Build Process

**Signing**: Release APKs are signed with the production keystore at `keystores/partyinpocket-release.keystore`. This is the same key used for v0.0.4+ published to RuStore. Earlier versions (v0.0.1-0.0.3) used `keystores/debug-backup.keystore` — kept only as historical backup.

**Required for any release build:**
- Locally: `keystore.properties` at the project root (gitignored). Format and instructions in `keystores/README.md`.
- In CI: GitHub Actions secrets `KEYSTORE_BASE64` and `KEYSTORE_PROPERTIES`.

`build.gradle.kts` **fails fast** on `assembleRelease`/`bundleRelease`/`packageRelease` if `keystore.properties` is missing — this prevents accidentally shipping an APK signed with a per-machine debug key (which would force users to uninstall and reinstall to update).

```bash
# 1. Update version
make bump-version

# 2. Build release APK (требует keystore.properties)
make build-release

# 3. Copy for RuStore (если публикуете вручную)
cp app/build/outputs/apk/release/app-release.apk PartyInPocket-v$(make info | grep versionName).apk
```

**Если потеряли keystore**: восстановите файл из бэкапа и `keystore.properties` по инструкции в `keystores/README.md`. Без этого keystore RuStore отклонит обновление как несовместимое.

### ProGuard Configuration

Release builds have R8 minification enabled (`isMinifyEnabled = true`). Required rules in `app/proguard-rules.pro`:

```proguard
# Ktor - suppress SLF4J warnings
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# Kotlinx Serialization - keep serializers
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.m3games.partyinpocket.**$$serializer { *; }
-keepclassmembers class com.m3games.partyinpocket.** {
    *** Companion;
}
-keepclasseswithmembers class com.m3games.partyinpocket.** {
    kotlinx.serialization.KSerializer serializer(...);
}
```

### Icon Generation Workflow

Icons are generated using AI (OpenAI/AI Tunnel) via Python script in `icon-generator/`:

```bash
cd icon-generator

# Generate icon
uv run main.py generate -p "Your icon description"

# Prepare for Android (converts to all densities)
uv run prepare_app_icons.py output/TIMESTAMP_generated_icon.png

# Or use Makefile from project root
cd ..
make icon PROMPT="Your description"
make copy-icon
```

**Important**: `prepare_app_icons.py` automatically removes:
- `mipmap-anydpi-v26/` (adaptive icon XMLs that reference old drawables)
- `drawable/ic_launcher_*.xml` (old launcher icon drawables)

This ensures WebP icons are used on all Android versions.

### GitHub Actions

Repository has automated builds configured:

**On every push to main** (`.github/workflows/build.yml`):
- Builds both debug and release APK
- Uploads as artifacts (retained 30 days)
- Accessible in Actions tab → Build APK workflow
- Ignores changes to docs, icon-generator, for_release

**On version tags** (`.github/workflows/release.yml`):
- Triggers on tags matching `v*` (e.g., v0.0.1, v1.0.0)
- Builds both APK variants
- Renames to `PartyInPocket-vX.Y.Z.apk` and `PartyInPocket-vX.Y.Z-debug.apk`
- Creates GitHub Release with both files
- Pre-fills release notes template

**Creating a release:**
```bash
# 1. Bump version
make bump-version

# 2. Create changelog
mkdir -p for_release/v0.0.X
nano for_release/v0.0.X/CHANGELOG.md
# Use for_release/CHANGELOG_TEMPLATE.md as reference

# 3. Commit and tag
git add app/build.gradle.kts for_release/v0.0.X/
git commit -m "Bump version to 0.0.X"
git tag v0.0.X

# 4. Push
git push origin main
git push origin v0.0.X
```

**Changelog structure:**
- Each version has its own directory: `for_release/vX.Y.Z/`
- Contains `CHANGELOG.md` describing what's new
- GitHub Actions automatically reads this file and includes in release notes
- If file is missing, release will show: "_Описание изменений не добавлено_"

### RuStore Publishing

App descriptions are in README.md under "Публикация в RuStore" section. Copy from there when publishing.

Files ready for stores in `for_release/`:
- `icon.png` (512x512, <1MB) - auto-generated by prepare_app_icons.py
- `short_description.txt` - 80 char limit
- `full_description.txt` - full app description

Текущая версия — `make info` или `grep versionName app/build.gradle.kts`.

## Future Improvements

### High Priority
1. **Persistence for Generated Packs**: Add Room database or DataStore to save AI-generated word packs between sessions
2. **Cloud Sync**: Allow users to backup/restore their custom word packs
3. **Statistics**: Track games played, favorite word packs, team win rates
4. **Sound & Haptics**: Add timer beep, success/fail sounds, button vibration

### Medium Priority
5. **More Games**: Alias, Spy, Crocodile, Mafia (see README for full list)
6. **Localization**: Support English, Ukrainian
7. **Dark Theme**: Add theme toggle in settings
8. **Enhanced AI Generation**:
   - Choose difficulty level (easy/medium/hard words)
   - Mix multiple themes
   - Exclude specific words/topics

### Low Priority / Technical Debt
9. **DI Framework**: Consider Hilt/Koin if project complexity grows
10. **UI tests**: Add Compose UI tests for critical flows (unit tests for game logic already in place)
11. **Analytics**: Firebase Analytics for usage tracking (requires privacy policy)

### Known Limitations
- Generated word packs don't persist after app restart (stored in memory only)
- No undo/redo for game actions
- Can't edit teams after game starts
- Timer doesn't pause when app goes to background (Android limitation)
- No offline mode for AI generation (requires internet)
