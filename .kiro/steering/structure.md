# Project Structure

_updated_at: 2026-11-20_

## Organization Philosophy

Un solo módulo Gradle (`app`) organizado **por capas** (`domain`, `data`, `ui`, `di`) y, dentro de
`ui/screens`, **por pantalla**. Paquete base: `com.trazo.app`. Carpetas, archivos y código en
inglés; textos de la app, comentarios y documentación de specs en español.

## Directory Patterns

### Dominio

**Location**: `app/src/main/java/com/trazo/app/domain/`
**Purpose**: Kotlin puro, sin Android, Hilt ni Room. Se permite `javax.inject.@Inject` (anotación
estándar, no de Hilt) en los constructores.

- `model/<área>/`: modelos de negocio agrupados por área (por ejemplo `model/stops/`). **Los
  modelos tienen comportamiento** (no anémicos): las reglas propias de un modelo son métodos que
  devuelven una copia nueva o lanzan `IllegalStateException` (por ejemplo
  `RunSession.ActiveSession.pause(nowMillis)`, `addPoint`, `toFrozenSummary`). Los cálculos puros
  sobre un modelo van como funciones de extensión junto a él (por ejemplo
  `RoutePoint.distanceMetersTo`).
- `repository/`: interfaces que implementa `data`, de dos tipos:
  - **Repositorio, «bobo»**: recibe, guarda y expone datos, sin reglas ni borrados condicionales
    (por ejemplo `BrakeRepository`, `RunSessionRepository`). Las lecturas o traducciones que
    otros necesitan sobre un repositorio van como extensiones de dominio en
    `<Nombre>RepositoryExt.kt` (por ejemplo `RunSessionRepositoryExt.kt`), no como adaptadores en
    `data`.
  - **Monitor**: lo que vigila un sensor o un proceso y expone lo que detecta (por ejemplo
    `StopMonitor`, `RegularityMonitor`, con implementaciones `Sensor<Nombre>Monitor`). Un monitor
    emite eventos (por ejemplo `StopMonitor.closedStops`), no decide qué se guarda.
- `services/`: lógica de dominio reutilizable que no es un caso de uso: reductores, estimadores y
  políticas (por ejemplo `StopDetector`, `StillnessEstimator`, `RegularityMeter`,
  `GpsPointFilterPolicy`). Son clases puras que pueden usar `data` y los casos de uso.
- `usecases/`: solo casos de uso, es decir acciones de la app; se crean solo si agregan lógica (un
  ViewModel consume el repositorio directo si solo pasaría la llamada). Las decisiones van acá y
  el use case llama al repositorio solo cuando corresponde (por ejemplo `SaveStopUseCase` guarda
  solo las frenadas; `RecordStopsUseCase` lo conecta con `StopMonitor.closedStops` desde
  `TrazoApplication`).

### Datos

**Location**: `app/src/main/java/com/trazo/app/data/`
**Purpose**: adaptadores por fuente: `sensors/`, `location/`, `local/` (Room), `preferences/`,
`camera/`, y `repository/` con las implementaciones de los contratos de `domain`.

### Inyección

**Location**: `app/src/main/java/com/trazo/app/di/` (o `data/di/` junto al adaptador)
**Purpose**: módulos Hilt con `@Binds` y scopes (`@ApplicationScope`).

### Pantallas

**Location**: `ui/screens/<pantalla>/` (`home`, `map`, `history`, `result`, `saverun`, `settings`,
`startrun`)
**Purpose**: tres piezas por pantalla:

- `<Nombre>Screen.kt`: conecta el ViewModel; debajo, en el mismo archivo, `<Nombre>Content`
  stateless (recibe `UiState` y callbacks).
- `<Nombre>ViewModel.kt`: `<Nombre>UiState` arriba y el ViewModel `@HiltViewModel` debajo.
- `components/`: componentes que usa solo esa pantalla.

```kotlin
@Composable
fun HomeScreen(onStartRunClick: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state = state, onStartRunFabClick = viewModel::onStartRunFabClick, /* … */)
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
- `res/values/strings.xml`: claves con prefijo de pantalla (`home_`, `nav_`, `map_`…).
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

- `ui/screens` → `domain`, `ui/components`, `ui/theme`. Nunca `data`.
- `ui/components` → solo `ui/theme` y `R`: no conocen ViewModels, rutas ni modelos de `domain`
  (usan modelos de UI como `RunSummaryUi`).
- Una pantalla **nunca** importa componentes de la carpeta de otra pantalla: si hace falta, se mueve
  a `ui/components/`.
- `domain` no importa nada de Android, `data` ni `ui`; `data` implementa los contratos de `domain`.
- `data` **no depende de `domain/usecases`**: puede usar modelos, `services/` y las interfaces de
  `repository/`. Si un dato de `data` tiene que pasar por un caso de uso, `data` lo expone (por
  ejemplo como `Flow`) y el caso de uso lo consume; nunca al revés.
- Componentes con state hoisting y `modifier: Modifier = Modifier` como último parámetro.
- Si una pantalla todavía no tiene componentes propios, no se crea `components/`.

Detalle visual y catálogo de componentes: `design-system.md` y `ui-components.md`.

---
_Document patterns, not file trees. New files following patterns shouldn't require updates_
