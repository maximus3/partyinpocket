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

## Testing Notes

- No unit tests currently (MVP approach)
- Manual testing via device/emulator
- Use `adb logcat` for debugging crashes
- Test rapid navigation (double-tap back button)
- Test API error scenarios (invalid token, rate limits)

## Release & Distribution

### Release Build Process

**Current setup**: Release builds use debug signing (no keystore needed) for RuStore manual publishing.

```bash
# 1. Update version
make bump-version

# 2. Build release APK
make build-release

# 3. Copy for RuStore
cp app/build/outputs/apk/release/app-release.apk PartyInPocket-v0.0.X.apk
```

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

### RuStore Publishing

App descriptions are in README.md under "Публикация в RuStore" section. Copy from there when publishing.

**Current version**: 0.0.1 (versionCode: 1, versionName: "0.0.1")

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
10. **Testing**: Add unit tests for game logic, UI tests for critical flows
11. **Analytics**: Firebase Analytics for usage tracking (requires privacy policy)
12. **Proper Signing**: Add keystore for Google Play (if publishing there in future)

### Known Limitations
- Generated word packs don't persist after app restart (stored in memory only)
- No undo/redo for game actions
- Can't edit teams after game starts
- Timer doesn't pause when app goes to background (Android limitation)
- No offline mode for AI generation (requires internet)
