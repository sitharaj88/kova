# Kova MVVM Template

A complete Kotlin Multiplatform MVVM app — **all logic shared, only UI native**.

```
template/
├── shared/                      # 100% of the app logic (Kotlin, commonMain)
│   ├── model/TaskItem.kt
│   ├── data/TaskRepository.kt       # swap for SQLDelight/Room/Ktor freely
│   ├── di/AppModule.kt              # Koin modules + initKoin() + ViewModels factory
│   └── feature/tasks/
│       ├── TasksContract.kt         # TasksState (data class) + TasksAction (sealed)
│       └── TasksViewModel.kt        # @NativeExport StateViewModel — shared verbatim
├── androidApp/                  # Jetpack Compose. UI only.
└── iosApp/                      # SwiftUI. UI only. Xcode project pre-wired.
```

## Run it

**Android**

```bash
./gradlew :androidApp:installDebug     # or open in Android Studio and Run
```

**iOS**

```bash
open iosApp/iosApp.xcodeproj           # select a simulator, Run
```

The Xcode target has a "Compile Kotlin Framework" build phase that runs
`./gradlew :shared:embedAndSignAppleFrameworkForXcode` — no manual framework juggling.

**Tests** (shared ViewModel logic, tested once for both platforms)

```bash
./gradlew :shared:testDebugUnitTest
```

## How a screen works

1. `TasksViewModel` (shared) exposes `state: StateFlow<TasksState>` and
   `actions: EventFlow<TasksAction>`, and takes all user input as plain method calls.
2. **Compose** collects `state` with `collectAsStateWithLifecycle()` and `actions` in a
   `LaunchedEffect`.
3. **SwiftUI** uses the KSP-generated `stateNative` / `actionsNative` accessors through the
   **KovaSwift** package (a local SPM dependency, `Package.swift` at the repo root) plus the
   app-side bridge `iosApp/iosApp/Kova/KovaBridge.swift`:
   - `ViewModelHolder { ... }` — owns the ViewModel, clears it on `deinit` (Android-equivalent lifecycle)
   - `Observing(vm.stateNative) { state in ... }` — re-renders on every emission
   - `stream(vm.actionsNative)` — one-shot actions as an `AsyncStream` inside `.task { }`

   `KovaBridge.swift` is the only file that imports both `Shared` and `KovaSwift`; it is the
   one piece you copy into a new app.

## Adding a screen

1. In `shared/feature/<name>/`: add `<Name>Contract.kt` (state + actions) and
   `@NativeExport class <Name>ViewModel : StateViewModel<...>`.
2. Register it in `di/AppModule.kt` (`factory { ... }` + a `ViewModels.<name>()` accessor).
3. Add a Composable on Android, a SwiftUI `View` on iOS. That's the whole checklist —
   no per-screen interop code.

## Using this template standalone

This template consumes Kova from source (`includeBuild("..")` in `settings.gradle.kts`).
To use it as the starting point for a real project:

1. Copy `template/` anywhere and delete the `includeBuild("..")` line.
2. Replace the version-catalog import path in `settings.gradle.kts` with your own catalog
   (copy `../gradle/libs.versions.toml` in).
3. Depend on the published `in.sitharaj.kova:*` artifacts.
4. Rename package `com.example.tasks` and the bundle id `com.example.tasks.ios`.
