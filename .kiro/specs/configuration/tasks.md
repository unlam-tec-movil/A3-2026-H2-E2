# Tasks Plan — Configuración

## Resumen de Evaluación de Paquete para Lectura de Permisos
* **Decisión:** El lector de permisos se aloja en `app/src/main/java/com/trazo/app/data/permissions/` (`PermissionReader.kt` / `AndroidPermissionReader.kt`).
* **Justificación:** Moverlo de `data/location/` a `data/permissions/` expresa con claridad en la estructura del proyecto que su responsabilidad abarca múltiples permisos del sistema Android (tanto **Ubicación** como **Cámara**), evitando acoplar conceptos de cámara dentro del paquete exclusivo de GPS/Ubicación.

---

## Plan de Tareas

### Tarea 1: Modelos de Dominio y Lectores en `data/permissions/` [COMPLETADA]
* **Capa / Componente:** `domain/model/` y `data/permissions/`
* **Archivos Creados:**
  * `app/src/main/java/com/trazo/app/domain/model/LocationPermissionState.kt`
  * `app/src/main/java/com/trazo/app/data/permissions/PermissionReader.kt`
  * `app/src/main/java/com/trazo/app/data/permissions/AndroidPermissionReader.kt`
* **Requisitos Cubiertos:** Requirement 2.

---

### Tarea 2: Fuente de Persistencia del Modo Demo en `data/local/` [COMPLETADA]
* **Capa / Componente:** `data/local/`
* **Archivos Creados:**
  * `app/src/main/java/com/trazo/app/data/local/DemoModePreferences.kt`
  * `app/src/main/java/com/trazo/app/data/local/SharedPreferencesDemoModePreferences.kt`
* **Requisitos Cubiertos:** Requirement 5.

---

### Tarea 3: Repositorios y Módulos Hilt en `data/repository/` y `data/di/` [COMPLETADA]
* **Capa / Componente:** `data/repository/` y `data/di/`
* **Archivos Creados:**
  * `app/src/main/java/com/trazo/app/data/repository/PermissionRepository.kt`
  * `app/src/main/java/com/trazo/app/data/repository/PermissionRepositoryImpl.kt`
  * `app/src/main/java/com/trazo/app/data/repository/DemoModeRepository.kt`
  * `app/src/main/java/com/trazo/app/data/repository/DemoModeRepositoryImpl.kt`
  * `app/src/main/java/com/trazo/app/data/di/PermissionModule.kt`
  * `app/src/main/java/com/trazo/app/data/di/DemoModeModule.kt`
* **Requisitos Cubiertos:** Requirement 2, Requirement 5.

---

### Tarea 4: Fakes Manuales de Prueba en `app/src/test/` [COMPLETADA]
* **Capa / Componente:** `app/src/test/` (JVM Unit Tests)
* **Archivos Creados:**
  * `app/src/test/java/com/trazo/app/data/repository/FakePermissionRepository.kt`
  * `app/src/test/java/com/trazo/app/data/repository/FakeDemoModeRepository.kt`
* **Requisitos Cubiertos:** Requirement 2, Requirement 4, Requirement 5, Requirement 6.

---

### Tarea 5: ViewModel, UI State y Pruebas Unitarias (`SettingsViewModelTest`) [COMPLETADA]
* **Capa / Componente:** `ui/screens/settings/` y `app/src/test/`
* **Archivos Creados:**
  * `app/src/main/java/com/trazo/app/ui/screens/settings/SettingsViewModel.kt`
  * `app/src/test/java/com/trazo/app/ui/screens/settings/SettingsViewModelTest.kt`
* **Requisitos Cubiertos:** Requirement 2, Requirement 4, Requirement 5, Requirement 6.

---

### Tarea 6: Recursos de Cadenas de Texto (`strings.xml`) [COMPLETADA]
* **Capa / Componente:** `res/values/strings.xml`
* **Archivos Modificados:**
  * `app/src/main/res/values/strings.xml`
* **Requisitos Cubiertos:** Requirement 1, Requirement 2, Requirement 3, Requirement 5, Requirement 6.

---

### Tarea 7: Componentes de UI de Configuración [COMPLETADA]
* **Capa / Componente:** `ui/screens/settings/components/`
* **Archivos Creados:**
  * `app/src/main/java/com/trazo/app/ui/screens/settings/components/PermissionRow.kt`
  * `app/src/main/java/com/trazo/app/ui/screens/settings/components/DemoModeSwitch.kt`
* **Requisitos Cubiertos:** Requirement 2, Requirement 3, Requirement 5, Requirement 6.

---

### Tarea 8: Composable Principal `SettingsScreen.kt` con Único `@Preview` [COMPLETADA]
* **Capa / Componente:** `ui/screens/settings/`
* **Archivos Creados:**
  * `app/src/main/java/com/trazo/app/ui/screens/settings/SettingsScreen.kt`
* **Requisitos Cubiertos:** Requirement 1, Requirement 2, Requirement 3, Requirement 4, Requirement 5, Requirement 6.

---

### Tarea 9: Navegación e Integración en `TrazoApp.kt` y Pruebas de Navegación [COMPLETADA]
* **Capa / Componente:** `ui/navigation/` y `ui/screens/profile/`
* **Archivos Modificados:**
  * `app/src/main/java/com/trazo/app/ui/navigation/TrazoApp.kt`
  * `app/src/main/java/com/trazo/app/ui/navigation/TrazoScaffold.kt`
  * `app/src/test/java/com/trazo/app/ui/navigation/TrazoScaffoldTest.kt`
* **Requisitos Cubiertos:** Requirement 1, Requirement 3.

---

### Tarea 10: Verificación Técnica Automatizada y Linter [COMPLETADA]
* **Resultados Obtenidos:**
  * `./gradlew :app:ktlintCheck` $\rightarrow$ `BUILD SUCCESSFUL` (0 errores)
  * `./gradlew :app:testDebugUnitTest` $\rightarrow$ `346 passed, 1 skipped, 0 failed`
  * `./gradlew :app:lintDebug` $\rightarrow$ `BUILD SUCCESSFUL` (0 errores)
  * `./gradlew :app:assembleDebug` $\rightarrow$ `BUILD SUCCESSFUL`
  * `./gradlew :app:koverHtmlReportDebug` $\rightarrow$ `BUILD SUCCESSFUL` (Informe generado en `app/build/reports/kover/htmlDebug/index.html`)
* **Requisitos Cubiertos:** Todos los requisitos.

---

### Tarea 11: Verificación Manual de los 6 Escenarios en Emulador [A EJECUTAR POR EL USUARIO]
* **Capa / Componente:** Pruebas Manuales de Aceptación
* **Escenarios a Validar:**
  1. **Req. 1:** Tocar el engranaje desde Perfil abre "Configuración" con secciones "Permisos" y "Modo demo".
  2. **Req. 2:** Muestra el estado real de permisos (Ubicación: Precisa / Aproximada / Denegada; Cámara: Permitida / Denegada).
  3. **Req. 3:** Tocar "Ir a configuración" abre los ajustes de la app en Android.
  4. **Req. 4:** Modificar un permiso en los ajustes de Android y volver a la app actualiza el estado inmediatamente.
  5. **Req. 5:** Sin carrera en curso, activar el interruptor muestra el mensaje *"Modo demo activado: los recorridos son simulados"* y persiste el cambio.
  6. **Req. 6:** Con una carrera en curso, el interruptor aparece deshabilitado con el mensaje *"Finalizá el recorrido para cambiar el modo demo"*. Entrar y salir de Configuración no altera la carrera.
* **Requisitos Cubiertos:** Requirement 1, Requirement 2, Requirement 3, Requirement 4, Requirement 5, Requirement 6.
