# Design Document · finish-run-result

> **Revisión 2026-11-23 · cierre HU-01 a HU-06 (refactor #128).** Esta nota manda sobre lo que se
> contradiga más abajo.
>
> - `ResultFormatter` se borró: el Resultado usa `RunValueFormatter` (`ui/components/common/`), con
>   los mismos formatos (`5,2 km`, `00:32:10`, `80 %`). Su test es `RunValueFormatterTest`.
> - `GetFinishedRunSummaryUseCase` ya no existe: `ResultViewModel` lee
>   `RunSessionRepository.getFinishedSummary(id)` directo.
> - La regularidad `null` se muestra como «sin dato» (`run_value_no_data`), no como «No disponible».
> - «Continuar» ya navega a `saverun/{runId}` (HU-07): el parche de la tarea 5.3 no corre más.
> - El slot `shortRunNotice` de `MapScreen` se borró: el aviso de recorrido corto es un diálogo
>   privado de `MapScreen`.
> - `GoalCelebrationOverlay` y `FinishConfirmDialog` pasaron a `ui/screens/map/components/`, y
>   `onFinish` es obligatorio en `GoalCelebrationOverlay`.

> Revisado contra el código real de `feat/hu-06-resultado` (con HU-04 ya mergeada desde `develop`).
> Los requisitos no cambian. Varias piezas que este diseño pensaba crear **ya existen** en HU-04
> (Persona 3); lo que queda para esta spec es la pantalla de resultado, su ruta y un ajuste mínimo
> al aviso de meta existente (este último pasó a Persona 3, ver «Acuerdos con Persona 3»).

## Overview

**Purpose**: entregar al corredor el cierre de su recorrido: aviso de meta alcanzada mientras corre,
aviso de recorrido demasiado corto y pantalla de resultado («¡Meta cumplida!» o «Recorrido
finalizado») con el paso a «Guardar recorrido».

**Users**: corredores recreativos que registran un recorrido con meta de 3, 5 o 10 km.

**Impact**: HU-04 ya implementa la decisión de cierre y casi todo el Mapa (ver «Existing
Architecture Analysis»). Esta spec agrega: (1) ~~el botón «Finalizar» al aviso de meta~~ (ahora lo hace Persona 3 en HU-04); (2) la ruta `result/{runId}` y la pantalla de resultado, que lee el
`FrozenRunSummary` con `RunSessionRepository.getFinishedSummary(id)`; (3) el paso a «Guardar
recorrido» por id.

### Goals

- Que llegar a la meta solo muestre el aviso: el registro sigue activo hasta que el corredor
  confirme.
- Que por navegación viaje solo el id del recorrido; la traza completa se lee del repositorio.
- Mostrar el resultado sin recalcular valores (5.5).
- Cumplir accesibilidad: «Quitar animaciones», fuente al 200 %, estado no solo por color.

### Non-Goals

- Formulario de guardado, destacados y fotos (`save-run`).
- Cálculo de frenadas (`stop-detection`), de regularidad (HU-05.2) y de las métricas (HU-04).
- **Modo demo: fuera de alcance.** `FrozenRunSummary.isDemoMode` existe (por defecto `false`), pero la
  pantalla de resultado lo ignora.
- Reescribir `LiveRouteTrackingViewModel`, `FinishConfirmDialog`, el diálogo de recorrido corto o
  `RunSessionRepositoryImpl` (HU-04, Persona 3).
- Persistencia en disco y recuperación ante muerte del proceso.
- Librería de animación (Lottie): las animaciones son nativas de Compose.

## Acuerdos con Persona 3

1. **Botón «Finalizar» y errores (Persona 3, HU-04).** Persona 3 agrega «Finalizar» en
   `GoalCelebrationOverlay`, lo conecta al diálogo de confirmación existente y corrige el manejo de
   errores de `finishSession()` para poder reintentar sin perder la sesión. Esta spec no toca esos
   archivos (tareas 2.1–2.3, a cargo de Persona 3).
2. **Limpieza del resumen.** `getFinishedSummary(id)` mantiene su nombre. Persona 3 agrega
   `removeFinishedSummary(id)` a `RunSessionRepository`; lo llama `save-run` (HU-07) al terminar de
   guardar o al descartar el recorrido. **HU-06 no lo llama**: la pantalla de Resultado solo lee, y el
   descarte de un recorrido corto usa `resetToIdle()` sin crear resumen.
3. **`rememberReduceMotion()`** vive en `ui/theme/ReduceMotion.kt` (PR #105, de Rocío). Persona 3 usa
   esos mismos archivos y borra su versión provisoria de `GoalCelebrationOverlay.kt`.
4. **Ruta y cableado.** `result/{runId}` y el cableado en `TrazoApp.kt` son de HU-06; Persona 3 no
   toca ese archivo. «Continuar» llama a `onContinue(runId)`; la ruta `saverun/{runId}` es de
   `save-run` y se conecta cuando exista.

## Existing Architecture Analysis (código real)

Qué hace hoy HU-04 y qué parte de esta spec ya cubre:

| Pieza real | Dónde | Qué hace | Req. cubiertos |
|-----------|-------|----------|----------------|
| `LiveRouteTrackingViewModel.onFinishRequested()` | `ui/screens/map/` | Si no hay sesión, nada. Si `rawDistanceMeters < 200.0` → `showShortRunDialog`; si no → `showFinishConfirmDialog`. El 200 está escrito a mano en el ViewModel. | 2.1, 3.1, 4.1, 4.2, 4.6 (la guarda vive acá) |
| `FinishConfirmDialog` | `ui/components/layout/` | `AlertDialog` con texto según `goalReached` (`map_finish_confirm_goal_reached` / `_not_reached`), «Finalizar» (`map_finish`, rojo) y «Cancelar». Se invoca directo desde `MapScreen`; no hay parámetro `confirmDialog` ni `FinishPrompt`. | 2.1, 2.4, 3.1, 3.4 |
| Aviso de recorrido corto | `MapScreen` | `AlertDialog` por defecto (`map_short_run_title`, `map_short_run_text`, «Descartar» / «Volver al recorrido»), con un slot opcional `shortRunNotice`. Cerrar = volver. | 4.1, 4.4 |
| `onDiscardShortRun()` | ViewModel | `resetToIdle()` (detiene jobs y deja `Idle`) + efecto `ReturnToIdleMap`. No llama a `finishSession()`, así que no queda resumen guardado. | 4.5 |
| `GoalCelebrationOverlay` | `ui/components/layout/` | Aviso de meta a pantalla completa (scrim, «¡Felicitaciones!», «Alcanzaste tu meta de X», «Seguir corriendo»), con `fadeIn`/`fadeOut` y sin animación si `rememberReduceMotion()`. El ViewModel lo muestra una vez al cruzar la meta (`previousGoalReached`). **No tiene botón «Finalizar»: lo agrega Persona 3.** | 1.1 (parcial), 1.2, 1.4, 1.5 |
| `LiveRouteTrackingUiState.suppressed` | `ui/screens/map/` | `showGoalCelebration \|\| showFinishConfirmDialog \|\| showShortRunDialog`. Ya está expuesto y testeado. | 7.1 (lado productor) |
| `RunSessionRepositoryImpl.finishSession()` | `data/repository/` | Detiene jobs, congela métricas, arma el `FrozenRunSummary`, lo guarda en `finishedSummaries` (interno), deja `Idle` y devuelve `Result<FrozenRunSummary>`. | 2.3, 3.3, 5.6 |
| `RunSessionRepository.getFinishedSummary(id)` | `domain/repository/` | `FrozenRunSummary?` por id. Hoy no existe `remove`; Persona 3 agrega `removeFinishedSummary(id)`, que llama `save-run`. | lectura para 5.x y 6.x |
| `FinishRunUseCase` | `domain/usecases/` | Delega en `finishSession()`. | 2.3, 3.3 |
| `LiveRouteTrackingUiEffect.NavigateToResults(summary)` | ViewModel / `MapScreen` | Tras `finishSession()` exitoso emite el **`FrozenRunSummary` completo** (no el id). | — |
| `TrazoApp` | `ui/navigation/` | `onNavigateToResults = { _ -> /* TODO (Persona 4) */ }`. No hay ruta de resultado ni de guardado. | — |
| `rememberReduceMotion()` | `ui/theme/ReduceMotion.kt` (PR #105, Rocío) | Utilidad definitiva. La versión provisoria de `GoalCelebrationOverlay.kt` la borra Persona 3. | 1.5, 5.4 |

**Modelo real**

- `FrozenRunSummary(id, goal: map.RunGoal, routePoints, metrics: RunMetrics, startedAt,
  startedAtMonotonicTimestamp, finishedAtMonotonicTimestamp, goalReached, isDemoMode = false)`.
- `RunMetrics(elapsedTimeSeconds: Long, distanceMeters: Double, currentSpeedKmh, averageSpeedKmh)`.
  La distancia está en **metros (Double)**, no en km.
- Hay **dos** `RunGoal`: `domain.model.RunGoal` (`kilometers: Int`, lo usa Iniciar recorrido) y
  `domain.model.map.RunGoal` (`distanceKm: Double`, lo usa el recorrido y el resumen). Esta spec usa
  el de `map`, porque es el del `FrozenRunSummary`.
- El resumen **no trae frenadas ni regularidad**.
- `finishSession()` ya deja la sesión en `Idle`: llamar a `resetToIdle()` después es redundante.

**Convenciones**: `ui → domain ← data`; `Screen` + `Content` stateless + `ViewModel`; use cases finos
en `domain/usecases/` (por ejemplo `FinishRunUseCase`); los componentes compartidos del Mapa viven
en `ui/components/layout/`; tests JVM en `app/src/test` (con `kotlinx-coroutines-test` ya incluido)
y de UI en `app/src/androidTest`.

## Qué queda realmente para esta spec

| Tema | Estado | Decisión |
|------|--------|----------|
| Aviso de meta con animación (1.1) | **Cubierto** por `GoalCelebrationOverlay` (texto distinto al del diseño viejo, pero los requisitos no fijan el texto) | Reutilizar |
| Botón «Finalizar» en el aviso (1.1, 1.3) | **Falta** | **Persona 3 (HU-04)** lo agrega y lo conecta; no es de esta spec. |
| Sin animación con «Quitar animaciones» (1.5) | Cubierto | Reutilizar |
| Aviso de recorrido demasiado corto (4.1–4.5) | **Cubierto** por el `AlertDialog` de `MapScreen` | No se crea `TooShortRunDialog` ni `DiscardRunUseCase` |
| Decisión y confirmaciones de «Finalizar» (2.x, 3.x, 4.2) | **Cubierto** por el ViewModel y `FinishConfirmDialog` | No se crean `FinishPolicy`, `FinishRunViewModel`, `FinishRunOverlay` ni `CloseRunUseCase` |
| Guarda de 200 m (4.6) | Cubierta en `onFinishRequested()` (la distancia no decrece) | Sin cambios |
| Prioridad del cierre, lado productor (7.1) | `suppressed` ya existe | Sin cambios |
| Prioridad del cierre, lado consumidor (7.1–7.3) | No hay código de `stop-detection` en `main` | **Bloqueado** por `stop-detection` |
| Pantalla de resultado (5.x) | **Falta** | Esta spec |
| Ruta del resultado y paso a «Guardar» con id (6.1) | **Falta** (stub con `TODO`) | Esta spec: ruta `result/{runId}` y `onContinue(runId)`; la ruta `saverun/{runId}` es de `save-run` |
| Conservar datos al guardar (6.2–6.4) | Los datos están en el resumen, salvo frenadas y regularidad | `save-run` los lee por id y llama `removeFinishedSummary(id)` al terminar; ver Open Questions |

## Boundary Commitments

### This Spec Owns

- Pantalla `result` completa: `ResultScreen`, `ResultContent`, `ResultViewModel`, `GoalReachedCard`,
  `ResultFormatter` y su ruta `result/{runId}`.
- `GetFinishedRunSummaryUseCase` (fino, sobre `getFinishedSummary(id)`).
- Conectar `onNavigateToResults` de `TrazoApp` a la ruta con el id.
- Textos `result_*` del resultado.
- El callback `onContinue(runId)` de «Continuar».

### Out of Boundary

- **Botón «Finalizar» del aviso de meta** (`GoalCelebrationOverlay`, conexión en `MapScreen`) y el
  manejo de errores de `finishSession()` con reintento: Persona 3 (HU-04).
- **`removeFinishedSummary(id)`**: lo agrega Persona 3 al repositorio y lo llama `save-run` (HU-07).
  HU-06 no lo llama.
- `LiveRouteTrackingViewModel`, `LiveRouteTrackingUiState`, `FinishConfirmDialog`, el diálogo de
  recorrido corto, `RunControls`, `RunSessionRepositoryImpl` y sus textos `map_*` (HU-04, Persona 3).
- Limpiar el resumen (ver arriba): esta spec solo lee.
- Detección de frenadas, alerta y consulta de 20 s (`stop-detection`).
- Regularidad (HU-05.2, sin dueño) y modo demo (la pantalla lo ignora).
- Pantalla «Guardar recorrido», su ruta y qué hace al guardar o descartar (`save-run`).
- Permisos, GPS, pausa y reanudación.

### Allowed Dependencies

- `domain`: `RunSessionRepository`, `FrozenRunSummary`, `RunMetrics`, `map.RunGoal`.
- `ui/screens/result` → `domain`, `ui/components`, `ui/theme`. Nunca `data`.
- Navigation Compose para `result/{runId}`.
- No se agrega ninguna librería (`kotlinx-coroutines-test` y `ui-test-junit4` ya están en
  `app/build.gradle.kts`).

### Revalidation Triggers

- HU-04 cambia `FrozenRunSummary`, `RunMetrics`, la firma de `getFinishedSummary` (nombre acordado)
  o de `finishSession()`.
- HU-04 cambia el efecto `NavigateToResults` (hoy lleva el resumen; se usa `summary.id`).
- Persona 3 no entrega el botón «Finalizar» del aviso (HU-04).
- Cambia la ubicación de `rememberReduceMotion()` (hoy `ui/theme/ReduceMotion.kt`).
- `stop-detection` define cómo consume `suppressed` o cómo aporta las frenadas.
- `save-run` define su ruta y cómo lee el recorrido.

## Architecture

```mermaid
graph TB
    subgraph UI
        MapScreen --> GoalCelebrationOverlay
        MapScreen --> FinishConfirmDialog
        MapScreen --> LiveRouteTrackingViewModel
        TrazoApp --> MapScreen
        TrazoApp --> ResultScreen
        ResultScreen --> ResultViewModel
        ResultScreen --> GoalReachedCard
    end
    subgraph Domain
        LiveRouteTrackingViewModel --> FinishRunUseCase
        ResultViewModel --> GetFinishedRunSummaryUseCase
        FinishRunUseCase --> RunSessionRepository
        GetFinishedRunSummaryUseCase --> RunSessionRepository
    end
    SaveRunFlow[save-run] --> RunSessionRepository
```

- **Pattern**: reutilizar lo de HU-04 y sumar solo lo que falta. Misma estructura
  Screen/Content/ViewModel.
- **Frontera**: el Mapa mide, decide y confirma (HU-04); el resultado lee el resumen congelado por id;
  `save-run` lo consume con el mismo id.
- **Steering**: dirección `ui → domain ← data`; sin hex ni medidas sueltas; voseo rioplatense.

### Technology Stack

| Layer | Choice | Role |
|-------|--------|------|
| UI | Jetpack Compose + Material 3 | Pantalla de resultado, botón del aviso |
| Navegación | Navigation Compose | Ruta `result/{runId}`, `BackHandler` |
| Estado | `StateFlow` | `ResultUiState` |
| DI | Hilt | `@HiltViewModel`, use case con `@Inject` |
| Tests | JUnit 4 + fakes a mano + `kotlinx-coroutines-test`; Compose UI Test | ViewModel, formateo y `Content` |

## File Structure Plan

```
app/src/main/java/com/trazo/app/
├── domain/usecases/
│   └── GetFinishedRunSummaryUseCase.kt   # NUEVO: getFinishedSummary(id)
└── ui/
    ├── components/layout/
    │   └── GoalCelebrationOverlay.kt     # EXISTENTE: lo modifica Persona 3 (botón «Finalizar»), no esta spec
    ├── navigation/
    │   └── TrazoApp.kt                   # EXISTENTE: ruta result/{runId}, barra oculta, onContinue
    └── screens/
        ├── map/MapScreen.kt              # EXISTENTE: de Persona 3, no se toca acá
        └── result/
            ├── ResultScreen.kt           # NUEVO: ResultScreen + ResultContent stateless
            ├── ResultViewModel.kt        # NUEVO: ResultUiState + ViewModel + ResultEffect
            ├── ResultFormatter.kt        # NUEVO: distancia, tiempo y regularidad como texto
            └── components/GoalReachedCard.kt
app/src/main/res/
├── values/strings_finish_run.xml         # NUEVO: result_* (el aviso y el diálogo ya tienen map_*)
└── drawable/ic_goal_check.xml            # NUEVO: check de la tarjeta de resultado
app/src/test/java/com/trazo/app/ui/screens/result/        # ResultViewModelTest, ResultFormatterTest
app/src/androidTest/java/com/trazo/app/ui/screens/result/ # ResultContentTest
```

Se reutiliza `rememberReduceMotion()` de `ui/theme/ReduceMotion.kt` (PR #105); esta spec no crea
otra. No hay `domain/model/result/`, `FinishPolicy`,
`CloseRunUseCase`, `DiscardRunUseCase`, `FinishRunViewModel`, `FinishRunOverlay`,
`GoalReachedNotice` ni `TooShortRunDialog`.

### Modified Files

- `ui/navigation/TrazoApp.kt` — reemplaza el `TODO` de `onNavigateToResults` por
  `navController.navigate("$RESULT_ROUTE/${summary.id}") { launchSingleTop = true }`, agrega
  `composable("$RESULT_ROUTE/{runId}")`, oculta la barra inferior en esa ruta y conecta «Continuar»
  (`onContinue(runId)`). Archivo de esta spec; Persona 3 no lo toca.
  `TrazoDestination` no cambia (`result` no es pestaña).
- `res/values/strings.xml` — sin cambios; los textos nuevos van en `strings_finish_run.xml`.

## System Flows

```mermaid
stateDiagram-v2
    [*] --> Recording
    Recording --> GoalNotice: distancia mayor o igual a la meta (una vez)
    GoalNotice --> Recording: Seguir corriendo
    GoalNotice --> Deciding: Finalizar (Persona 3)
    Recording --> Deciding: Finalizar de RunControls
    Deciding --> TooShortDialog: menos de 200 m
    Deciding --> Confirm: 200 m o más (texto según goalReached)
    TooShortDialog --> Recording: volver o cerrar
    TooShortDialog --> Idle: descartar
    Confirm --> Recording: cancelar o cerrar
    Confirm --> Result: Finalizar
    Result --> SaveRun: Continuar (onContinue, ruta de save-run)
```

```mermaid
sequenceDiagram
    participant U as Corredor
    participant VM as LiveRouteTrackingViewModel
    participant R as RunSessionRepository
    participant App as TrazoApp
    participant RVM as ResultViewModel
    U->>VM: onConfirmFinish
    VM->>R: finishSession (via FinishRunUseCase)
    R-->>VM: FrozenRunSummary (ya guardado, sesión Idle)
    VM-->>App: NavigateToResults(summary)
    App->>RVM: navigate result/{summary.id}
    RVM->>R: getFinishedSummary(id)
    R-->>RVM: FrozenRunSummary?
```

**Decisiones de flujo**

- **Se decide al tocar, se resuelve al confirmar.** El diálogo se elige con la distancia al tocar
  «Finalizar». El título del resultado sale de `goalReached` del resumen congelado. Si el corredor
  cruza la meta con el diálogo «Todavía no llegaste…» abierto, ve «¡Meta cumplida!» igual; aceptable.
- **El aviso de meta se muestra una vez al cruzar la meta** (el ViewModel recuerda
  `previousGoalReached`) y se descarta con «Seguir corriendo» o «Finalizar». Mientras sigue visible
  `suppressed` es `true`; al descartarlo, vuelve a `false`. Esto resuelve la pregunta abierta del
  diseño anterior (el aviso no bloquea las alertas para siempre). «Finalizar» de `RunControls` está
  siempre disponible, así que 1.3 se cumple.
- **«Finalizar» desde cualquier estado**: `canFinish` ya lo permite en `ACTIVE`, `PAUSED`,
  `PERSONAL_PAUSED` y `STOPPED`, y `finishSession()` usa `requireActiveSession()` sin validar el
  estado.
- **`finishSession()` ya deja `Idle`**: no hay `resetToIdle()` posterior.
- **Por navegación viaja solo el id** (`summary.id`). Hoy el efecto lleva el resumen completo; `TrazoApp`
  usa solo su `id` y descarta el resto.
- **Descartar** (recorrido corto) usa `resetToIdle()` sin `finishSession()`: no queda resumen, así
  que no hay nada que limpiar.
- **El resultado solo lee.** No llama a `removeFinishedSummary(id)`; lo hace `save-run` al guardar o
  descartar. Mientras tanto el resumen sigue disponible por id.
- **«Atrás» en el resultado no hace nada** (`BackHandler` sin acción), sin flecha de volver y con la
  barra inferior oculta: el recorrido no está guardado y se perdería.

## Requirements Traceability

| Req | Resumen | Cubierto por | Qué agrega esta spec |
|-----|---------|--------------|----------------------|
| 1.1 | Aviso animado con «Finalizar» | `GoalCelebrationOverlay` + ViewModel | — (botón «Finalizar»: Persona 3) |
| 1.2 | Sigue corriendo y registrando | ViewModel (no detiene nada) | — |
| 1.3 | «Finalizar» disponible | `RunControls` + botón del aviso (Persona 3) | — |
| 1.4 | Sin aviso si no hay meta | ViewModel (`goalReached`) | — |
| 1.5 | Sin animación con «Quitar animaciones» | `rememberReduceMotion()` | — |
| 2.1–2.4 | Confirmación con meta | `FinishConfirmDialog`, ViewModel | Resultado «¡Meta cumplida!» (5.2) |
| 3.1–3.4 | Confirmación sin meta | `FinishConfirmDialog`, ViewModel | Resultado «Recorrido finalizado» (5.3) |
| 4.1–4.5 | Recorrido corto | `AlertDialog` de `MapScreen`, `onDiscardShortRun` | — |
| 4.6 | Sin «Guardar» si < 200 m | `onFinishRequested()` (guarda) | Test de regresión del resultado (ver tareas) |
| 5.1 | Distancia, tiempo, regularidad, «Continuar» | — | `ResultContent`, `ResultFormatter` |
| 5.2–5.4 | Título con/sin animación | — | `GoalReachedCard` |
| 5.5 | Valores sin recalcular | — | `ResultFormatter` solo formatea `metrics` |
| 5.6 | No registra en el resultado | `finishSession()` ya detuvo | — |
| 6.1 | «Continuar» abre «Guardar» | — | `onContinue(runId)`; `saverun/{runId}` es de `save-run` |
| 6.2 | Conserva métricas y meta | `FrozenRunSummary` por id | Frenadas y regularidad: Open Questions |
| 6.3 | Inicio, demo, traza | `FrozenRunSummary` (`startedAt`, `isDemoMode`, `routePoints`) | — |
| 6.4 | Meta alcanzada o no | `goalReached` | — |
| 6.5 | Título, destacados y fotos | `save-run` | — |
| 7.1 | Sin alerta de frenada con cierre visible | `suppressed` (productor) | Consumidor: bloqueado por `stop-detection` |
| 7.2 | La frenada se registra igual | `stop-detection` | — |
| 7.3 | Consulta pendiente → frenada | — | Bloqueado por `stop-detection` |

## Components and Interfaces

### Domain

#### GetFinishedRunSummaryUseCase

```kotlin
class GetFinishedRunSummaryUseCase @Inject constructor(
    private val repository: RunSessionRepository,
) {
    operator fun invoke(id: String): FrozenRunSummary? = repository.getFinishedSummary(id)
}
```

Sigue el patrón de `FinishRunUseCase`. Solo lee: **nunca limpia** el resumen (no hay `remove`).

### UI (Mapa): a cargo de Persona 3

El botón «Finalizar» de `GoalCelebrationOverlay`, su conexión al diálogo de confirmación y el
reintento ante fallo de `finishSession()` los implementa Persona 3 (HU-04). Esta spec no define su
firma ni toca esos archivos.

### UI (Resultado)

```kotlin
data class ResultUiState(
    val isLoaded: Boolean = false,
    val isGoalReached: Boolean = false,
    val goal: RunGoal? = null,              // domain.model.map.RunGoal
    val distanceText: String = "",
    val timeText: String = "",
    val regularityText: String? = null,     // null = mostrar «No disponible»
)

sealed interface ResultEffect { data object LeaveToMap : ResultEffect }

@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,     // runId de result/{runId}
    getFinishedRunSummary: GetFinishedRunSummaryUseCase,
    formatter: ResultFormatter,
) : ViewModel()
```

- Lee `runId`, llama a `getFinishedRunSummary(runId)` y mapea a `ResultUiState` con
  `summary.metrics.distanceMeters` y `summary.metrics.elapsedTimeSeconds`. No recalcula (5.5).
  `regularityText` queda en `null` (el resumen no tiene regularidad) y la pantalla muestra «No
  disponible».
- Si devuelve `null` (por ejemplo, la app se recreó) emite `LeaveToMap`.
- Ignora `isDemoMode`.
- `ResultFormatter` (puro, `es-AR`): distancia con coma decimal y «km» a partir de **metros**
  (`5200.0 m → 5,2 km`), tiempo `HH:MM:SS` desde segundos, regularidad `80 %`. El ViewModel del
  Mapa tiene formateadores privados (`formatDistance` con 2 decimales, `formatElapsedTime`); no se
  tocan.

`ResultScreen(onContinue: (runId: String) -> Unit, onLeave)` (`onContinue` es el callback que
`TrazoApp` conecta a `saverun/{runId}` cuando exista): conecta el ViewModel, consume
`LeaveToMap` y registra `BackHandler(enabled = true) {}`. `ResultContent(state, onContinue)`: fondo
`primary` y `GoalReachedCard` centrada; botón «Continuar» filled a todo el ancho.

`GoalReachedCard`: con meta cumplida, check de `ic_goal_check` con `scale` (spring) + `fadeIn` una
vez, título «¡Meta cumplida!» y «Completaste tu meta de X.» con `goalLabel(goal)`; sin meta, mismo
layout sin check ni animación, título «Recorrido finalizado» y marca «Incompleto». Con
`rememberReduceMotion()` (de `ui/theme/ReduceMotion.kt`) el check se dibuja estático. Recuadros: Distancia, Tiempo y Regularidad
(«No disponible» sin dato). Frenadas no se muestra (5.1 no la incluye). Título con `heading()`.

### Navegación

- `TrazoApp`: `const val RESULT_ROUTE = "result"` y `composable("$RESULT_ROUTE/{runId}")` con
  `navArgument("runId") { type = NavType.StringType }`.
- `onNavigateToResults = { summary -> navController.navigate("$RESULT_ROUTE/${summary.id}") { launchSingleTop = true } }`.
- Barra inferior oculta cuando `currentRoute` empieza con `RESULT_ROUTE`.
- `onContinue(runId)` navega a `saverun/$runId`. La ruta `saverun` es de `save-run` y todavía **no
  existe**: hasta que exista, «Continuar» usa un parche temporal aprobado por Rocío (tarea 5.3) y
  vuelve al Mapa con la misma navegación de `onLeave` (lambda local en el bloque `composable`).

### Contrato pedido a otras specs

| Spec | Lo que este diseño necesita |
|------|-----------------------------|
| HU-04 (Persona 3) | Botón «Finalizar» en `GoalCelebrationOverlay` conectado al diálogo existente; reintento ante fallo de `finishSession()` sin perder la sesión; `removeFinishedSummary(id)` en `RunSessionRepository`; borrar su `rememberReduceMotion()` provisoria; mantener `getFinishedSummary(id)` y `NavigateToResults` con un resumen que tenga `id`. No tocar `TrazoApp.kt`. |
| `stop-detection` | Consumir `suppressed` y resolver la consulta pendiente como frenada (7.3). Sin código en `main` todavía. Aportar las frenadas a `save-run`. |
| `save-run` (HU-07) | Definir la ruta `saverun/{runId}`, leer con `getFinishedSummary(id)` y llamar `removeFinishedSummary(id)` al terminar de guardar o al descartar. |
| Rocío (PR #105) | `rememberReduceMotion()` en `ui/theme/ReduceMotion.kt`. |

## Data Models

- Esta spec no define modelos nuevos: usa `FrozenRunSummary` y `RunMetrics`.
- `goalReached` del resumen es la única fuente del título del resultado y del indicador de `save-run`.
- Por navegación viaja solo `runId`; la traza (`routePoints`) no se pasa por argumentos.

## Error Handling

| Escenario | Respuesta | UI |
|-----------|-----------|----|
| `finishSession()` falla | Hoy `onConfirmFinish()` no maneja el fallo. Persona 3 lo corrige (HU-04) para poder reintentar sin perder la sesión | Fuera de esta spec |
| Resultado sin datos (`getFinishedSummary` devuelve `null`) | `LeaveToMap` | Vuelve al Mapa |
| Doble toque en «Finalizar recorrido» | No hay `isFinishing`; el segundo `finishSession()` falla con «no hay sesión» | Sin efecto visible |
| Regularidad ausente | `regularityText = null` | «No disponible» |
| `saverun` sin ruta | Parche temporal (5.3) | «Continuar» vuelve al Mapa hasta que exista |

## Testing Strategy

### Unit Tests (JVM)

- `ResultFormatterTest`: `5200.0 m → 5,2 km`, `1930 s → 00:32:10`, `80 → 80 %`.
- `ResultViewModelTest` con un fake de `RunSessionRepository`: con meta, sin meta, regularidad
  ausente, id desconocido → `LeaveToMap`; usa `metrics.distanceMeters` sin recalcular; ignora
  `isDemoMode`.
- Ya cubierto en `LiveRouteTrackingViewModelTest` (sin cambios): diálogos por distancia y `suppressed`.
  Agregar solo si falta: `onConfirmFinish` emite `NavigateToResults` con el resumen del repositorio.

### UI Tests (`androidTest`)

- `ResultContent`: ambos títulos, métricas, «No disponible», sin check en «Recorrido finalizado»,
  «Continuar» invoca el callback con el id.
- Accesibilidad: fuente al 200 % sin recortes; estado comunicado con texto e ícono.

## Security Considerations

Sin cuentas ni red. Por navegación viaja solo el id; la traza queda en el repositorio en memoria. No
se registra la ubicación en logs.

## Performance & Scalability

- La traza no viaja por navegación; el resumen se lee por id.
- `finishedSummaries` se limpia con `removeFinishedSummary(id)`, que llama `save-run`; hasta entonces el
  resumen queda en memoria.

## Open Questions / Risks

- **Resuelto con Persona 3:** el botón «Finalizar» del aviso y el reintento ante fallo de
  `finishSession()` son de HU-04; `removeFinishedSummary(id)` lo agrega ella y lo llama `save-run`.
  Riesgo residual: hasta que `save-run` exista, los resúmenes quedan en memoria.
- **Frenadas y regularidad (6.2) no están en `FrozenRunSummary`.** Dependen de `stop-detection` y
  HU-05.2. El resultado muestra «No disponible» en regularidad y no muestra frenadas; `save-run`
  necesita otra fuente.
- **Prioridad del cierre (7.1–7.3)**: `suppressed` ya está, pero `stop-detection` no tiene código en
  `main`. Queda bloqueado hasta que exista su overlay.
- **Formato de distancia**: el panel del Mapa muestra 2 decimales; el resultado usa 1 (`5,2 km`).
  Alinear con el equipo de diseño.
- **`TODO (Persona 4)` en `TrazoApp`**: acordado que la ruta `result/{runId}` y el cableado son de
  esta spec; Persona 3 no toca ese archivo.
- **Dos `RunGoal`** (`domain.model` con `kilometers: Int` y `domain.model.map` con `distanceKm:
  Double`): se usa el de `map`. Riesgo de confusión al importar.
- **Ruta `saverun/{runId}` inexistente** (es de `save-run`): «Continuar» vuelve al Mapa (parche temporal 5.3, aprobado por Rocío) hasta que exista.
