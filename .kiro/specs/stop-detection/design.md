# Documento de diseño · stop-detection (HU-05.1 · Frenadas y pausas personales)

## Overview

**Propósito**: esta funcionalidad entrega al corredor la detección automática de sus detenciones durante un recorrido en curso, distingue la frenada propia del recorrido (semáforo, esquina) de la pausa personal y deja registrada la ubicación de cada frenada.
**Usuarios**: el corredor recreativo urbano que registra un recorrido con Trazo. Ve una alerta animada a los 8 s de quietud y, si la detención supera los 20 s, un mensaje para clasificarla.
**Impacto**: hoy `domain/` y `data/` están vacíos y `MapScreen` es un placeholder. El diseño agrega un núcleo de dominio puro (máquina de estados con reloj inyectado), un adaptador del acelerómetro, un repositorio que orquesta ambos y una capa de UI (`BrakeAlert`, `StopClassificationDialog`) montada como superposición sobre `MapScreen`, sin tocar el futuro `MapViewModel` de HU-04.

### Goals
- Detectar quietud con el acelerómetro y mantener los estados «Movimiento continuo» y «Frenada» (1.1–1.4, 2.1).
- Registrar una única frenada por detención continua con su ubicación y conservarla al retomar el movimiento (2.2–2.4).
- Mostrar la alerta animada accesible y cerrarla a los 4 s (3.1–3.5).
- Consultar la clasificación ante una quietud de más de 20 s y aplicar la respuesta (4.1–4.7).
- Dejar la lógica de negocio (8 s, 20 s, histéresis) testeable con JUnit y tiempo virtual, sin Android.

### Non-Goals
- Pausar o reanudar el recorrido, medir la regularidad del piso y gestionar el ciclo de vida del sensor (HU-05.2).
- Conteo y presentación de frenadas en resultados, guardado e historial (`save-run`, HU-06).
- Pin de frenada en el mapa (queda para HU-04; esta spec solo expone la lista de frenadas con ubicación).
- Persistencia de las frenadas y servicio en primer plano para el rastreo (HU-04).

## Boundary Commitments

### This Spec Owns
- La regla de negocio de detención: umbral de quietud, 8 s, 20 s (estricto), confirmación de reanudación y exclusión de pausas personales.
- El modelo `StopDetectionState` / `BrakeEvent` y la lista de frenadas del recorrido en curso (autoridad única hasta el fin del recorrido).
- Los puertos `MotionSource`, `StopLocationProvider`, `RunningRunSource`, `MonotonicClock` y el contrato de salida `StopDetectionRepository`.
- La lectura del acelerómetro mientras hay un recorrido en curso (`AccelerometerMotionSource`).
- La UI de la alerta de frenada y del mensaje de clasificación, y la utilidad `rememberReduceMotion()`.

### Out of Boundary
- Estado del recorrido (`RunState`), pausa/reanudación, ubicación y trazado: pertenecen a HU-04 y HU-05.2.
- Qué se hace con las frenadas al finalizar (contarlas, guardarlas, mostrarlas): `save-run`, HU-06.
- Servicio en primer plano y permisos de ubicación.
- Medición de regularidad del piso (HU-05.2) aunque comparta el acelerómetro.

### Allowed Dependencies
- `domain` no importa nada de Android, de `data` ni de `ui`. `data` implementa los puertos de `domain`. `ui/screens` depende de `domain` (nunca de `data`), `ui/components` solo de `ui/theme` (steering `design-system.md`).
- **Dependencia dura con HU-04 (P0)**: los adaptadores `RunSessionRunningRunSource` y `RunSessionStopLocationProvider`, el módulo Hilt y el montaje en `MapScreen` requieren `RunSessionRepository` (`activeSession`, `RunState.RUNNING`, `routePoints`) y `@ApplicationScope` de la rama `origin/HU-04-map`. El dominio, el sensor, el repositorio y la UI con previews no dependen de HU-04.
- Android framework: `SensorManager`, `Settings.Global`. Sin dependencias nuevas de producción; en pruebas, `kotlinx-coroutines-test`.

### Revalidation Triggers
- Cambio de forma de `StopDetectionState`, `BrakeEvent` o `StopLocation` (consumen `save-run` y HU-06).
- Que HU-05.2 tome la propiedad del registro/baja del acelerómetro o cambie su frecuencia.
- Que HU-04 cambie `RunSession.id`, la semántica de `RunState.RUNNING` / `PAUSED` o el origen de `routePoints`.
- Que `save-run` lea las frenadas en un momento distinto a «antes de iniciar el siguiente recorrido».
- Que se pida detección con pantalla apagada (exige servicio en primer plano; ver Riesgos).

## Architecture

### Existing Architecture Analysis
- Arquitectura por capas `ui → domain ← data` con Hilt; `HomeViewModel` + `HomeContent` con `@Preview` es el patrón de pantalla. Un componente nace en `screens/<pantalla>/components/`, recibe estado y eventos por parámetro y lleva su `@Preview` con `TrazoTheme`.
- En esta rama no existen modelos, repositorios, sensores ni módulos Hilt de dominio. En `origin/HU-04-map` ya hay `RunSession`, `RunSessionRepository.activeSession: StateFlow<RunSession?>`, `RoutePoint` y `ApplicationScope`, que el diseño reutiliza sin copiarlos.
- Testing hoy: JUnit 4 sin coroutines-test, sin MockK. Se mantiene JUnit 4 con fakes escritos a mano.

### Architecture Pattern & Boundary Map

```mermaid
graph TB
    subgraph UI
        MapScreen --> StopDetectionOverlay
        StopDetectionOverlay --> StopDetectionViewModel
        StopDetectionOverlay --> BrakeAlert
        StopDetectionOverlay --> StopClassificationDialog
    end
    subgraph Domain
        StopDetectionRepository
        StopDetector
        StillnessEstimator
        MotionSource
        StopLocationProvider
        RunningRunSource
        MonotonicClock
    end
    subgraph Data
        StopDetectionRepositoryImpl
        AccelerometerMotionSource
        RunSessionRunningRunSource
        RunSessionStopLocationProvider
    end
    StopDetectionViewModel --> StopDetectionRepository
    StopDetectionRepositoryImpl -.implements.-> StopDetectionRepository
    StopDetectionRepositoryImpl --> StopDetector
    StopDetectionRepositoryImpl --> StillnessEstimator
    StopDetectionRepositoryImpl --> MotionSource
    StopDetectionRepositoryImpl --> StopLocationProvider
    StopDetectionRepositoryImpl --> RunningRunSource
    StopDetectionRepositoryImpl --> MonotonicClock
    AccelerometerMotionSource -.implements.-> MotionSource
    RunSessionRunningRunSource -.implements.-> RunningRunSource
    RunSessionStopLocationProvider -.implements.-> StopLocationProvider
    RunSessionRunningRunSource --> HU04RunSessionRepository
    RunSessionStopLocationProvider --> HU04RunSessionRepository
```

**Architecture Integration**:
- Patrón elegido: opción C del gap analysis (híbrida). Núcleo de dominio puro con puertos mínimos y adaptadores diferidos hasta integrar HU-04.
- Límites: el reductor `StopDetector` no tiene tiempo ni E/S propios (recibe `nowMillis` en cada observación). `StopDetectionRepositoryImpl` es el único que cruza sensor, reloj, ubicación y estado del recorrido.
- Patrones preservados: Hilt, `StateFlow`, ViewModel + `Content` stateless con `@Preview`, textos en `strings.xml`.
- Componentes nuevos: ver tabla de componentes. No se crean casos de uso intermedios: el ViewModel consume el repositorio directamente (solo hay una lectura y una acción, un caso de uso sería indirección sin valor).
- Cumplimiento de steering: `BrakeAlert` y el pin siguen `design-system.md` §6.8; la UI no importa `data/`.

### Technology Stack

| Capa | Elección / Versión | Rol en la funcionalidad | Notas |
|------|--------------------|-------------------------|-------|
| UI | Jetpack Compose (BOM 2025.04.01) + Material 3 | `BrakeAlert`, `StopClassificationDialog` | `AnimatedVisibility`, `AlertDialog`, sin dependencias nuevas |
| Presentación | `androidx.lifecycle` ViewModel + Hilt 2.60.1 | `StopDetectionViewModel` | `collectAsStateWithLifecycle` ya disponible |
| Dominio | Kotlin 2.3.21 puro + corrutinas | `StopDetector`, `StillnessEstimator` | Sin Android |
| Datos / Sensor | `SensorManager` (`TYPE_ACCELEROMETER`) | `AccelerometerMotionSource` como `callbackFlow` | Sin permiso; `SENSOR_DELAY_UI` (~16 Hz) |
| Pruebas | JUnit 4.13.2 + `kotlinx-coroutines-test` 1.10.2 (nueva, solo test) | Tiempo virtual para 8 s / 20 s | Se agrega a `libs.versions.toml` |

## File Structure Plan

### Directory Structure
```
app/src/main/java/com/trazo/app/
├── domain/
│   ├── model/stops/
│   │   ├── MovementState.kt          # enum CONTINUOUS_MOVEMENT, BRAKING
│   │   ├── StopKind.kt               # enum BRAKE, PERSONAL_PAUSE (respuesta del corredor)
│   │   ├── StopLocation.kt           # lat, lon, precisión (independiente de RoutePoint de HU-04)
│   │   ├── BrakeEvent.kt             # id, ubicación nullable, detectedAtMillis
│   │   ├── MotionSample.kt           # magnitud de aceleración + instante
│   │   ├── StopPhase.kt              # fases de la máquina de estados (sellada)
│   │   ├── StopDetectionState.kt     # fase + lista de frenadas + derivados
│   │   └── StopDetectionConfig.kt    # umbrales y tiempos (parámetros calibrables)
│   ├── repository/
│   │   ├── StopDetectionRepository.kt   # contrato de salida (state, classifyCurrentStop)
│   │   ├── MotionSource.kt              # puerto: flujo de MotionSample
│   │   ├── StopLocationProvider.kt      # puerto: última ubicación conocida
│   │   ├── RunningRunSource.kt          # puerto: id del recorrido mientras está RUNNING
│   │   └── MonotonicClock.kt            # puerto: ms monotónicos
│   └── usecases/
│       ├── StillnessEstimator.kt     # ventana deslizante -> ¿quieto?
│       └── StopDetector.kt           # reductor puro de la máquina de estados
├── data/
│   ├── sensors/AccelerometerMotionSource.kt   # SensorManager -> Flow<MotionSample>
│   ├── repository/
│   │   ├── StopDetectionRepositoryImpl.kt     # orquestación, reloj y StateFlow
│   │   ├── RunSessionRunningRunSource.kt      # adapta RunSessionRepository (requiere HU-04)
│   │   └── RunSessionStopLocationProvider.kt  # último routePoint (requiere HU-04)
│   └── di/StopDetectionModule.kt              # binds + CoroutineScope + MonotonicClock (requiere HU-04)
└── ui/
    ├── theme/ReduceMotion.kt                  # rememberReduceMotion(): utilidad compartida
    └── screens/map/
        ├── StopDetectionViewModel.kt          # estado de UI, cierre de alerta a los 4 s
        └── components/
            ├── BrakeAlert.kt
            ├── StopClassificationDialog.kt
            └── StopDetectionOverlay.kt        # Composable con ViewModel + Content stateless
app/src/main/res/drawable/ic_brake.xml
app/src/test/java/com/trazo/app/... (ver Testing Strategy)
```

### Modified Files
- `ui/screens/map/MapScreen.kt` — monta `StopDetectionOverlay` sobre el contenido; se hace en la tarea de integración con HU-04 para no chocar con su `MapScreen`.
- `res/values/strings.xml` — textos de alerta y de diálogo.
- `gradle/libs.versions.toml` y `app/build.gradle.kts` — `kotlinx-coroutines-test` como `testImplementation`.

## System Flows

```mermaid
stateDiagram-v2
    [*] --> Moving
    Moving --> Still: sample quieto
    Still --> Moving: movimiento sostenido
    Still --> Braking: quieto 8 s
    Braking --> Moving: movimiento sostenido
    Braking --> BrakingAsked: quieto mas de 20 s
    BrakingAsked --> Moving: movimiento sostenido
    BrakingAsked --> BrakingConfirmed: elige Frenada
    BrakingAsked --> PersonalPause: elige Pausa personal
    BrakingConfirmed --> Moving: movimiento sostenido
    PersonalPause --> Moving: movimiento sostenido
```

En el código `Braking`, `BrakingAsked` y `BrakingConfirmed` son una sola fase `StopPhase.Braking` con `query: NotDue | Pending | Answered`; `Still` y `PersonalPause` llevan `sinceMillis`.

**Decisiones de flujo**
- **Tiempos desde el inicio de la quietud**: tanto los 8 s como los 20 s se miden desde `sinceMillis`, el primer instante en que el estimador dijo «quieto» (no desde que apareció la alerta). Frenada si `now - since >= 8000`; consulta si `now - since > 20000` (estricto, 4.1).
- **Movimiento sostenido**: salir de una fase de quietud exige `resumeConfirmMillis` (1500 ms) de lectura «en movimiento» continua. Un solo pico de ruido en un semáforo no parte una detención en dos frenadas (2.3) ni cancela la cuenta de 8 s; un tramo en movimiento menor a 8 s antes de la frenada vuelve a «Movimiento continuo» sin registro (1.3).
- **Pausa personal**: elimina la frenada de la lista (4.3, 4.4) y la fase pasa a `PersonalPause`, que impide volver a registrar la misma detención. El estado expuesto sigue siendo «Movimiento continuo».
- **Recorrido que deja de estar RUNNING** (pausa, fin): la fase vuelve a `Moving`, la consulta pendiente se cierra, se deja de leer el sensor y **la lista de frenadas se conserva**. Se vacía solo cuando arranca un recorrido con distinto id (no al reanudar el mismo).

## Requirements Traceability

| Req | Resumen | Componentes | Interfaces | Flujos |
|-----|---------|-------------|------------|--------|
| 1.1 | Mantener «Movimiento continuo» en movimiento | StillnessEstimator, StopDetector | `advance`, `StopDetectionState.movement` | Moving |
| 1.2 | Sin frenada en movimiento continuo | StopDetector | `advance` (solo `Still`→`Braking` registra) | Moving, Still |
| 1.3 | <8 s y retoma: sin frenada | StopDetector | `advance`, `resumeConfirmMillis` | Still → Moving |
| 1.4 | Sin recorrido en curso no detecta | StopDetectionRepositoryImpl, RunningRunSource, AccelerometerMotionSource | `RunningRunSource.runningRunId`, `flatMapLatest` | cierre por fin de recorrido |
| 2.1 | A los 8 s pasa a «Frenada» | StopDetector | `advance`, `brakeAfterMillis` | Still → Braking |
| 2.2 | Registrar ubicación | StopDetector, StopLocationProvider | `BrakeEvent.location`, `StopLocationProvider.lastKnown` | Still → Braking |
| 2.3 | Una frenada por detención continua | StopDetector | Fase `Braking`/`PersonalPause` + `resumeConfirmMillis` | Braking, PersonalPause |
| 2.4 | Conservar al retomar | StopDetector, StopDetectionRepositoryImpl | `StopDetectionState.brakes` | Braking → Moving |
| 3.1 | Alerta «Frenada detectada» / «Semáforo o intersección» | BrakeAlert, StopDetectionViewModel | `StopDetectionUiState.alert` | alerta |
| 3.2 | Sin animación con «Quitar animaciones» | ReduceMotion, BrakeAlert | `rememberReduceMotion()` | alerta |
| 3.3 | Anunciar a accesibilidad | BrakeAlert | `liveRegion = Polite` | alerta |
| 3.4 | Texto + ícono, no solo color | BrakeAlert, `ic_brake.xml` | — | alerta |
| 3.5 | Cierre a los 4 s | StopDetectionViewModel | `ALERT_DURATION_MILLIS` | alerta |
| 4.1 | Consulta a >20 s desde la quietud | StopDetector, StopClassificationDialog | `pendingClassificationBrakeId`, `classifyAfterMillis` | Braking → BrakingAsked |
| 4.2 | «Frenada» mantiene el registro | StopDetector, StopDetectionRepositoryImpl | `classifyCurrentStop(BRAKE)` | BrakingAsked → BrakingConfirmed |
| 4.3 | «Pausa personal» no es frenada | StopDetector | `classifyCurrentStop(PERSONAL_PAUSE)` | BrakingAsked → PersonalPause |
| 4.4 | Excluir ubicación | StopDetector | `brakes` sin la frenada descartada | BrakingAsked → PersonalPause |
| 4.5 | Cerrar al elegir | StopDetectionViewModel | `pendingClassificationBrakeId == null` | — |
| 4.6 | Texto legible al 200 %, 48 dp | StopClassificationDialog | `Sizes.minTouchTarget` | — |
| 4.7 | Cierre si retoma sin responder | StopDetector | Movimiento sostenido desde `Braking(query = Pending)` | BrakingAsked → Moving |

## Components and Interfaces

| Componente | Capa | Intención | Req | Dependencias (criticidad) | Contratos |
|------------|------|-----------|-----|---------------------------|-----------|
| StopDetector | Domain | Reductor puro de la máquina de estados | 1.1–1.3, 2.1–2.4, 4.1–4.4, 4.7 | StopDetectionConfig (P0) | Service, State |
| StillnessEstimator | Domain | Decide «quieto/en movimiento» con la desviación de la magnitud en una ventana | 1.1, 1.2 | StopDetectionConfig (P0) | Service |
| StopDetectionRepository | Domain | Contrato de salida: estado y clasificación | 2.4, 4.2–4.3 | — | Service, State |
| StopDetectionRepositoryImpl | Data | Orquesta sensor, reloj, ubicación y recorrido | 1.4, 2.1–2.4 | MotionSource, RunningRunSource, StopLocationProvider, MonotonicClock (P0) | Service, State |
| AccelerometerMotionSource | Data | `callbackFlow` sobre `TYPE_ACCELEROMETER` | 1.1, 1.4 | SensorManager (P0) | Service |
| RunSessionRunningRunSource / RunSessionStopLocationProvider | Data | Adaptan `RunSessionRepository` de HU-04 | 1.4, 2.2 | HU-04 `RunSessionRepository` (P0) | Service |
| StopDetectionViewModel | UI | Estado de UI y cierre de alerta | 3.1, 3.5, 4.5 | StopDetectionRepository (P0) | State |
| BrakeAlert, StopClassificationDialog, StopDetectionOverlay | UI | Presentación | 3.1–3.4, 4.1, 4.6 | `ui/theme` (P0) | — |

### Domain

#### StopDetector

| Campo | Detalle |
|-------|---------|
| Intención | Transiciones puras entre fases de detención a partir de observaciones con tiempo explícito |
| Requisitos | 1.1–1.3, 2.1–2.4, 4.1–4.4, 4.7 |

**Responsabilidades y restricciones**
- Sin reloj, sin E/S, sin corrutinas. Genera ids de frenada mediante `idGenerator: () -> String` inyectado.
- Invariantes: a lo sumo una fase activa; una fase `Braking` tiene siempre su `BrakeEvent` en `brakes`; `PersonalPause` nunca tiene `BrakeEvent` asociado; no hay dos frenadas con la misma `sinceMillis`.

**Contratos**: Service [x] / State [x]

##### Service Interface
```kotlin
data class Observation(
    val nowMillis: Long,
    val isStill: Boolean,
    val lastLocation: StopLocation?,
)

class StopDetector(
    private val config: StopDetectionConfig,
    private val idGenerator: () -> String,
) {
    fun initial(): StopDetectionState
    fun onRunStopped(state: StopDetectionState): StopDetectionState
    fun advance(state: StopDetectionState, observation: Observation): StopDetectionState
    fun classify(state: StopDetectionState, kind: StopKind): StopDetectionState
}
```
- Precondiciones: `advance` se invoca solo mientras hay un recorrido RUNNING; `nowMillis` no decrece.
- Postcondiciones: `advance` no muta `brakes` salvo en `Still → Braking` (agrega una) ; `classify` sin consulta pendiente devuelve el mismo estado (idempotente).
- `onRunStopped` devuelve fase `Moving` y conserva `brakes`.

##### State Management
```kotlin
sealed interface StopPhase {
    data object Moving : StopPhase
    data class Still(val sinceMillis: Long, val movingSinceMillis: Long? = null) : StopPhase
    data class Braking(
        val sinceMillis: Long,
        val brakeId: String,
        val query: QueryStatus,
        val movingSinceMillis: Long? = null,
    ) : StopPhase
    data class PersonalPause(val sinceMillis: Long, val movingSinceMillis: Long? = null) : StopPhase
}
enum class QueryStatus { NOT_DUE, PENDING, ANSWERED }

data class StopDetectionState(
    val phase: StopPhase = StopPhase.Moving,
    val brakes: List<BrakeEvent> = emptyList(),
) {
    val movement: MovementState        // BRAKING solo en StopPhase.Braking
    val pendingClassificationBrakeId: String?  // brakeId si query == PENDING
}

data class BrakeEvent(val id: String, val location: StopLocation?, val detectedAtMillis: Long)
data class StopLocation(val latitude: Double, val longitude: Double, val accuracyMeters: Float)
enum class MovementState { CONTINUOUS_MOVEMENT, BRAKING }
enum class StopKind { BRAKE, PERSONAL_PAUSE }
```
- Consistencia: `StopDetectionState` es inmutable; toda transición produce un valor nuevo.

#### StillnessEstimator

| Campo | Detalle |
|-------|---------|
| Intención | Convertir `MotionSample` en «quieto / en movimiento» sin depender de la orientación del teléfono |
| Requisitos | 1.1, 1.2 |

**Decisión de umbral (parámetro delegado por los requisitos)**: se usa la **desviación estándar de la magnitud** `|a|` del acelerómetro en una ventana de 1000 ms. La gravedad es un offset constante y se cancela, así que no hace falta `TYPE_LINEAR_ACCELERATION` (inexistente en algunos equipos) ni conocer la orientación del bolsillo o brazalete. El estado es «quieto» cuando la ventana cubre al menos 1000 ms y la desviación es menor a `stillStdDevThreshold`. Mientras la ventana no está llena, el resultado es «en movimiento» (nunca se registra una frenada sin evidencia).

```kotlin
class StillnessEstimator(private val config: StopDetectionConfig) {
    fun accept(sample: MotionSample): Boolean // true = quieto
    fun reset()
}
data class MotionSample(val timestampMillis: Long, val magnitude: Float) // m/s²
```

```kotlin
data class StopDetectionConfig(
    val stillStdDevThreshold: Float = 0.5f,   // m/s², valor inicial a calibrar en dispositivo
    val windowMillis: Long = 1_000,
    val brakeAfterMillis: Long = 8_000,        // 2.1, 1.3
    val classifyAfterMillis: Long = 20_000,    // 4.1, estricto (>)
    val resumeConfirmMillis: Long = 1_500,
    val tickIntervalMillis: Long = 500,
)
```
Valores esperables de la desviación: parado con el teléfono en mano o bolsillo ≈ 0,05–0,3 m/s²; caminar ≈ 1,5–3; trotar > 3. El umbral 0,5 deja margen sobre la quietud y debajo de la caminata; **se calibra con datos reales** (ver Riesgos).

#### StopDetectionRepository

```kotlin
interface StopDetectionRepository {
    val state: StateFlow<StopDetectionState>
    fun classifyCurrentStop(kind: StopKind)   // no-op si no hay consulta pendiente
}
```
- Contrato para `save-run`: `state.value.brakes` contiene las frenadas válidas (sin pausas personales) del último recorrido y es estable desde que el recorrido deja de estar RUNNING hasta que arranca otro recorrido con distinto id.

#### Puertos

```kotlin
interface MotionSource { fun samples(): Flow<MotionSample> }       // frío: registra el sensor al colectar
interface RunningRunSource { val runningRunId: Flow<String?> }      // id mientras RunState == RUNNING, null si no
interface StopLocationProvider { fun lastKnown(): StopLocation? }  // null si no hay fix
fun interface MonotonicClock { fun nowMillis(): Long }
```
Los puertos viven en `domain/repository/` (misma convención que `LocationClient` en HU-04). Todos son P0: sin ellos el repositorio no puede operar.

### Data

#### StopDetectionRepositoryImpl

| Campo | Detalle |
|-------|---------|
| Intención | Convertir las muestras del sensor y un tick de 500 ms en observaciones del reductor y publicar el estado |
| Requisitos | 1.4, 2.1–2.4, 4.2–4.4 |

**Responsabilidades y restricciones**
- Colecta `runningRunId`; con un id no nulo usa `flatMapLatest` para colectar `MotionSource.samples()` y un ticker (`tickIntervalMillis`), y cancela ambos cuando el id pasa a nulo (1.4: no se lee el sensor sin recorrido en curso).
- Cada muestra pasa por `StillnessEstimator`; el último booleano se reevalúa en cada tick para que los 8 s y 20 s se cumplan aunque el sensor no emita muestras nuevas. Cada observación usa `MonotonicClock.nowMillis()` (no el timestamp del `SensorEvent`) para unificar la base de tiempo.
- Si el id del recorrido difiere del último id visto, parte de `StopDetector.initial()`. Si es el mismo (reanudación), conserva `brakes`. Al pasar a nulo aplica `onRunStopped`.
- Todas las modificaciones (observaciones y `classifyCurrentStop`) usan `MutableStateFlow.update { }`, que es atómico, sin locks adicionales.
- Alcance de corrutinas: un `CoroutineScope` inyectado, vinculado a `@ApplicationScope` en el módulo Hilt.

**Contratos**: Service [x] / State [x]

**Implementation Notes**
- Integración: el módulo `StopDetectionModule` provee `MonotonicClock { SystemClock.elapsedRealtime() }`, el `CoroutineScope` y los `@Binds`. Se escribe cuando HU-04 esté integrada.
- Validación: pruebas con `runTest` y reloj falso atado a `testScheduler.currentTime`.
- Riesgos: ver «Riesgos y preguntas abiertas».

#### AccelerometerMotionSource

| Campo | Detalle |
|-------|---------|
| Intención | Exponer el acelerómetro como `Flow<MotionSample>` frío |
| Requisitos | 1.1, 1.4 |

- `callbackFlow` que registra un `SensorEventListener` con `SENSOR_DELAY_UI` al colectar y lo da de baja en `awaitClose`; calcula `magnitude = sqrt(x² + y² + z²)`. Sin permisos.
- Sin acelerómetro (`getDefaultSensor == null`): el flujo no emite y se registra un aviso en log; la detección queda inactiva sin afectar el recorrido (decisión de degradación elegante).
- Propiedad del sensor: esta spec solo lo registra mientras hay recorrido RUNNING. Si HU-05.2 centraliza la gestión del sensor, reemplaza esta clase detrás de `MotionSource` sin tocar el dominio (ver Revalidation Triggers).

#### Adaptadores de HU-04

- `RunSessionRunningRunSource`: `activeSession.map { s -> s?.takeIf { it.state == RunState.RUNNING }?.id }.distinctUntilChanged()`. Solo `RUNNING` cuenta como «en curso»; `PAUSED` no (decisión de diseño; HU-05.2 define la pausa).
- `RunSessionStopLocationProvider`: `activeSession.value?.routePoints?.lastOrNull()` convertido a `StopLocation`; `null` si no hay puntos.

### UI

#### StopDetectionViewModel

```kotlin
data class BrakeAlertUi(val brakeId: String)

data class StopDetectionUiState(
    val alert: BrakeAlertUi? = null,
    val classificationVisible: Boolean = false,
)

class StopDetectionViewModel @Inject constructor(
    private val repository: StopDetectionRepository,
) : ViewModel() {
    val uiState: StateFlow<StopDetectionUiState>
    fun onClassify(kind: StopKind)
}
```
- Muestra la alerta cuando aparece un `BrakeEvent` nuevo en `state.brakes` mientras la fase es `Braking` y la oculta a los `ALERT_DURATION_MILLIS = 4000` (3.5) o antes si la consulta pasa a pendiente o la frenada deja de existir.
- `classificationVisible = state.pendingClassificationBrakeId != null` (4.1, 4.5, 4.7). Cumple la restricción de que la alerta (8–12 s) ya no está visible cuando aparece la consulta (>20 s).

#### BrakeAlert (resumen)
Sigue `design-system.md` §6.8: `Surface` `inverseSurface`, círculo blanco de 36 dp con `ic_brake` en `primary`, título «Frenada detectada» (`titleSmall`) y detalle «Semáforo o intersección» (`bodyMedium`), `semantics { liveRegion = LiveRegionMode.Polite }` (3.3), texto + ícono (3.4). Entrada con `slideInVertically + fadeIn`; con `rememberReduceMotion() == true` usa `EnterTransition.None` / `ExitTransition.None` (3.2). Se ubica con `Modifier` desde `MapScreen` a 12 dp sobre el panel del temporizador.

#### StopClassificationDialog (resumen)
`AlertDialog` con título de pregunta, botones «Frenada» y «Pausa personal» (`TextButton` con `heightIn(min = Sizes.minTouchTarget)`), en el slot de botones de M3 que se envuelve en varias líneas al crecer la fuente (4.6). No se descarta por toque exterior ni por atrás (`DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)`): se cierra solo al elegir o cuando la detención termina (4.5, 4.7).

#### ReduceMotion
`@Composable fun rememberReduceMotion(): Boolean` lee `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` desde `LocalContext.current.contentResolver`. Vive en `ui/theme/ReduceMotion.kt` porque también la usará la celebración de «Meta cumplida».

## Data Models

### Domain Model
- Agregado: `StopDetectionState` (fase + lista de `BrakeEvent`). `BrakeEvent` es una entidad con id estable (UUID) y ubicación opcional.
- Reglas: una `Braking` activa ↔ un `BrakeEvent` en `brakes`; descartar por pausa personal elimina ese evento por id; las frenadas ya registradas nunca se modifican al retomar el movimiento.
- Sin persistencia: el estado vive en memoria mientras el proceso vive. Persistir frenadas pertenece a `save-run`.

## Error Handling

- **Sin acelerómetro**: flujo vacío, detección inactiva, sin interrupción del recorrido.
- **Sin ubicación al registrar** (2.2): se registra `BrakeEvent(location = null)`; la frenada cuenta igual y los consumidores deben tolerar `null` al dibujar pines. Ver Riesgos.
- **`classifyCurrentStop` sin consulta pendiente** (doble toque, cierre concurrente): no-op idempotente.
- **Recorrido que termina con consulta o alerta visibles**: la fase vuelve a `Moving`, ambos elementos desaparecen por derivación del estado.
- Errores del sensor/log: solo logging; no hay mensajes al usuario porque los requisitos no los definen.

## Testing Strategy

### Unit Tests (JUnit 4, sin Android)
- `StillnessEstimatorTest`: serie constante → quieto; serie con desviación > umbral → en movimiento; ventana incompleta (<1000 ms) → en movimiento (1.1, 1.2).
- `StopDetectorTest`: (a) quieto 7 999 ms y retoma → sin frenada, `Moving` (1.3); (b) quieto 8 000 ms → `Braking`, un `BrakeEvent` con la ubicación (2.1, 2.2); (c) 30 observaciones seguidas quieto → una sola frenada (2.3); (d) pico de movimiento de 500 ms durante la quietud → no resetea ni duplica; movimiento ≥ 1 500 ms → `Moving` con la frenada conservada (2.3, 2.4); (e) 20 000 ms exactos → sin consulta; 20 001 ms → `PENDING`, medido desde el inicio de la quietud (4.1); (f) `classify(BRAKE)` → `ANSWERED`, lista intacta (4.2); (g) `classify(PERSONAL_PAUSE)` → frenada y ubicación fuera de `brakes`, sin nuevo registro mientras sigue quieto (4.3, 4.4); (h) retoma sin responder → consulta cerrada, frenada conservada (4.7).
- `StopDetectionStateTest`: `movement` y `pendingClassificationBrakeId` derivan de la fase.

### Integration Tests (`runTest` + fakes de `MotionSource`, `RunningRunSource`, `StopLocationProvider`, reloj)
- `StopDetectionRepositoryImplTest`: sin id de recorrido no se colecta `MotionSource` y el estado no cambia (1.4); muestras quietas + tiempo virtual 8 s → una frenada con ubicación (2.1, 2.2); pausa y reanudación con el mismo id conservan `brakes`; un id distinto las vacía; fin del recorrido conserva `brakes` y cancela la colección; `classifyCurrentStop` concurrente con ticks no pierde actualizaciones.
- `StopDetectionViewModelTest`: alerta aparece al registrarse la frenada y se limpia a los 4 000 ms virtuales (3.1, 3.5); `classificationVisible` sigue a `pendingClassificationBrakeId` (4.1, 4.5, 4.7).

### UI Tests (Compose, `androidTest`)
- `BrakeAlertTest`: muestra «Frenada detectada» y «Semáforo o intersección», con ícono y `liveRegion` Polite (3.1, 3.3, 3.4).
- `StopClassificationDialogTest`: los dos botones existen, miden ≥ 48 dp de alto y no se truncan con `fontScale = 2f`; el clic dispara `onClassify` con `StopKind` correcto (4.1, 4.6).
- Verificación manual en dispositivo: «Quitar animaciones» activo → la alerta aparece sin transición (3.2); TalkBack anuncia la alerta (3.3); calibración del umbral caminando, trotando y parado en un semáforo.

## Security & Privacy
- La ubicación de las frenadas es dato personal; esta spec no lo transmite ni lo persiste. El acelerómetro no requiere permisos.

## Performance & Scalability
- ~16 muestras/s y 2 ticks/s; la ventana mantiene ~16 elementos. Costo despreciable. El sensor solo se registra mientras hay recorrido en curso (batería).

## Riesgos y preguntas abiertas
- **Calibración del umbral (riesgo medio)**: el valor 0,5 m/s² es una hipótesis. Trotar en el lugar o llevar el teléfono en brazalete puede subir la desviación; mitigación: `StopDetectionConfig` central y una tarea de calibración en dispositivo antes de dar por cerrada la spec.
- **Ejecución con pantalla apagada (riesgo alto si se exige)**: desde Android 9 las apps en segundo plano no reciben eventos de sensores continuos; solo un servicio en primer plano lo evita. Se asume que HU-04 mantiene el servicio de rastreo. Si no, la detección solo funciona con la app visible; la decisión se eleva a HU-04.
- **Frenada sin ubicación**: se registra con `location = null` si no hay fix. Alternativa descartada: demorar el registro, que violaría 2.1/2.2. Los consumidores (pin del mapa, `save-run`) deben tolerar `null`.
- **Propiedad del sensor con HU-05.2**: ambos specs leen el acelerómetro. Esta spec expone el puerto `MotionSource`; si HU-05.2 comparte la lectura, se reemplaza el adaptador sin tocar el dominio.
- **Lectura de `brakes` por `save-run`**: el contrato es «antes de que arranque otro recorrido». Si `save-run` necesita persistencia previa, debe copiar la lista al recibir el fin del recorrido.
- **Nomenclatura «Frenada» (opción y estado)**: se respetan los textos pedidos; se sugiere validar con UX.
