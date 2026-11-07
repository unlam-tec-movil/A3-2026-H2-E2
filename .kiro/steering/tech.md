# Technology Stack

_updated_at: 2026-11-05_

## Architecture

App Android nativa de **una sola Activity** (`MainActivity`) con Jetpack Compose. Arquitectura en
capas inspirada en **hexagonal (puertos y adaptadores)**:

- `domain/`: modelos y reglas de negocio en Kotlin puro, más los **contratos** (interfaces de
  repositorio y puertos como `MotionSource`, `LocationClient`, `MonotonicClock`).
- `data/`: adaptadores que implementan esos contratos (sensores, ubicación, Room, preferencias,
  cámara).
- `ui/`: pantallas Compose con **MVVM + flujo unidireccional** (`ViewModel` expone
  `StateFlow<UiState>` y recibe eventos).
- `di/`: módulos Hilt que unen contratos con implementaciones.

Dirección de dependencias: `ui → domain ← data`. La UI nunca importa `data`.

> El `README.md` (plantilla de la cátedra) ubica las interfaces de repositorio en `data`. El
> proyecto sigue `AGENTS.md` y las specs: **los contratos viven en `domain/repository/`**.

## Core Technologies

- **Language**: Kotlin 2.3 (bytecode Java 17)
- **UI**: Jetpack Compose (BOM 2025.04.01) + Material 3
- **Platform**: Android `minSdk 24`, `targetSdk`/`compileSdk 37`, AGP 9.4
- **Build**: Gradle Kotlin DSL con catálogo de versiones (`gradle/libs.versions.toml`)

## Key Libraries

- **Hilt** (Dagger 2.60) + `hilt-navigation-compose`: inyección y `hiltViewModel()`.
- **Navigation Compose**: rutas por string en un único `NavHost`.
- **Lifecycle** (`collectAsStateWithLifecycle`): observar estado en Compose.
- **Coroutines / Flow**: estado reactivo (`StateFlow`) y flujos fríos de sensores (`callbackFlow`).
- **Coil**: imágenes.
- Previstas por las specs (todavía no en el catálogo): Room (guardado), librería de mapas y de
  ubicación (HU-04), `kotlinx-coroutines-test` (tests con tiempo virtual).

Antes de agregar una dependencia, sumarla al catálogo y avisar al equipo (el `build.gradle.kts` lo
tocan varias personas).

## Development Standards

### Estilo y calidad
- **ktlint** (plugin Gradle + CI). `.editorconfig` permite PascalCase en funciones `@Composable`.
- **Android lint** en CI sobre cada PR a `main`, `develop` y `releases/**`.
- Indentación de 4 espacios, guard clauses, funciones chicas. Comentarios solo para lógica no
  obvia.
- Sin colores hex ni medidas sueltas en composables: todo sale de `ui/theme` (ver
  `design-system.md`).
- Textos visibles siempre en `res/values/strings.xml`.

### Testing
- **JVM (`app/src/test`)**: JUnit 4 con fakes escritos a mano (sin MockK). ViewModels y lógica de
  dominio. Para reglas con tiempo (8 s, 15 s, 20 s), reloj inyectado y tiempo virtual.
- **Instrumentados (`app/src/androidTest`)**: Compose UI Test (`createComposeRule`) sobre los
  `Content` stateless, envueltos en `TrazoTheme`.
- Nombre de clase `<Sujeto>Test`; cada criterio de aceptación relevante tiene su test.
- Cobertura con **Kover** (mínimo configurado: 0 %).

## Development Environment

### Required Tools
- Android Studio con plugin ktlint; JDK 17 (el daemon de Gradle pide JetBrains JDK 25).
- SDK configurado en `local.properties` (no se commitea).

### Common Commands
```bash
./gradlew :app:assembleDebug            # Build
./gradlew :app:testDebugUnitTest        # Tests JVM
./gradlew :app:connectedDebugAndroidTest # Tests en dispositivo/emulador
./gradlew :app:ktlintCheck :app:lint    # Estilo y lint
./gradlew :app:ktlintFormat             # Formatear
```
Correr los builds de Gradle de a uno: en paralelo se queda sin memoria.

## Key Technical Decisions

- **Estado del recorrido fuera de la pantalla**: la sesión en curso vive en un repositorio
  `@Singleton` con un `CoroutineScope` de aplicación, para sobrevivir a los cambios de pestaña. Los
  ViewModels solo la observan.
- **Dominio testeable sin Android**: reglas de tiempo y umbrales como funciones puras / reductores
  que reciben el instante (`nowMillis`) y una configuración calibrable.
- **Reloj monotónico** (`elapsedRealtime`) para duraciones; la fecha y hora reales se guardan aparte.
- **Sin servicio en primer plano**: la app mantiene la pantalla encendida durante el recorrido.
- **Sin splash propia**: Android 12+ muestra el ícono adaptativo al abrir.
- **Íconos** como vector drawables de Material Symbols (no `material-icons-extended`).

## Known Drift / Deuda

- CI corre `koverXmlReportRelease`, que con AGP 9 no ejecuta tests (reporta 0 %). Propuesta: pasar
  a `koverXmlReportDebug`.
- `hilt-android-testing` está declarado como `implementation`; debería ser de test.

---
_Document standards and patterns, not every dependency_
