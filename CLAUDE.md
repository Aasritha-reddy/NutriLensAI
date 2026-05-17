# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Set JAVA_HOME first — required on this machine
export JAVA_HOME="/d/Android Studio/jbr"

# Debug build
./gradlew assembleDebug

# Release build (minification enabled)
./gradlew assembleRelease

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Push Gemma model to device (required before first run)
adb push gemma-4-E2B-it.litertlm \
  /sdcard/Android/data/com.nutrilens.nutrilensai/files/gemma-4-E2B-it.litertlm
```

There are no unit tests yet (the test directories exist but are empty stubs). Run `./gradlew lint` for static analysis.

## Architecture

MVVM with a single screen. The full data path is:

```
AnalysisScreen (Compose)
    └── AnalysisViewModel (AndroidViewModel)
            ├── GemmaRepository   — LiteRT-LM engine, prompt builder, response parser
            ├── OcrHelper         — ML Kit text recognition (suspend fun)
            └── AssetReader       — reads sample_health_report.txt from assets
```

**State model** — two independent StateFlows drive the UI:
- `uiState: StateFlow<UiState>` — covers the full analysis lifecycle: `ModelNotFound → ModelLoading → Ready → Analyzing → Streaming(partialResponse) → Result(analysisResult) | Error(errorMessage)`
- `ocrState: StateFlow<OcrState>` — `Idle → Processing → Success(extractedText) | Error(errorMessage)`

Both sealed classes live in `model/` (`UiState.kt`, `OcrState.kt`) annotated `@Immutable`. `AnalysisResult(verdict, explanation)` is in `model/AnalysisResult.kt`.

**AI inference** — `GemmaRepository` wraps the LiteRT-LM `Engine`. Key API details:
- Engine init: `Engine(EngineConfig(modelPath, Backend.CPU())).initialize()`
- Inference: `engine.createConversation(ConversationConfig(systemInstruction = Contents.of(str), samplerConfig = SamplerConfig(topK, topP: Double, temperature: Double)))`
- Streaming: `conversation.sendMessageAsync(msg).collect { chunk -> emit(chunk.toString()) }` — returns a `Flow<String>`
- `analyzeStream()` returns `Flow<String>` using `flow { }.flowOn(Dispatchers.IO)`; the ViewModel accumulates chunks into a `StringBuilder` and emits `UiState.Streaming` on each token
- Engine access is protected by a `Mutex` (`engineLock`) to guard concurrent load/read

**Constants** — all magic strings (model filename, FileProvider authority suffix, asset filename) are in `Constants.kt`. Never hardcode them in other files.

**Camera** — uses `ActivityResultContracts.TakePicture` (system camera, no CameraX). `CameraHelper.createPhotoUri()` in `util/` creates the temp file and returns a FileProvider URI. The FileProvider authority is `${packageName}.fileprovider`.

**Timber** — initialized in `NutriLensApplication.onCreate()` (debug builds only). Use `Timber.d/e` everywhere instead of `Log`.

## Version Pinning Constraints

This project has a hard version triangle driven by the Gradle wrapper (8.10.2):

| Constraint | Value |
|---|---|
| Gradle wrapper | 8.10.2 (max supports AGP 8.8.x) |
| AGP | 8.8.0 |
| Kotlin | **2.2.0 minimum** — LiteRT-LM 0.11.0 is compiled with Kotlin metadata 2.2.0 |
| core-ktx | 1.15.0 (1.16+ requires compileSdk 36 + AGP 8.9.1) |
| activity-compose | 1.9.3 (1.10+ same constraint) |
| lifecycle | 2.8.7 (2.9+ same constraint) |

Do **not** bump any of these without first upgrading the Gradle wrapper and AGP together.

## Model File

The Gemma model is **not** bundled in the APK. It must be manually pushed to the device at:
```
/sdcard/Android/data/com.nutrilens.nutrilensai/files/gemma-4-E2B-it.litertlm
```
Source: [huggingface.co/litert-community](https://huggingface.co/litert-community) — download `gemma-4-E2B-it-litert-lm`.

The model filename is defined in `Constants.MODEL_FILENAME`. If the model is renamed or a different model is used, update only that constant.

## Health Report

`app/src/main/assets/sample_health_report.txt` is a synthetic patient profile (Ananya Rao, 42F) with pre-diabetes, high triglycerides, elevated BP, reduced kidney function, and vitamin D deficiency. The entire file is injected verbatim into the LLM prompt as the patient context. Replace it to change the patient profile; the asset name is in `Constants.HEALTH_REPORT_ASSET_NAME`.
