# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

NutriTrainer AI is a conversational nutrition app (see `Conversational_Nutrition_App_PRD.pdf` for the product spec — an image-based PDF, open it in a viewer). It is a **Kotlin Multiplatform** project targeting **Android and iOS**, with a shared UI written in **Compose Multiplatform**. The codebase is currently at the Kotlin Multiplatform starter stage (`Greeting` / `getPlatform()` sample), so most feature work means adding real code to `:shared`.

## Modules

| Path | What it is |
|------|-----------|
| `shared/` | Kotlin Multiplatform library — all shared logic **and** the shared Compose UI. Exposes the `App()` composable and, for iOS, a `MainViewController()` bundled into a static framework named `Shared`. |
| `androidApp/` | Android application module. Thin shell: `MainActivity` calls `setContent { App() }`. |
| `iosApp/` | Xcode project (`iosApp.xcodeproj`). Thin SwiftUI shell: `ContentView` wraps `MainViewControllerKt.MainViewController()` from the `Shared` framework in a `UIViewControllerRepresentable`. Add SwiftUI / native-iOS code here. |

Both app modules are intentionally minimal — new screens, state, and business logic belong in `shared/src/commonMain`, with platform specifics behind `expect`/`actual`.

## Shared module source sets

- `commonMain` — target-agnostic code and Compose UI. Entry point is `App.kt`'s `@Composable fun App()`.
- `androidMain` / `iosMain` — `actual` implementations (e.g. `Platform.android.kt`, `Platform.ios.kt` provide `actual fun getPlatform()`); `iosMain` also holds `MainViewController.kt`.
- `commonTest` (`kotlin.test`), `androidHostTest` (JVM host tests), `iosTest` (simulator tests).
- `composeResources/` — Compose Multiplatform resources; generated accessors live in package `nutritrainerai.shared.generated.resources` (`Res`, `Res.drawable.*`).

Package is `in.acstechnologies.nutritrainerai`. Note `in` is a Kotlin keyword, so package declarations escape it: `` package `in`.acstechnologies.nutritrainerai ``.

## Common commands

Run from the repo root; the Gradle wrapper is committed.

**JDK:** there may be no system Java on `PATH`. The Gradle *daemon* is pinned to
JDK 21 via `gradle/gradle-daemon-jvm.properties` (auto-provisioned into
`~/.gradle/jdks/`), but the *launcher* still needs a JVM. If `./gradlew` reports
"Unable to locate a Java Runtime", point `JAVA_HOME` at the Android Studio
bundled JBR:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

```bash
# Android app
./gradlew :androidApp:assembleDebug        # build APK
./gradlew :androidApp:installDebug         # build + install on a connected device/emulator

# iOS app: open iosApp/iosApp.xcodeproj in Xcode and run, OR build the framework first:
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Tests
./gradlew :shared:testAndroidHostTest              # shared JVM/host tests
./gradlew :shared:iosSimulatorArm64Test            # shared iOS simulator tests
./gradlew :shared:testAndroidHostTest --tests "in.acstechnologies.nutritrainerai.SharedLogicAndroidHostTest.example"   # single test (host)

# Lint / full verification
./gradlew :androidApp:lint                  # Android Lint (no ktlint/detekt configured)
./gradlew check                             # all checks across modules
```

## Build setup notes

- Versions and plugins are centralized in `gradle/libs.versions.toml` — add/adjust dependencies there, reference as `libs.*` / `libs.plugins.*`, not with hardcoded coordinates.
- The `:shared` module uses AGP 9's **`com.android.kotlin.multiplatform.library`** plugin, so its Android config lives in an `android { }` block **inside `kotlin { }`** in `shared/build.gradle.kts` (not a top-level `android {}`). `withHostTest` / `withDeviceTestBuilder` there control the `androidHostTest` / device-test source sets.
- SDK levels: `compileSdk`/`targetSdk` 36, `minSdk` 28. JVM target 11. Kotlin 2.4.10, Compose Multiplatform 1.11.1.
- Gradle configuration cache and build cache are **on** (`gradle.properties`). If a change breaks configuration-cache compatibility, the build fails at configuration time — fix the offending build logic rather than disabling the cache.
- The iOS framework is `isStatic = true`, baseName `Shared`; Kotlin symbols are exposed to Swift with the `Kt` suffix (e.g. `MainViewControllerKt`).
