# Kova

**Production-ready MVVM for Kotlin Multiplatform — share everything, render natively.**

Kova lets you put *all* of your app's logic — state, ViewModels, use cases, repositories, DI — in
`commonMain`, and keep only rendering code on each platform: Jetpack Compose on Android, SwiftUI on
iOS. Both UIs observe the *same* ViewModel with idiomatic, main-thread-safe APIs.

```
┌─────────────────────── shared (Kotlin) ────────────────────────┐
│  Repositories · Use cases · Koin DI · StateViewModel<S, A>     │
│        state: StateFlow<S>       actions: EventFlow<A>         │
└──────────────┬────────────────────────────────┬────────────────┘
               │ collectAsStateWithLifecycle()  │ stateNative (generated)
        ┌──────▼──────┐                  ┌──────▼──────┐
        │   Compose   │                  │   SwiftUI   │
        │ (UI only)   │                  │  (UI only)  │
        └─────────────┘                  └─────────────┘
```

## Why Kova (vs. plain KMP interop, SKIE, moko-mvvm)

Kotlin's `Flow`, `StateFlow` and coroutines don't survive the Objective-C bridge: Swift sees opaque
suspending machinery, callbacks land on random threads, and nothing is cancellable. SKIE fixes this
by post-processing the compiled framework with a compiler plugin. Kova takes a different, simpler
route with **zero compiler magic**:

- **A tiny runtime** (`kova-core`): `NativeFlow` / `NativeStateFlow` / `NativeSuspend` — closure
  based, always delivered on the **main thread**, cancellable from Swift, *and* auto-cancelled with
  their owning scope. `NativeStateFlow.value` is synchronous, so SwiftUI renders the first frame
  with real state.
- **A real ViewModel** (`kova-viewmodel`): `StateViewModel<State, Action>` built on
  `androidx.lifecycle.ViewModel` (multiplatform). On Android it *is* a Jetpack ViewModel — config
  changes, `viewModelScope`, Compose `viewModel()` all work. On iOS, `ViewModelHost` gives SwiftUI
  the same lifecycle contract (`onCleared`, scope cancellation) via `deinit`.
- **One-shot events done right**: `EventFlow` buffers actions while the UI is detached and delivers
  each exactly once — no lost snackbars on rotation, no replayed navigation on re-subscribe.
- **Codegen where it pays off** (`kova-ksp`): annotate a ViewModel with `@NativeExport` and every
  public `StateFlow`/`Flow`/`EventFlow` property gets a generated `<name>Native` accessor — only in
  the iOS source sets, only for what you export. No boilerplate, no framework-wide rewriting, fully
  debuggable generated Kotlin you can read.
- **A complete MVVM template** (`template/`): a working Tasks app where the Compose and SwiftUI
  screens are line-for-line mirrors over one shared ViewModel — clone it and rename.

|                                   | Kova | SKIE | moko-mvvm |
|-----------------------------------|------|------|-----------|
| Swift-friendly flows              | ✅ generated accessors | ✅ compiler plugin | ✅ manual wrappers |
| Main-thread delivery guaranteed   | ✅ | ⚠️ caller's job | ⚠️ caller's job |
| Real androidx ViewModel base      | ✅ multiplatform | — (interop only) | ❌ custom class |
| One-shot event channel built in   | ✅ | ❌ | ❌ |
| SwiftUI lifecycle for ViewModels  | ✅ `ViewModelHost` | ❌ | ⚠️ partial |
| Build impact                      | tiny KSP step | compiler plugin on every framework build | none |
| Full MVVM app template            | ✅ | ❌ | ❌ |

## Modules

| Artifact | What's inside |
|---|---|
| `in.sitharaj.kova:kova-core` | `NativeFlow`, `NativeStateFlow`, `NativeSuspend`, `EventFlow`, `Cancellable` |
| `in.sitharaj.kova:kova-viewmodel` | `StateViewModel<S, A>`, `ViewModelHost` |
| `in.sitharaj.kova:kova-annotations` | `@NativeExport` |
| `in.sitharaj.kova:kova-ksp` | KSP processor generating `<property>Native` accessors |
| `KovaSwift` (Swift Package, this repo) | SwiftUI bridge: `ViewModelHolder`, `Observing`, `FlowState`, `asyncStream` |

## Quick start

### 1. Shared module

```kotlin
// build.gradle.kts (shared)
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget()
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export("in.sitharaj.kova:kova-core:0.1.0")
            export("in.sitharaj.kova:kova-viewmodel:0.1.0")
            export("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.9.1")
        }
    }
    sourceSets.commonMain.dependencies {
        api("in.sitharaj.kova:kova-core:0.1.0")
        api("in.sitharaj.kova:kova-viewmodel:0.1.0")
        api("in.sitharaj.kova:kova-annotations:0.1.0")
    }
}

dependencies {
    add("kspIosArm64", "in.sitharaj.kova:kova-ksp:0.1.0")
    add("kspIosSimulatorArm64", "in.sitharaj.kova:kova-ksp:0.1.0")
    add("kspIosX64", "in.sitharaj.kova:kova-ksp:0.1.0")
}
```

### 2. Write a ViewModel — once

```kotlin
data class CounterState(val count: Int = 0)
sealed interface CounterAction { data class Toast(val text: String) : CounterAction }

@NativeExport
class CounterViewModel : StateViewModel<CounterState, CounterAction>(CounterState()) {
    fun increment() = setState { copy(count = count + 1) }

    fun save() = intent {                    // coroutine in viewModelScope, errors -> onError()
        repository.save(currentState.count)  // suspend call
        sendAction(CounterAction.Toast("Saved!"))
    }
}
```

### 3. Android — plain Compose

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
LaunchedEffect(viewModel) {
    viewModel.actions.collect { action -> /* snackbar, navigation, ... */ }
}
```

### 4. iOS — plain SwiftUI

```swift
struct CounterScreen: View {
    @StateObject private var holder = ViewModelHolder { CounterViewModel() }

    var body: some View {
        Observing(holder.viewModel.stateNative) { state in   // generated accessor
            Text("Count: \(state.count)")
        }
        .task {
            for await action in stream(holder.viewModel.actionsNative) { /* toast */ }
        }
    }
}
```

`ViewModelHolder`, `Observing` and `FlowState` live in the **KovaSwift** Swift Package (this
repo's [Package.swift](Package.swift)) — add it via SPM. It is framework-agnostic by design;
the only per-app piece is a ~40-line bridge file
([template/iosApp/iosApp/Kova/KovaBridge.swift](template/iosApp/iosApp/Kova/KovaBridge.swift))
that adapts *your* framework's `NativeStateFlow`/`ViewModel` types to it — copy it once and
you're done.

## The template

[template/](template/) is a complete, buildable MVVM app (Tasks CRUD):

- `shared/` — model, repository, Koin modules, `TasksViewModel` (+ unit tests). **All** logic.
- `androidApp/` — Compose UI only.
- `iosApp/` — SwiftUI UI only, Xcode project already wired to build the Kotlin framework.

```bash
cd template
./gradlew :androidApp:assembleDebug          # Android
open iosApp/iosApp.xcodeproj                 # iOS — just Run
./gradlew :shared:testDebugUnitTest          # shared ViewModel tests
```

The template consumes Kova from source via `includeBuild(..)`; in your own project depend on the
published artifacts and delete that line from `settings.gradle.kts`.

## Design rules Kova enforces for you

1. **State is a single immutable data class** per screen, exposed as `StateFlow`.
2. **Everything UI-bound arrives on the main thread** — subscriptions dispatch to
   `Dispatchers.Main.immediate`, on both platforms, always.
3. **Every subscription has two lifelines**: the Swift/Compose side can cancel, *and* the
   `viewModelScope` cancels on clear. Forgetting one never leaks.
4. **Events are not state**: one-shot effects go through `EventFlow`, delivered exactly once,
   buffered while the UI is away.
5. **Nullable flow values are rejected at codegen time** (Objective-C generics can't represent
   them) — model absence inside the state class instead.

## Requirements

- Kotlin 2.2.21+, KSP 2.2.21-2.0.5, Gradle 8.14+, AGP 8.9+
- Xcode 15+, iOS 16+ deployment target (SwiftUI `NavigationStack`)
- JDK 17+

## Roadmap

- `@NativeExport` for suspend functions → generated `NativeSuspend` accessors
- KSP-generated per-app bridge file (retiring the copied `KovaBridge.swift`)
- SavedStateHandle support in `StateViewModel`
- watchOS / tvOS / macOS targets
- Kotlin 2.4 Swift Export backend (true Swift types, no Objective-C bridge) once stable

## License

Apache 2.0 — see [LICENSE](LICENSE).
