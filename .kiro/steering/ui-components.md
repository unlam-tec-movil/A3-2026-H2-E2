# UI Components

Patrones para construir la interfaz de Trazo con Jetpack Compose y Material 3. La guía completa, con los componentes de cada pantalla, es la "Guía de UI de Trazo"; acá van las reglas y el catálogo de componentes globales.

_updated_at: 2026-10-28_

## Dónde va cada componente

| Lugar | Qué va |
| --- | --- |
| `ui/screens/<pantalla>/components/` | Componentes que usa una sola pantalla |
| `ui/components/layout/` | Globales que arman la estructura de una pantalla (header, contenedor, título de sección) |
| `ui/components/common/` | Globales sueltos: botones, tarjetas, chips, avisos, diálogos |
| `ui/navigation/` | Lo que conoce pantallas o rutas: `TrazoApp` (NavHost), `BottomNavBar`, destinos |

- **Un componente nuevo nace en `ui/screens/<pantalla>/components/`**, aunque figure en el catálogo de globales. Se mueve a `ui/components/common/` (o `layout/`) recién cuando lo necesita otra pantalla (Mapa, Historial u otra), y en ese momento se generaliza.
- Una pantalla nunca importa componentes de la carpeta de otra pantalla.
- Un componente global no conoce pantallas, rutas, ViewModels ni modelos de `domain`.
- El catálogo de abajo indica **dónde termina** cada componente cuando ya lo comparten varias pantallas, no dónde se crea.

## Estructura de una pantalla

Cada pantalla vive en `ui/screens/<pantalla>/` con tres piezas:

- `<Nombre>Screen`: conecta el ViewModel; no dibuja nada propio.
- `<Nombre>Content`: dibuja la pantalla. Recibe `<Nombre>UiState` y callbacks; no conoce el ViewModel.
- `<Nombre>ViewModel` + `<Nombre>UiState`: estado de la pantalla.

```kotlin
@Composable
fun SaveRunScreen(
    onBack: () -> Unit,
    viewModel: SaveRunViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SaveRunContent(
        state = state,
        onTitleChange = viewModel::onTitleChange,
        onSave = viewModel::onSave,
        onBack = onBack,
    )
}

@Composable
fun SaveRunContent(
    state: SaveRunUiState,
    onTitleChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) { /* solo UI */ }
```

Por qué: `Content` se puede construir y revisar con previews antes de que exista el ViewModel, y se testea sin Android.

## Reglas de los componentes

- Reciben datos y callbacks por parámetro (state hoisting). Nunca un ViewModel.
- El último parámetro opcional es `modifier: Modifier = Modifier`, aplicado al elemento raíz.
- El aspecto (colores, tipografía, tamaños y variantes de cada componente) sigue `.kiro/steering/design-system.md`.
- Colores, tipografía y formas salen de `MaterialTheme` (`colorScheme`, `typography`, `shapes`) y de `ui/theme`. Nada de `Color(0xFF…)` ni medidas sueltas.
- Los textos visibles van en `res/values/strings.xml`.
- Cada componente y cada `Content` tiene un `@Preview` por estado relevante (vacío, cargando, error, completo), envuelto en `TrazoTheme`.
- Antes de crear un componente propio, usar el de Material 3 directo si alcanza. Se envuelve solo para aplicar el estilo de Trazo o repetir una combinación.
- Los modales de Trazo son `Dialog` centrados con fondo oscurecido; no se usa `ModalBottomSheet`.
- Los mensajes cortos usan el `Snackbar` del `Scaffold` de `TrazoApp`.

## Nombres

- Archivos y componentes en inglés y PascalCase; el archivo se llama como el componente.
- Se nombra por función (`RunCard`, `GoalSelector`), no por apariencia (`BlueBox`).
- Prefijo `Trazo` solo para globales que envuelven un componente de Material 3 con el estilo de la app (`TrazoTopBar`).
- Previews: `private fun <Componente><Estado>Preview()`.

## Catálogo de componentes globales

Usar estos antes de crear uno nuevo.

| Componente | Carpeta | Base | Uso |
| --- | --- | --- | --- |
| `BottomNavBar` | navigation | `NavigationBar` | Pestañas Inicio · Mapa · Historial |
| `TrazoApp` | navigation | `Scaffold` + `NavHost` | Esqueleto de la app, snackbars y rutas |
| `TrazoTopBar` | layout | `TopAppBar` | Header con título e ícono opcional (engranaje, volver, cerrar) |
| `ScreenContainer` | layout | `Column` | Padding, fondo y scroll comunes |
| `SectionHeader` | layout | `Text` | Título de sección |
| `PrimaryButton` | common | `Button` | Acción principal |
| `SecondaryButton` | common | `OutlinedButton` | Acción secundaria: "Cancelar" en modales, "Ir a configuración". Para "Seguir editando" se usa `TextButton` |
| `StartRunFab` | common | `FloatingActionButton` + `DropdownMenu` | Botón "+" con "Iniciar un recorrido" |
| `RunCard` | common | `Card` | Tarjeta de recorrido (Inicio, Historial) |
| `MetricItem` | common | `Column` + `Text` | Valor + etiqueta ("5,2 km / Distancia") |
| `StatusBadge` | common | `AssistChip` / `Badge` | "Demo", "Incompleto", "Meta cumplida", "Modo demo" |
| `HighlightChip` | common | `FilterChip` | Destacado, seleccionable o fijo |
| `PhotoThumbnail` | common | `AsyncImage` + `IconButton` | Miniatura de foto, con o sin botón de quitar |
| `RoutePolyline` | common | `Polyline` (Maps Compose) | Traza sobre el mapa |
| `EmptyState` | common | `Image` + `Text` | "Todavía no hay recorridos" |
| `ConfirmDialog` | common | `AlertDialog` | Confirmación de dos opciones |
| `PermissionNotice` | common | `Card` + `SecondaryButton` | Por qué hace falta un permiso + "Ir a configuración" |
| `LoadingIndicator` | common | `CircularProgressIndicator` | Carga o espera de señal GPS |

Los componentes globales que reciben un recorrido (`RunCard`, `MetricItem`, `StatusBadge`) usan un modelo de UI como `RunSummaryUi`, nunca el modelo de `domain`.

## Pantallas

`splash`, `home`, `startrun` (modal), `map`, `result`, `saverun`, `camera`, `history`, `rundetail` (modal), `settings`. Los componentes propios de cada una están en la Guía de UI y en el `design.md` de cada spec.