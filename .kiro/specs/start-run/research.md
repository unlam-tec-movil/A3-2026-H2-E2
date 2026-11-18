# Research & Design Decisions

## Summary
- **Feature**: `start-run`
- **Discovery Scope**: Extension (light discovery sobre código existente en `develop`, 2026-11-18)
- **Key Findings**:
  - El modal, los permisos y los avisos ya existen, pero la lógica de inicio vive repartida entre
    `TrazoApp.kt` (`requestStartRun`, que además importa `data.location`) y `StartRunViewModel`.
    Nadie llama a `attachActiveSession`: el recorrido nunca arranca.
  - Hay dos `RunGoal` (`domain/model/RunGoal` con `KM_5` y `domain/model/map/RunGoal` con
    `FIVE_KILOMETERS`). El contrato de HU-04 usa el de `map`; se decidió dejar ese.
  - `StartRunFab` vive en `ui/screens/home/components/`, lo usa solo Inicio y su estado
    (`isStartRunEnabled`, `isStartRunOptionVisible`) está en `HomeViewModel`. Mapa no tiene botón
    «+»: `LiveRouteTrackingViewModel.onStartRunClicked()` no lo llama nadie.

## Research Log

### Puntos de extensión
- **Context**: dónde enganchar el arranque real y el «+» único.
- **Sources Consulted**: `ui/navigation/TrazoApp.kt`, `ui/screens/startrun/StartRunViewModel.kt`,
  `ui/screens/home/HomeViewModel.kt`, `ui/screens/map/MapScreen.kt`,
  `domain/repository/RunSessionRepository.kt`, `domain/repository/LocationClient.kt`,
  `domain/usecases/AttachActiveRunSessionUseCase.kt`, `domain/usecases/GpsPointFilterPolicy.kt`,
  `data/location/LocationStatusReader.kt`, `data/di/MotionSensorModule.kt`.
- **Findings**:
  - `StartRunViewModel` se obtiene con `hiltViewModel()` en `TrazoApp`, fuera del `NavHost`: su
    dueño es la Activity y lo comparten Inicio y Mapa. Un `hiltViewModel()` dentro de un destino
    quedaría atado a esa entrada del back stack, así que el estado del «+» tiene que bajar desde
    `TrazoApp`.
  - `LocationClient.getLocationUpdates(intervalMillis)` entrega `RawLocationPoint` con
    `accuracyMeters`. Alcanza para esperar la primera ubicación precisa sin tocar HU-04.
  - El umbral de 30 m ya existe en `GpsPointFilterPolicy` (privado). Se define una constante propia
    del arranque con el mismo valor.
  - `MonotonicClock` ya tiene proveedor único en `MotionSensorModule` (HU-05.2).
  - `kotlin.time.Clock.System.now()` se usa sin opt-in en los tests del proyecto.
  - `FakeRunSessionRepository` (tests JVM) ya implementa `attachActiveSession` e
    `isRunInProgress`.
- **Implications**: un puerto de dominio `LocationStatusReader`, un caso de uso
  `AwaitPreciseLocationUseCase`, y `StartRunViewModel` como único dueño del flujo.

### Skills consultadas
- `trazo-design-system`: el «+» y los avisos usan tokens de `ui/theme`; textos en voseo; área
  táctil mínima de 48 dp.

## Architecture Pattern Evaluation

| Option | Description | Strengths | Risks / Limitations | Notes |
|--------|-------------|-----------|---------------------|-------|
| ViewModel compartido + slots | `StartRunViewModel` dueño de todo; `TrazoApp` arma el `StartRunFab` y el aviso de espera y los pasa como slots a Inicio y Mapa | Una sola lógica; pantallas sin dependencias cruzadas; respeta «sin lógica en `ui/components`» | `HomeScreen` y `MapScreen` cambian su firma | Elegida |
| Lógica dentro de `StartRunFab` | El componente pide permisos y abre el modal | Uso simple | Rompe `structure.md`; difícil de testear | Descartada por el usuario |
| ViewModel por pantalla | Cada pantalla con su propio estado del «+» | Menos cambios | Duplica lógica; dos fuentes de verdad | Descartada |

## Design Decisions

### Decision: dejar `domain/model/map/RunGoal`
- **Context**: dos enums para lo mismo.
- **Selected Approach**: borrar `domain/model/RunGoal`; el modal etiqueta con
  `distanceKm.roundToInt()`.
- **Trade-offs**: toca el modal y sus tests; no toca el contrato de HU-04.

### Decision: esperar la señal en Mapa
- **Context**: R8.2–R8.4.
- **Selected Approach**: al confirmar se cierra el modal, se navega a Mapa y se muestra
  «Esperando señal GPS» con la meta y «Cancelar». La espera es una corrutina del ViewModel que se
  cancela con «Cancelar».

### Decision: puerto `LocationStatusReader`
- **Context**: `TrazoApp` (UI) importa `data.location.readLocationStatus`, contra la regla
  `ui → domain ← data`.
- **Selected Approach**: interfaz en `domain/repository/`, adaptador en `data/location/` que
  reutiliza `readLocationStatus`. `locationSettingsIntent` queda como está (arma un `Intent` de UI).

## Synthesis
- **Generalización**: el «+» deja de ser de Inicio y pasa a `ui/components/common/` sin cambiar su
  API visual.
- **Build vs. adopt**: no se agregan librerías; se reutilizan `LocationClient`, `MonotonicClock`,
  `AttachActiveRunSessionUseCase` y `FakeRunSessionRepository`.
- **Simplificación**: sin timeout de espera (no lo piden los requisitos); el id es un UUID.

## Risks & Mitigations
- Cambio de firma de `HomeScreen`/`HomeContent` (spec `home-recent-runs`, Persona 1) — se documenta
  como disparador de revalidación y se mueven sus tests del FAB a `StartRunViewModelTest` y
  `StartRunFabTest`.
- `LiveRouteTrackingViewModel.onStartRunClicked()` queda sin uso — se deja intacto (HU-04) y se
  avisa a Persona 3.
- Si el permiso se revoca durante la espera, `getLocationUpdates` falla con `SecurityException` —
  se captura y se muestra el aviso de ubicación (R8.5).
