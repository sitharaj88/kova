---
name: kova-migrate
description: Migrate an existing Android or Kotlin Multiplatform project to the Kova MVVM architecture — shared StateViewModels, EventFlow one-shot events, @NativeExport Swift interop, UI-only platform code. Use when the user wants to adopt Kova in an existing app, move ViewModels/business logic into shared code, or add an iOS app to an Android project.
---

# Migrate an existing project to Kova

Kova (https://github.com/sitharaj88/kova) provides `StateViewModel<S, A>` on the androidx
multiplatform ViewModel, `EventFlow` one-shot events, and `@NativeExport` KSP codegen for
Swift-friendly state observation. Migration is incremental — one screen at a time, the app
keeps building throughout. Read the repo README and `template/` for reference wiring.

## 1. Assess the starting point

- **Android-only app** → first create a `shared` KMP module (androidTarget + iosArm64/
  iosSimulatorArm64/iosX64, framework `baseName`, `isStatic = true`). Copy the structure from
  `template/shared/build.gradle.kts` in the Kova repo.
- **Existing KMP project** → reuse its shared module; only add the Kova pieces.
- Record the current ViewModel inventory: for each screen note its LiveData/StateFlow
  properties, one-shot events (SingleLiveEvent/SharedFlow/Channel), and coroutine usage.

## 2. Add Kova

In the shared module: `api("in.sitharaj.kova:kova-core:<v>")`, `kova-viewmodel`,
`kova-annotations`; `kova-ksp` via `add("kspIosArm64", ...)` etc. (iOS targets only).
In the iOS framework block, `export()` kova-core, kova-viewmodel, and
`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel`. If artifacts aren't on Central yet,
use `mavenLocal()` (after `./scripts/publish.sh local` in the Kova repo) or `includeBuild`.

## 3. Migrate one screen (repeat per screen)

For a screen with `FooViewModel`:

1. **Contract**: create `FooState` (one immutable data class — merge every LiveData/StateFlow
   field into it; model per-widget loading as non-generic sealed sub-states) and `FooAction`
   (sealed interface for navigation/snackbar/one-shot effects).
2. **ViewModel**: `@NativeExport class FooViewModel(deps) : StateViewModel<FooState, FooAction>(FooState())`
   in `commonMain`. Mechanical mapping:
   - `_state.value = x` / LiveData `postValue` → `setState { copy(...) }`
   - SingleLiveEvent / event SharedFlow → `sendAction(...)`
   - `viewModelScope.launch { }` → `intent { }` (errors route to `onError`)
   - Move platform dependencies behind interfaces (repository pattern); actual implementations
     can stay platform-specific via Koin.
3. **Android UI**: unchanged pattern — `viewModel.state.collectAsStateWithLifecycle()`,
   actions collected in `LaunchedEffect`. Delete the old ViewModel once the screen compiles.
4. **iOS UI** (if the project has one): copy `KovaBridge.swift` from the template and add the
   KovaSwift package (repo root `Package.swift` via SPM, or vendor the single source file).
   Screens use `ViewModelHolder { ... }`, `Observing(vm.stateNative) { state in ... }`,
   `stream(vm.actionsNative)` in `.task { }`.
5. **Tests**: port the screen's ViewModel tests to `commonTest` (StandardTestDispatcher +
   `Dispatchers.setMain`, Turbine for flows — see `TasksViewModelTest` in the template).

## 4. Verify after every screen

`:shared:testDebugUnitTest` + `:androidApp:assembleDebug` (+ the iOS `xcodebuild` if present)
must stay green between screens. Never migrate a second screen on top of a red build.

## Known traps

- Kotlin package must not start with `in.` (hard keyword). Class named `Task` collides with
  Swift Concurrency — rename during the move to shared.
- Nullable flow value types are rejected by the KSP processor — model absence inside the
  state class.
- Hilt does not work in commonMain; migrate screen DI to Koin as each ViewModel moves
  (Koin android + core split as in the template's `AppModule.kt`).
- Swift files need both `import Shared` and `import KovaSwift`; keep the
  `Lifecycle_viewmodelViewModel` typealias from `KovaBridge.swift`.
