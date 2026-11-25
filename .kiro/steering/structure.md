# Project Structure

_updated_at: 2026-11-20_

## Organization Philosophy

Un solo módulo Gradle (`app`) organizado **por capas** (`domain`, `data`, `ui`, `di`) y, dentro de
`ui/screens`, **por pantalla**. Paquete base: `com.trazo.app`. Carpetas, archivos y código en
inglés; textos de la app, comentarios y documentación de specs en español.

La organización sigue el `README.md` de la cátedra (raíz del repo) y la guía oficial de arquitectura
de Android (developer.android.com/topic/architecture): la lógica de negocio vive en Domain y **la
interfaz de cada repositorio vive en Data**, junto a su implementación, y devuelve modelos de Domain.

## Directory Patterns

### Dominio

**Location**: `app/src/main/java/com/trazo/app/domain/`
**Purpose**: lógica de negocio en Kotlin puro, sin Android, Hilt, Room ni Retrofit. Se permite
`javax.inject.@Inject` (anotación estándar, no de Hilt) en los constructores. Tres paquetes:

- `model/<área>/`: modelos de negocio agrupados por área (por ejemplo `model/stops/`). **Los
  modelos tienen comportamiento** (no anémicos): las reglas propias de un modelo son métodos que
  devuelven una copia nueva o lanzan `IllegalStateException` (por ejemplo
  `RunSession.ActiveSession.pause(nowMillis)`, `addPoint`, `toFrozenSummary`). Los cálculos y
  reglas puras sobre un modelo van junto a él (por ejemplo `RoutePoint.distanceMetersTo`,
  `RoutePoint.isAccurate()` y `GpsSignalRules`).
- `services/`: lógica de dominio reutilizable que no es un caso de uso: reductores, estimadores y
  **monitores**. Un monitor orquesta un sensor y la sesión para detectar algo y lo expone, sin
  decidir qué se guarda (por ejemplo `StopMonitor` / `SensorStopMonitor`, que emite
  `closedStops`, y `RegularityMonitor`). Se usa una interfaz solo si los consumidores necesitan un
  fake en sus tests (`StopMonitor`); si no, es una clase concreta (`RegularityMonitor`).
- `usecases/`: solo casos de uso, es decir acciones de la app; se crean solo si agregan lógica (un
  ViewModel consume el repositorio directo si solo pasaría la llamada). Las decisiones van acá y
  el use case llama al repositorio solo cuando corresponde (por ejemplo `SaveStopUseCase` guarda
  solo las frenadas; `RecordStopsUseCase` lo conecta con `StopMonitor.closedStops` desde
  `TrazoApplication`).

### Datos

**Location**: `app/src/main/java/com/trazo/app/data/`
**Purpose**: acceso a las fuentes de datos. Tres tipos de paquete:

- `repository/`: **interfaz e implementación** de cada repositorio, juntas (por ejemplo
  `RunSessionRepository` / `RunSessionRepositoryImpl`, `BrakeRepository` /
  `InMemoryBrakeRepository`, `LocationRepository` / `LocationRepositoryImpl`). Un repositorio es
  «bobo»: recibe, guarda y expone datos, sin reglas propias (las reglas son del modelo). Devuelve
  modelos de `domain.model`.
- Un paquete por fuente, con su interfaz y su adaptador de Android: `location/` (`LocationClient`,
  `LocationStatusReader`), `sensors/` (`MotionSource`, `MotionSensorAvailability`), `clock/`
  (`MonotonicClock`), `tiles/` (`CartoTileStreamProvider`, `CartoTileUrl`: tiles del mapa por
  HTTP) y, cuando existan, `local/` (Room), `preferences/` y `camera/`.
- `di/`: módulos Hilt que enlazan las interfaces de `data` con sus implementaciones.

### Inyección

**Location**: `app/src/main/java/com/trazo/app/data/di/` y `app/src/main/java/com/trazo/app/di/`
**Purpose**: módulos Hilt con `@Binds`/`@Provides` y scopes (`@Singleton`, `@ApplicationScope`).

- `data/di/`: enlaces de repositorios y fuentes de datos (solo importa `data` y `domain.model`).
- `di/`: scope de la app (`ApplicationScope`) y clases de `domain` que necesitan scope o
  parámetros (por ejemplo `MonitorModule` provee `StopMonitor` y `RegularityMonitor`). En `domain`
  no hay anotaciones de Hilt: lo que necesita scope se provee desde acá con `@Provides`.

### Pantallas

**Location**: `ui/screens/<pantalla>/` (`map` = pestaña Actividad, `profile` = pestaña
Perfil, `result`, `saverun`, `settings`, `startrun`)
**Purpose**: tres piezas por pantalla:

- `<Nombre>Screen.kt`: conecta el ViewModel; debajo, en el mismo archivo, `<Nombre>Content`
  stateless (recibe `UiState` y callbacks).
- `<Nombre>ViewModel.kt`: `<Nombre>UiState` arriba y el ViewModel `@HiltViewModel` debajo.
- `components/`: componentes que usa solo esa pantalla.

```kotlin
@Composable
fun ProfileScreen(onSettingsClick: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileContent(state = state, onSettingsClick = onSettingsClick)
}
```

### Componentes globales

**Location**: `ui/components/layout/` (marcos que reciben `content`) y `ui/components/common/`
(piezas sueltas). **Purpose**: componentes que usan 2+ pantallas. **Todo componente nace en
`screens/<pantalla>/components/`** y se mueve acá cuando lo necesita una segunda pantalla.

### Navegación y tema

- `ui/navigation/`: `TrazoApp` (Scaffold + NavHost), `BottomNavBar`, `TrazoDestination`. Es lo único
  que conoce todas las pantallas.
- `ui/theme/`: `Color`, `Type`, `Shape`, `Dimens` (`Spacing`, `Sizes`), `Theme` y utilidades de UI
  compartidas (por ejemplo «Quitar animaciones»).

### Recursos y tests

- `res/drawable/`: prefijos `ic_` (íconos), `img_` (ilustraciones), `ic_nav_*_filled|outlined`.
- `res/values/strings.xml`: claves con prefijo de pantalla (`profile_`, `nav_`, `map_`…).
- `app/src/test/` y `app/src/androidTest/` replican el paquete del código probado.
- `design/assets/`: SVG fuente entregados por diseño.

## Naming Conventions

- **Archivos y clases**: PascalCase, archivo = clase o composable principal.
- **Funciones y propiedades**: camelCase. Composables en PascalCase.
- **Componentes**: por función (`RunCard`, `GoalChip`), no por apariencia. Prefijo `Trazo` solo para
  globales que envuelven un componente de Material 3 (`TrazoTopBar`).
- **Eventos de ViewModel**: `on<Elemento><Acción>` (`onStartRunFabClick`,
  `onStartRunOptionDismiss`).
- **Previews**: `private fun <Componente><Estado>Preview()`, envueltas en `TrazoTheme`.
- **Tests**: `<Sujeto>Test`.

## Import Organization

Imports absolutos por paquete (`com.trazo.app.ui.theme.Spacing`); sin alias. ktlint ordena los
imports.

## Code Organization Principles

- **Dirección de dependencias**: `ui` → `domain` → `data`, y también `ui` → `data` (un ViewModel
  puede usar un repositorio directo).
- `data` solo importa `com.trazo.app.domain.model.*`: nunca `domain.services` ni
  `domain.usecases`. Si un dato tiene que pasar por un caso de uso, `data` lo expone (por ejemplo
  como `Flow`) y el caso de uso lo consume.
- `domain` no importa nada de Android, Hilt ni `ui`.
- Ni los ViewModels ni los casos de uso dependen de una fuente de datos (cliente de GPS, sensor):
  siempre pasan por un repositorio (por ejemplo `LocationRepository`). `MonotonicClock` y
  `kotlin.time.Clock` son utilidades y se pueden inyectar directo.
- `ui/components` → solo `ui/theme` y `R`: no conocen ViewModels, rutas ni modelos de `domain`
  (usan modelos de UI como `RunSummaryUi`).
- Una pantalla **nunca** importa componentes de la carpeta de otra pantalla: si hace falta, se mueve
  a `ui/components/`.
- Componentes con state hoisting y `modifier: Modifier = Modifier` como último parámetro.
- Si una pantalla todavía no tiene componentes propios, no se crea `components/`.

Detalle visual y catálogo de componentes: `design-system.md` y `ui-components.md`.

---
_Document patterns, not file trees. New files following patterns shouldn't require updates_
