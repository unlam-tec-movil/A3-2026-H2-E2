# Investigación · home-recent-runs

## Análisis de brechas (gap analysis)

_Fecha: 2026-11-02 · Requisitos analizados: `requirements.md` (generados, sin aprobar)_

### 1. Estado actual

**Estructura y patrones**
- App de una sola Activity: `MainActivity` (`@AndroidEntryPoint`) → `TrazoTheme { TrazoApp() }`. Hilt ya está configurado (`TrazoApplication` con `@HiltAndroidApp`).
- `ui/navigation/TrazoApp.kt`: tiene un `Scaffold` con `bottomBar = BottomNavBar` y un `NavHost` cuyo `startDestination` es `home`. Hay 3 rutas: `home`, `map` y `history`. La navegación entre pestañas usa `popUpTo(start) { saveState }`, `launchSingleTop` y `restoreState`.
- `ui/navigation/TrazoDestination.kt`: un enum con `route`, `title` (en un String fijo: "Inicio", "Mapa", "Historial") e `icon` (`Icons.Filled.Home`, `Place` y `AutoMirrored.Filled.List`).
- `ui/navigation/BottomNavBar.kt`: un `NavigationBar` de Material 3 sin colores ni íconos propios. Usa el mismo ícono para el ítem activo y los inactivos.
- `ui/screens/home/HomeScreen.kt`, `map/MapScreen.kt` y `history/HistoryScreen.kt` son placeholders con un `Text` centrado. No hay ViewModel, UiState ni `Content`.
- `ui/theme/` está completo según el sistema de diseño: colores por rol, `extendedColors`, `Spacing`, `Sizes` y las formas small, medium y large.
- `ui/components/common/` y `ui/components/layout/` solo tienen `.gitkeep`, así que **no existe todavía ningún componente global**.
- `domain/` y `data/` también son solo `.gitkeep`: no hay modelo de recorrido, repositorio ni persistencia.
- Tests: hay un `ExampleUnitTest` y un `ExampleInstrumentedTest`. `ui-test-junit4` está disponible solo para `androidTest`. Robolectric no está configurado.

**Dependencias relevantes** (`gradle/libs.versions.toml`)
- Compose BOM 2025.04.01, Material 3, `material-icons-core` (sin la versión *extended*), Navigation Compose 2.8.9, `hilt-navigation-compose` 1.2.0 y Coil.
- No están `androidx.core:core-splashscreen`, `lifecycle-runtime-compose` (que trae `collectAsStateWithLifecycle`) ni Room.

**Recursos**
- `strings.xml` solo tiene `app_name`.
- `themes.xml` usa `Theme.Trazo` con parent `android:Theme.Material.Light.NoActionBar`. No hay tema de splash.
- `colors.xml` todavía tiene los colores de la plantilla (purple/teal).
- Drawables: solo los del ícono del launcher. **No hay logo de Trazo ni ilustración del estado vacío.**

### 2. Mapa de requisitos y recursos

| Req. | Necesidad técnica | Qué hay | Brecha |
|---|---|---|---|
| 1.1–1.2 | Pantalla de carga con logo | Nada | **Missing**: el mecanismo de splash, el asset del logo y el tema de splash |
| 1.3 | Pasar solo a Inicio al terminar de cargar | `startDestination = home` | **Missing**: falta definir la condición de "terminó de cargar" (**Unknown**: hoy no se carga nada) |
| 1.4 | "Atrás" no vuelve a la carga | — | **Constraint**: depende del mecanismo. Con la API de SplashScreen sale gratis. Con una ruta en Compose hace falta `popUpTo(inclusive)` |
| 2.1–2.2 | Estado vacío con ilustración y "Todavía no hay recorridos" | Placeholder | **Missing**: `HomeContent`, `HomeUiState`, la ilustración (**Unknown**: ¿quién la provee?) y el string |
| 2.1 | Saber si hay recorridos guardados | Nada en `domain` ni `data` | **Missing**: la fuente de datos. **Constraint**: Room no está y la persistencia es de otra historia |
| 2.3 | Texto accesible | — | La ilustración es decorativa (`contentDescription = null`) y el texto es un `Text`. Sin brecha técnica |
| 3.1–3.2 | FAB "+" habilitado, abajo a la derecha, sobre la barra | Nada | **Missing**: `StartRunFab`. **Constraint**: aparece en el catálogo como componente global (`common/`), pero las reglas no dejan crear globales dentro de una historia (ver §4) |
| 3.1 | Condición "no hay recorrido en curso" | Nada | **Unknown**: no hay estado de recorrido. Por ahora es siempre "sin recorrido" |
| 3.3 | Opción "Iniciar un recorrido" al tocar el FAB | Nada | **Missing**: FAB extendido en secondaryContainer con ícono `route` |
| 3.4 | Cerrar la opción tocando afuera o con "Atrás" | — | **Missing**: un scrim o detección de toques fuera, más un `BackHandler` |
| 3.5 | `contentDescription` del FAB | — | **Missing**: el string |
| 4.1 | 3 opciones con ícono y etiqueta | Existe | **Constraint**: las etiquetas están fijas en el código (las reglas piden `strings.xml`) |
| 4.2 | Inicio acentuado: indicador primaryContainer y ícono relleno | `NavigationBar` por defecto | **Missing**: colores propios. El indicador por defecto de M3 es `secondaryContainer`, que en Trazo es **Lima**, y eso no es lo que pide el diseño. También falta el par de íconos relleno/con trazo |
| 4.3 | Inactivos sin acento y tocables | Ya son tocables | **Missing**: ícono con trazo (outlined) y `onSurfaceVariant` |
| 4.4 | Navegar y acentuar el destino | Existe | Sin brecha |
| 4.5 | Selección no solo por color | — | **Missing**: etiqueta en negrita e ícono relleno en el ítem activo |

### 3. Íconos

- `material-icons-core` tiene `Home` (relleno y con trazo), `Place`, `List`, `Add` y `Settings`.
- El sistema de diseño pide `map`, `history` y `route`, que solo están en `material-icons-extended` los cuales se importan como vector drawables.
- **Research Needed**: importar como `res/drawable` solo los íconos necesarios, cada uno en sus variantes rellena y con trazo.

### 4. Tensiones con las reglas del proyecto

1. **Componentes globales dentro de una historia.**
   - `ui-components.md` lista `EmptyState` y `StartRunFab` como globales de `common/`, pero no existen todavía.
   - La misma guía dice: *"No crear ni modificar componentes globales dentro de una historia de usuario. Si falta uno, se avisa al equipo y se agrega en un PR aparte."*
   - `design-system.md`, en cambio, dice que *todo componente nuevo nace en `screens/<pantalla>/components/`*.
   - Una salida que cumple las dos reglas es crear los componentes en `ui/screens/home/components/` y moverlos a `common/` cuando Mapa o Historial los necesiten. La otra es pedir un PR previo con los globales. **Hay que decidirlo en el diseño.**
2. **Modificar `BottomNavBar` y `TrazoDestination`.** Están en `navigation/`, no en `components/`, así que no les aplica la prohibición de tocar globales. Igual impactan en Mapa e Historial: el cambio es visual, no cambia el comportamiento.
3. **Catálogos que no coinciden.** `RunCard` aparece en `common/` en `ui-components.md` y en `history/components/` en `design-system.md`. No afecta esta historia, pero conviene avisar al equipo.

### 5. Opciones de implementación

#### Opción A: extender lo que ya existe
- La carga se hace como una ruta `splash` en Compose dentro del `NavHost` actual. Navega a `home` con `popUpTo("splash") { inclusive = true }`.
- `HomeScreen.kt` incluye el estado vacío y el FAB directamente, sin `Content`, ViewModel ni componentes aparte.
- `BottomNavBar` se ajusta en el mismo archivo.
- ✅ Muy pocos archivos y no agrega dependencias.
- ❌ No cumple la estructura `Screen`/`Content`/`ViewModel`/`UiState` que pide `ui-components.md`. Una ruta de splash en Compose deja un hueco en blanco antes de la primera composición (el *cold start*) y se superpone con la splash del sistema en Android 12+, que aparece igual. La barra inferior se vería en la pantalla de carga si no se oculta según la ruta.

#### Opción B: piezas nuevas siguiendo el patrón del proyecto
- **Carga:** la API `androidx.core:core-splashscreen`.
  - Un tema `Theme.Trazo.Splash` con fondo primary y el logo, y `postSplashScreenTheme = Theme.Trazo`.
  - `installSplashScreen()` en `MainActivity`, con `setKeepOnScreenCondition` hasta que la app esté lista.
  - Cubre los Req. 1.1–1.4 de forma nativa: no hay pantalla en blanco ni hay que manejar "Atrás".
- **Inicio:**
  - `HomeScreen` conecta el ViewModel.
  - `HomeContent(state, onStartRunClick)` dibuja la pantalla.
  - `HomeViewModel` + `HomeUiState` (con `recentRuns` vacío y `isRunInProgress = false`).
  - `ui/screens/home/components/`: `HomeEmptyState` (o `EmptyState`) y `StartRunFab`.
- **Datos:** un puerto en `domain/repository` (por ejemplo `RunRepository.observeRecentRuns(): Flow<List<…>>`), con un adapter provisorio que devuelva una lista vacía hasta que exista la persistencia. Otra posibilidad es que el ViewModel arranque con la lista vacía sin repositorio (ver Research Needed).
- **Navegación:**
  - `TrazoDestination` pasa a usar `@StringRes` y dos íconos (relleno y con trazo).
  - `BottomNavBar` usa `NavigationBarItemDefaults.colors(indicatorColor = primaryContainer, …)`, y el ítem activo lleva la etiqueta en negrita.
- ✅ Cumple las reglas de estructura, deja previews y tests de `Content` sin Android, y la splash es la oficial.
- ❌ Agrega `core-splashscreen` y `lifecycle-runtime-compose`, y hay más archivos.

#### Opción C: híbrida
- La Opción B para Inicio y la navegación, pero **sin** puerto de repositorio: `HomeUiState` arranca vacío y la fuente de datos queda para la historia que guarde recorridos.
- La splash con `core-splashscreen`, pero sin condición de espera, porque hoy no hay nada que cargar y solo se ve mientras arranca el proceso.
- ✅ Es lo mínimo que cumple los requisitos y las reglas, y no anticipa el modelo de dominio.
- ❌ La historia siguiente va a tener que agregar el repositorio y cambiar el ViewModel. Además, el Req. 1.2 ("mientras carga") queda atado al tiempo real de arranque, que puede ser muy corto.

### 6. Esfuerzo y riesgo

- **Esfuerzo: S (1–3 días).** Son pantallas en patrones conocidos, la navegación ya existe y no hay lógica de negocio.
- **Riesgo: Bajo.** La tecnología es conocida. Lo único abierto es tener los assets (logo e ilustración) y resolver cómo se ubican los componentes globales.

### 7. Recomendaciones para el diseño

**Enfoque sugerido:** la Opción B o la C, según se resuelvan los puntos de abajo. La Opción A no se recomienda porque rompe la estructura `Screen`/`Content` y tiene los problemas de la splash en Compose.

**Decisiones clave para el diseño**
1. El mecanismo de la pantalla de carga (se recomienda `core-splashscreen`) y qué se considera "terminó de cargar".
2. Dónde viven `EmptyState` y `StartRunFab`: en `screens/home/components/` o en un PR previo con los globales.
3. Si se define ya el puerto `RunRepository` o si el estado vacío queda fijo en el ViewModel.
4. Quién es dueño del FAB: `HomeContent` (un `Box` con alineación) o el `Scaffold` de `TrazoApp` (`floatingActionButton` según la ruta), pensando en que Mapa también lo va a usar.
5. Los íconos: `material-icons-extended` o vector drawables importados.
6. La barra de navegación con colores de Trazo (indicador primaryContainer, no Lima) y las etiquetas en `strings.xml`.

**Research Needed**
- Los assets del **logo de Trazo** (para la splash: ícono animado o estático de 240 dp con máscara circular) y de la **ilustración del estado vacío**: ¿los entrega el equipo de diseño?
- Si `core-splashscreen` funciona bien con `minSdk 24` y `enableEdgeToEdge()`, y cómo se ve antes de Android 12.
- Cómo implementar el cierre de la opción "Iniciar un recorrido" tocando afuera (un scrim propio o un `Popup` con `onDismissRequest`), cuidando la accesibilidad del foco.
- La estrategia de tests de UI: si `HomeContent` se prueba con instrumentados (`ui-test-junit4`) o si se agrega Robolectric para correr los tests de Compose en la JVM (el workflow `test-coverage.yml` corre los tests unitarios).

---

## Actualización: decisiones del equipo (2026-11-02)

### Decisiones tomadas
1. **Logo e ilustración:** se entregan como `.svg`. Se convierten a VectorDrawable (`res/drawable/*.xml`) para no sumar dependencias y para que se vean nítidos en cualquier densidad.
   - Nombres propuestos: `ic_trazo_logo.xml` (logo) e `img_empty_runs.xml` (ilustración del estado vacío).
   - **Riesgo:** VectorDrawable no soporta todo SVG (filtros, máscaras, `<text>`, gradientes complejos, CSS embebido). Si la conversión falla, la alternativa es exportarlos como PNG/WebP por densidad.
2. **Pantalla de carga:** no hay una pantalla propia (ni una ruta en Compose ni una espera). Solo se ve el logo por un momento sobre fondo Primary950 mientras la app arranca.
   - Esto resuelve la Opción A vs. B de la carga: **no** se agrega una ruta `splash`. Queda una sola solución por tema de ventana.
   - Requisito 1 actualizado: se quitó "mientras carga" y se agregó "sin espera extra".
3. **Opción "Iniciar un recorrido":** se cierra cuando el corredor toca afuera. Se quitó "Atrás" del Req. 3.4.
4. **Regla de componentes:** se crean en `ui/screens/home/components/` y se mueven a `common/` cuando los necesita otra pantalla. Se actualizó `.kiro/steering/ui-components.md`, porque `design-system.md` ya lo decía. La tensión de §4.1 queda resuelta.

### Opciones para el logo al abrir la app

| | Cómo | A favor | En contra |
|---|---|---|---|
| **S1. `core-splashscreen`** | Tema `Theme.Trazo.Starting` con `windowSplashScreenBackground = Primary950`, `windowSplashScreenAnimatedIcon = @drawable/ic_trazo_logo` y `postSplashScreenTheme = @style/Theme.Trazo`. Una línea `installSplashScreen()` en `MainActivity`, sin `setKeepOnScreenCondition` | El mismo resultado en API 24–37, y es lo que recomienda Google | Una dependencia nueva (chica) |
| **S2. Solo temas, sin dependencia** | `values/themes.xml`: `android:windowBackground` = layer-list (Primary950 + logo centrado). `values-v31/themes.xml`: atributos `android:windowSplashScreen*`. `MainActivity` vuelve a `Theme.Trazo` | Sin dependencias | Dos caminos según la versión. El fondo con el logo se puede ver detrás de Compose en las transiciones si no se restablece `windowBackground` |

- En Android 12+ el sistema **siempre** muestra una splash al abrir la app. Si no se configura, usa el ícono del launcher sobre el fondo del tema. Así que la decisión no es si hay splash o no, sino si se configura con los colores de Trazo.
- **Constraint** de la splash del sistema: el ícono va dentro de un círculo de 160 dp (sobre un área de 240 dp). El logo `.svg` tiene que tener margen para que no se recorte.
- `colors.xml` necesita `primary_950` (`#1B2E31`) para usarlo desde XML. Es la única excepción a "colores por rol", porque los temas de ventana no ven `MaterialTheme`.

### Opciones para cerrar "Iniciar un recorrido" tocando afuera

| | Cómo | A favor | En contra |
|---|---|---|---|
| **F1. `DropdownMenu` / `Popup`** | Anclado al FAB, con `onDismissRequest` | Ya cierra al tocar afuera, y el foco y la accesibilidad vienen resueltos | El estilo del diseño (FAB extendido en Lima) se tiene que dibujar adentro del menú, y su contenedor trae fondo y padding propios |
| **F2. Capa propia en `HomeContent`** | Un `Box` a pantalla completa, transparente o con scrim, con `clickable` para cerrar, y el FAB extendido arriba | Control total del aspecto y de la animación | Hay que manejar a mano el orden del foco y que el lector de pantalla no lea lo de atrás |

- "Atrás" no se pidió. Con F1 igual cierra el menú, porque es el comportamiento estándar de `Popup`. Con F2 no cierra, salvo que se agregue un `BackHandler`.

### Brechas que siguen abiertas
- **Assets:** los `.svg` todavía no están en el repo.
- **Íconos `map`, `history` y `route`:** elegir entre `material-icons-extended` y vectores importados (sigue de §3).
- **Fuente de datos de los recorridos:** puerto `RunRepository` o estado vacío fijo (Opción B vs. C de §5).
- **Tests de UI:** instrumentados.
- **Estimación:** se mantiene esfuerzo **S** y riesgo **Bajo**. La decisión sobre la splash bajó el riesgo.

### Assets convertidos (2026-11-02)
- Los originales se movieron de `app/src/main/res/design/` a `design/assets/`. Una carpeta `res/design/` no es un tipo de recurso válido y rompe la compilación.
- `res/drawable/ic_trazo_logo.xml` (96 dp) y `res/drawable/img_empty_runs.xml` (192 dp) fueron convertidos a mano. `:app:mergeDebugResources` y `packageDebugResources` compilan bien.
  - El `<mask>` y el `<use>` de la cabeza del logo se reemplazaron por un relleno equivalente.
  - Los `<rect rx>` se pasaron a paths.
- **Colores fuera de la paleta** (no se ajustan):
  - Logo: el fondo es `#2E474B` (Primary900, no Primary950) y la figura es `#CDFDCD` (un menta). El sistema de diseño dice que *Lima es el único verde* y que el menta se descartó.
  - Ilustración: la figura es `#9CA3AF`, que no está en la paleta. Sobre `#DDE2EA` tiene un contraste de ~2:1. Es aceptable porque la ilustración es decorativa y el mensaje va en texto.
- **Logo en la splash del sistema** (Android 12+, ícono sin fondo propio, 288 dp con un círculo visible de 192 dp): el cuadrado redondeado necesita un radio de ~57,7 unidades. El logo se puede mostrar a una escala de hasta ~1,66× (≈160 dp) sin que se recorte. Hace falta un drawable contenedor (por ejemplo `splash_logo.xml`, 288 dp, con el logo escalado ×1,5 y centrado), o un `layer-list` con inset si se usa la opción S2.

---

## Diseño: descubrimiento y síntesis (2026-11-03)

### Summary
- **Feature:** `home-recent-runs`
- **Discovery Scope:** Extension (descubrimiento liviano). Extiende la navegación y el tema que ya existen y no tiene integraciones externas.
- **Skills y guías consultadas:** `trazo-design-system` (§6.2 FAB, §6.11 navegación, §7 patrón de Inicio), `.kiro/steering/design-system.md` y `.kiro/steering/ui-components.md` (estructura Screen/Content/ViewModel, ubicación de componentes, strings en `strings.xml`).
- **Key Findings:**
  - El workflow `test-coverage.yml` corre `koverXmlReportRelease`, que ejecuta los tests unitarios de **release**. `ui-test-manifest` es `debugImplementation`, así que los tests de Compose con Robolectric no funcionarían en ese job sin cambiar la configuración. Por eso los tests de UI van como instrumentados.
  - En la caché local están `lifecycle-runtime-compose` y Material 3 1.3.x. **No** está `core-splashscreen`, así que la primera compilación necesita red.
  - El `Scaffold` de `TrazoApp` ya aplica `innerPadding`: lo que se alinea abajo en `HomeContent` queda por encima de la barra de navegación sin calcular nada.

### Design Decisions

#### Decision: splash con `core-splashscreen` sin condición de espera
- **Alternativas:** (1) `core-splashscreen`, (2) solo temas (`windowBackground` + `values-v31`), (3) una ruta `splash` en Compose (descartada por el equipo).
- **Elegida:** (1). `Theme.Trazo.Starting` + `installSplashScreen()`, sin `setKeepOnScreenCondition`.
- **Por qué:** se comporta igual en API 24–37 con un solo tema y no deja `windowBackground` con el logo detrás de Compose. Cumple "por un momento" y "sin espera extra".
- **Trade-off:** una dependencia chica.

#### Decision: el estado de la opción del FAB en `HomeUiState`
- **Alternativas:** `rememberSaveable` local en `HomeContent` o en `StartRunFab`, o un campo en el `UiState`.
- **Elegida:** un campo en el `UiState`, con eventos en el ViewModel.
- **Por qué:** la lógica de 3.3 y 3.4 se puede testear en la JVM (la única suite que corre en CI), y `HomeContent` no tiene estado propio, como pide `ui-components.md`.

#### Decision: la opción en un `Popup` focusable (F1 refinada)
- **Alternativas:** `DropdownMenu` (trae contenedor y padding propios, con un estilo que no coincide con el FAB extendido Lima) o un scrim propio dentro de `HomeContent` (F2). El scrim no cubre la barra de navegación, porque está fuera del contenido del `NavHost`, así que tocar la barra no cerraría la opción.
- **Elegida:** `Popup(focusable = true, onDismissRequest)` con un `PopupPositionProvider` propio y el `ExtendedFloatingActionButton` adentro.
- **Por qué:** cubre toda la ventana, el foco y la accesibilidad los resuelve la plataforma, y el aspecto queda libre.
- **Follow-up:** verificar en el test instrumentado que tocar afuera solo cierra la opción y no dispara la acción de abajo.

#### Decision: íconos como VectorDrawable de Material Symbols
- **Alternativas:** `material-icons-extended` (miles de íconos; con `isMinifyEnabled = false` agranda mucho el APK) o `material-icons-core` (no tiene `map`, `history` ni `route`).
- **Elegida:** importar solo los 8 íconos necesarios. Plan B: `material-icons-core`.

### Síntesis
- **Generalización:** `EmptyState` recibe la ilustración y el mensaje por parámetro, no asume Inicio. Historial lo va a reutilizar (su estado vacío es "igual que Inicio") moviéndolo a `common/`.
- **Build vs. adopt:** se adopta la splash del sistema (`core-splashscreen`), `Popup` y `NavigationBar` de M3. No se construye nada de animación ni de scrim propio.
- **Simplificación:**
  - No se crea `RunRepository` ni un modelo de dominio (sería especulativo: hoy no hay persistencia). `hasRecentRuns` queda en `false`.
  - No hay `setKeepOnScreenCondition` porque no se carga nada.
  - No se agrega Robolectric.

### Risks & Mitigations
- **La primera compilación necesita red** para `core-splashscreen`. Mitigación: ejecutar `./gradlew assembleDebug` con conexión antes de implementar.
- **Tests instrumentados sin emulador en CI:** no corren en el PR. Mitigación: la lógica queda en el ViewModel (JVM), y los instrumentados se corren en local.
- **Logo fuera de la paleta.** Mitigación: queda aislado en dos drawables.

### Cambio de decisión: el logo como ícono de la app, sin splash (2026-11-03)
- **Contexto:** el equipo no quiere una splash, ni propia ni configurada. Alcanza con que el logo se vea al abrir la app. Entre "logo en el tema de la ventana" y "logo como ícono de la app", eligió el **ícono de la app**.
- **Reemplaza a:** la decisión "splash con `core-splashscreen`" de la sección anterior. Ya no se agregan `core-splashscreen`, `Theme.Trazo.Starting`, `splash_logo.xml` ni `trazo_primary_950`, y `MainActivity` no cambia. Sale también el riesgo de "la primera compilación necesita red".
- **Cómo funciona:**
  - Se reescriben `ic_launcher_foreground.xml` (la figura del logo, centrada en 108 dp y dentro de la zona segura de 66 dp) e `ic_launcher_background.xml` (`#2E474B`).
  - `mipmap-anydpi-v26/ic_launcher*.xml` ya los referencia.
  - Desde Android 12, el sistema dibuja ese ícono en cada arranque en frío.
- **Requisitos actualizados:** el Req. 1 pasa a 4 criterios. 1.1 es el ícono de la app. 1.2 es el logo al abrir en Android 12+ (antes decía "logo sobre Primary950"). 1.3 es el paso a Inicio sin espera, y 1.4 es que "Atrás" no vuelve al logo.
- **Consecuencias aceptadas:**
  - En Android 7–11 no se ve un logo al abrir.
  - En Android 12+ el fondo del arranque es el del tema (claro), no Primary950.
  - En API 24–25 el launcher sigue mostrando los `.webp` de la plantilla, hasta que alguien los rasterice a mano con *Image Asset Studio*.
