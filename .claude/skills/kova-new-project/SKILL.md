---
name: kova-new-project
description: Create a new Kotlin Multiplatform MVVM app from the Kova template — all logic shared (ViewModels, repositories, Koin DI), UI-only Compose Android app and SwiftUI iOS app. Use when the user wants to start a new KMM/KMP app, "create a project with Kova", or scaffold a shared-ViewModel mobile app.
---

# Create a new project from the Kova template

Kova's template lives in the `template/` directory of https://github.com/sitharaj88/kova
(locally at `/Users/sitharajseenivasan/Documents/kmm_lib/template` if present). It is a
complete Tasks app: shared module (models, repository, Koin, `@NativeExport` StateViewModels),
Compose `androidApp`, SwiftUI `iosApp` with a pre-wired Xcode project.

## Gather inputs first

Ask the user (or infer from their request): **app name** (e.g. "Recipes"), **Kotlin package**
(e.g. `com.acme.recipes` — reject packages starting with `in.` since `in` is a Kotlin hard
keyword; suggest dropping the TLD instead), **destination directory**, and whether to keep the
sample Tasks feature as reference code or strip it.

## Steps

1. **Copy the template.** Prefer the local checkout; otherwise
   `git clone --depth 1 https://github.com/sitharaj88/kova` to a temp dir and copy `template/`.
   Copy `gradle/libs.versions.toml` from the repo root into the new project's `gradle/`
   directory, and the Gradle wrapper (`gradlew`, `gradle/wrapper/`) if not already present in
   the template copy.

2. **Make it standalone.** In `settings.gradle.kts`: remove `includeBuild("..")`, point the
   version catalog at the local `gradle/libs.versions.toml` (`from(files("gradle/libs.versions.toml"))`),
   and set `rootProject.name`.

3. **Kova dependency.** Keep the `in.sitharaj.kova:*` coordinates (core, viewmodel,
   annotations as `api`, `kova-ksp` in the per-iOS-target `ksp*` configurations, plus the
   `export(...)` lines in the framework block). If the artifacts are not yet resolvable from
   Maven Central, tell the user to either run `./scripts/publish.sh local` in the Kova repo
   and add `mavenLocal()` to repositories, or use `includeBuild("<path-to-kova>")`.

4. **Rename the package** from `com.example.tasks` to the chosen package: move source
   directories under `shared/src/*/kotlin/` and `androidApp/src/main/kotlin/`, then update
   every `package`/`import` statement, `namespace` in both `build.gradle.kts` files,
   `applicationId`, and the Koin/`initKoin` references. Verify with a project-wide grep that
   `com.example.tasks` no longer appears outside `iosApp`.

5. **iOS app.** In `iosApp/iosApp.xcodeproj/project.pbxproj` update `PRODUCT_BUNDLE_IDENTIFIER`
   (e.g. `com.acme.recipes.ios`) and `PRODUCT_NAME` (the app name). Swift files need no package
   changes (Swift sees framework type names only). Keep `KovaBridge.swift` as is — it is the
   app↔KovaSwift adapter. The Xcode project references the KovaSwift Swift package via a local
   path (`XCLocalSwiftPackageReference` "../.." in the template repo); for a standalone project
   change it to the GitHub URL package reference (repo root `Package.swift` of sitharaj88/kova)
   or vendor `kova-swift/Sources/KovaSwift.swift` into the app target and delete the package
   reference machinery.

6. **App identity.** Update `android:label` in the manifest, the visible titles in
   `TasksScreen.kt`/`TasksScreen.swift`, and rename `TasksApplication` if the user wants.
   If stripping the sample: keep `di/`, one empty state/action contract, and one minimal
   `StateViewModel` as a seed screen so both UIs still compile.

7. **Verify — do not skip.** `./gradlew :shared:testDebugUnitTest :androidApp:assembleDebug`
   must pass, and if on macOS with Xcode:
   `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`.
   Builds need JDK 17+ (on this machine: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`).

## Known traps

- Never name a Kotlin class `Task` (collides with Swift Concurrency) — the template uses `TaskItem`.
- SwiftUI files using Kova helpers need both `import Shared` and `import KovaSwift`.
- The androidx ViewModel exports to Swift as `Lifecycle_viewmodelViewModel`; the typealias in
  `KovaBridge.swift` handles it — don't remove it.
- KSP generation is per-iOS-target on purpose; don't add `kova-ksp` to Android/common configs.
