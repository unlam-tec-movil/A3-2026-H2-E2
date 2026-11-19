# Repository Guidelines

## Project Structure & Module Organization
- This Kotlin Android application has one Gradle module, `app`, and uses Jetpack Compose, Material 3, and Hilt.
- Source code lives in `app/src/main/java/com/trazo/app/`. Keep business models and repository contracts in `domain/`, implementations in `data/`, and composables in `ui/components/` with styling in `ui/theme/`.
- Keep domain logic independent of Android UI and persistence details. Follow source layout over README examples.
- Resources belong in `app/src/main/res/`; JVM tests in `app/src/test/`; device tests in `app/src/androidTest/`. Dependency versions are centralized in `gradle/libs.versions.toml`.

## Build, Test, and Development Commands
Run from the repository root; use `./gradlew` instead of `./gradlew.bat` on Unix.
- `./gradlew.bat :app:assembleDebug` — build the debug APK.
- `./gradlew.bat :app:installDebug` — install on a running emulator or connected device; launch from its app launcher.
- `./gradlew.bat :app:testDebugUnitTest` — run local unit tests.
- `./gradlew.bat :app:connectedDebugAndroidTest` — run device tests.
- `./gradlew.bat :app:ktlintCheck :app:lint` — check Kotlin formatting and Android lint.
- `./gradlew.bat :app:ktlintFormat` — apply Kotlin formatting.
- `./gradlew.bat :app:koverXmlReportDebug` — generate the coverage report used by CI.

## Coding Style & Naming Conventions
- Use four-space indentation, Kotlin idioms, and ktlint. Respect `.editorconfig`, which permits PascalCase names for `@Composable` functions.
- Use PascalCase for classes and matching filenames, camelCase for functions and properties, and lowercase package names.
- Prefer guard clauses and small, single-purpose functions. Add comments only for non-obvious logic.

## Testing Guidelines
- Use JUnit 4 for JVM tests and AndroidX JUnit, Espresso, or Compose testing for device tests.
- Name test classes `<Subject>Test` and methods descriptively, following `addition_isCorrect` where appropriate.
- Test behavior and failure cases for changed logic. Run tests after every change and report execution blockers. CI publishes Kover coverage; CI fails below 80% line coverage (`koverVerifyDebug`; generated Hilt/Dagger code is excluded).

## Commit & Pull Request Guidelines
- Recent history uses `feat: ...`; follow the same concise, type-prefixed style, using `fix:` or `docs:` as appropriate.
- Keep commits focused. Describe the change and validation in PRs, link relevant issues, and attach screenshots for visible UI changes.
- Run formatting, lint, and relevant tests before requesting review.

## Configuration & Agent Workflow
- Configure the Android SDK through local `local.properties`; never commit credentials or machine-specific paths. The daemon toolchain requests JetBrains JDK 25, while app bytecode targets Java 17.
- Ask before creating additional files unless the current request explicitly authorizes them. Prefer editing existing files and keep changes within the requested scope.
