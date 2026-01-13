# VoiceNotes 🎙️

AI-powered voice notes app for Android. Record your thoughts, get instant transcription and smart summaries.

## Features

- **Voice Recording** — One-tap recording with visual feedback
- **AI Transcription** — Automatic speech-to-text conversion
- **Smart Summaries** — AI-generated titles and summaries for each note
- **Multiple AI Providers** — Support for Google Gemini and OpenAI
- **BYOK** — Bring Your Own Key model (your API keys stay on your device)
- **Localization** — English and Russian languages
- **Material Design 3** — Modern, beautiful UI with dynamic theming

## Screenshots

<!-- Add screenshots here -->

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room
- **Network**: Retrofit + OkHttp
- **Preferences**: DataStore
- **AI APIs**: Google Gemini, OpenAI

## Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- Android SDK 35
- JDK 11+

### API Keys

This app uses a BYOK (Bring Your Own Key) model. You'll need to get API keys from:

1. **Google Gemini** (recommended): https://aistudio.google.com/app/apikey
2. **OpenAI** (optional): https://platform.openai.com/api-keys

Enter your API keys in the app's Settings screen.

### Building

1. Clone the repository:
   ```bash
   git clone https://github.com/YOUR_USERNAME/VoiceNotes.git
   ```

2. Open the project in Android Studio

3. Sync Gradle and build:
   ```bash
   ./gradlew assembleDebug
   ```

4. Run on your device or emulator

## Project Structure

```
app/src/main/java/com/example/voicenotes/
├── ai/                    # AI service interfaces and implementations
│   ├── AiService.kt       # Common interface & models
│   ├── GeminiAiService.kt # Google Gemini implementation
│   └── OpenAiService.kt   # OpenAI implementation
├── data/                  # Data layer
│   ├── AppDatabase.kt     # Room database
│   ├── NoteDao.kt         # Data access object
│   ├── NoteEntity.kt      # Database entity
│   ├── NoteRepository.kt  # Repository pattern
│   └── UserPreferencesRepository.kt # DataStore preferences
├── di/                    # Dependency injection
│   └── AppModule.kt       # Hilt module
├── navigation/            # Navigation
│   └── Screen.kt          # Screen routes
├── network/               # Network layer
│   ├── GeminiApi.kt       # Gemini API interface
│   ├── OpenAiApi.kt       # OpenAI API interface
│   ├── NetworkModule.kt   # Retrofit setup
│   └── model/             # API models
├── ui/                    # UI screens
│   ├── NoteDetailsScreen.kt
│   ├── SettingsScreen.kt
│   └── theme/             # Material 3 theme
├── util/                  # Utilities
├── AudioRecorder.kt       # Audio recording
├── MainActivity.kt        # Main activity with navigation
├── NotesListScreen.kt     # Main list screen
└── NotesViewModel.kt      # ViewModel
```

## How It Works

1. **Recording**: User taps the FAB to start recording audio
2. **Processing**: Audio is encoded to Base64 and sent to the selected AI provider
3. **Transcription**: AI transcribes the audio to text
4. **Summarization**: AI generates a short title and summary
5. **Storage**: Note is saved to local Room database
6. **Display**: UI updates automatically via Flow

## License

This project is open source. Feel free to use, modify, and distribute.

## Author

Made with ❤️ as a personal pet project
