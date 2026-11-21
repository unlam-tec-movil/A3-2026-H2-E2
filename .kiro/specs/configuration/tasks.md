# Tasks Plan — Configuración

## Resumen de Decisiones de Arquitectura y Refactorización
* **Ubicación:** Se reutiliza `LocationRepository.readLocationStatus()` y el enum existente `LocationStatus`. Se elimina `LocationPermissionState`.
* **Cámara:** `PermissionRepository` queda dedicado únicamente a la cámara (`readCameraPermissionStatus()`), implementado en `PermissionRepositoryImpl` llamando a Android directamente sin capas intermedias. Se eliminan `PermissionReader` y `AndroidPermissionReader`.
* **Modo Demo:** `DemoModeRepositoryImpl` utiliza `SharedPreferences` directamente con `by lazy` para no leer el disco en `init`. Se eliminan `DemoModePreferences` y `SharedPreferencesDemoModePreferences`.
* **Regla de Negocio:** La lógica de no alternar el modo demo con carrera en curso se traslada a `SetDemoModeUseCase` en `domain/usecases/`. `SettingsViewModel` solo delega en este caso de uso.
* **Módulos Hilt:** `DemoModeModule` y `PermissionModule` mantienen un único `@Binds` cada uno.

---

## Plan de Tareas

### Tarea 1: Caso de Uso de Dominio para Modo Demo [COMPLETADA]
* **Capa / Componente:** `domain/usecases/`
* **Archivos Creados:**
  * `app/src/main/java/com/trazo/app/domain/usecases/SetDemoModeUseCase.kt`
  * `app/src/test/java/com/trazo/app/domain/usecases/SetDemoModeUseCaseTest.kt`
* **Requisitos Cubiertos:** Requirement 5, Requirement 6.

---

### Tarea 2: Repositorio Directo de Modo Demo [COMPLETADA]
* **Capa / Componente:** `data/repository/` y `data/di/`
* **Archivos Modificados / Creados:**
  * `app/src/main/java/com/trazo/app/data/repository/DemoModeRepositoryImpl.kt` (usa `SharedPreferences` con `by lazy`)
  * `app/src/main/java/com/trazo/app/data/di/DemoModeModule.kt` (un solo `@Binds`)
  * `app/src/test/java/com/trazo/app/data/repository/DemoModeRepositoryImplTest.kt` (pruebas Robolectric)
* **Requisitos Cubiertos:** Requirement 5.

---

### Tarea 3: Repositorio Simplificado de Permisos [COMPLETADA]
* **Capa / Componente:** `data/repository/` y `data/di/`
* **Archivos Modificados / Creados:**
  * `app/src/main/java/com/trazo/app/data/repository/PermissionRepository.kt` (solo cámara)
  * `app/src/main/java/com/trazo/app/data/repository/PermissionRepositoryImpl.kt` (lee Android directamente)
  * `app/src/main/java/com/trazo/app/data/di/PermissionModule.kt` (un solo `@Binds`)
  * `app/src/test/java/com/trazo/app/data/repository/PermissionRepositoryImplTest.kt` (pruebas Robolectric)
  * `app/src/test/java/com/trazo/app/data/repository/FakePermissionRepository.kt`
* **Requisitos Cubiertos:** Requirement 2.

---

### Tarea 4: Eliminación de Capas Intermedias Obsoletas [COMPLETADA]
* **Archivos Eliminados:**
  * `app/src/main/java/com/trazo/app/domain/model/LocationPermissionState.kt`
  * `app/src/main/java/com/trazo/app/data/permissions/PermissionReader.kt`
  * `app/src/main/java/com/trazo/app/data/permissions/AndroidPermissionReader.kt`
  * `app/src/main/java/com/trazo/app/data/local/DemoModePreferences.kt`
  * `app/src/main/java/com/trazo/app/data/local/SharedPreferencesDemoModePreferences.kt`
* **Directorios Eliminados:** `data/permissions/` y `data/local/`.

---

### Tarea 5: ViewModel, UI State y Pruebas Unitarias [COMPLETADA]
* **Capa / Componente:** `ui/screens/settings/` y `app/src/test/`
* **Archivos Modificados:**
  * `app/src/main/java/com/trazo/app/ui/screens/settings/SettingsViewModel.kt` (`SettingsUiState` usa `LocationStatus`, `onDemoModeToggled` delega a `SetDemoModeUseCase`)
  * `app/src/test/java/com/trazo/app/ui/screens/settings/SettingsViewModelTest.kt` (usa `FakeLocationRepository`, comprueba no persistencia con carrera activa)
* **Requisitos Cubiertos:** Requirement 2, Requirement 4, Requirement 5, Requirement 6.

---

### Tarea 6: Componentes de UI y Composable Principal [COMPLETADA]
* **Capa / Componente:** `ui/screens/settings/`
* **Archivos Modificados:**
  * `app/src/main/java/com/trazo/app/ui/screens/settings/components/PermissionRow.kt` (`locationPermissionStatusText` traduce `READY`/`DEVICE_LOCATION_OFF` a "Precisa", etc.)
  * `app/src/main/java/com/trazo/app/ui/screens/settings/SettingsScreen.kt` (usa `state.locationStatus`)
  * `app/src/test/java/com/trazo/app/ui/screens/settings/SettingsContentTest.kt`
* **Requisitos Cubiertos:** Requirement 1, Requirement 2, Requirement 3, Requirement 4, Requirement 5, Requirement 6.

---

### Tarea 7: Verificación Técnica Automatizada y Linter [EN EJECUCIÓN]
* **Comandos de Verificación:**
  * `./gradlew :app:ktlintCheck`
  * `./gradlew :app:testDebugUnitTest`
  * `./gradlew :app:lintDebug`
  * `./gradlew :app:koverVerifyDebug`
  * `./gradlew :app:koverLogDebug`
