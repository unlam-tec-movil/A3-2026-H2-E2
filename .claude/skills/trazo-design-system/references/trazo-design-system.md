# Trazo · Sistema de diseño (v0.1)

Fuente única de verdad para diseñar y programar la app Android **Trazo** (Kotlin + Jetpack Compose + Material 3).
Usá este documento para crear pantallas, componentes y código Compose nuevos. No se inventan colores, tamaños ni estilos fuera de lo que figura acá.

---

## 1. Contexto del producto

- **Qué es:** un diario personal de recorridos para corredores recreativos de Buenos Aires. La app registra un recorrido libre con una meta de 3, 5 o 10 km. Mide distancia, tiempo, velocidad promedio, frenadas (acelerómetro) y regularidad del piso. Al terminar, el corredor lo guarda con título, destacados y hasta 2 fotos.
- **Plataforma:** Android, Kotlin, Jetpack Compose, Material 3. Todo se guarda en el dispositivo (Room). No hay login ni cuentas: la pestaña Perfil es local.
- **Navegación:** barra inferior con 2 pestañas: **Actividad · Perfil**. La app abre en Actividad (el mapa), que es la única pestaña con el botón flotante "+". Perfil reúne «Mis estadísticas» y «Mis recorridos», y desde su barra superior el engranaje abre Configuración.
- **Usuario:** corredor de 18 a más de 40 años, principiante o intermedio, que usa la app **mientras corre**. Por eso los objetivos táctiles son grandes, las cifras se leen rápido y los textos son cortos.
- **Idioma:** español rioplatense con voseo ("Escribí un título", "Podés agregar hasta 2 fotos"). La configuración regional es es-AR y se usa coma decimal ("5,2 km").

---

## 2. Principios

1. **Datos primero.** Las métricas (tiempo, distancia, frenadas, regularidad) son el contenido principal. Van en cifras grandes y tabulares.
2. **Usable en movimiento.** Objetivos táctiles de 48 dp como mínimo, y 56 dp para los controles del recorrido. Mensajes de una línea.
3. **Sobrio, con un acento con presencia.** Paleta de verde azulado oscuro con un acento lima apagado. Nada de neón, degradados ni emojis.
4. **El color tiene un significado fijo.** Primary es acción. Lima es el único verde de la app: lo que elige o logra el corredor (Secondary) y los avisos y marcas de éxito (Success) comparten la misma escala. Error, Warning e Info son solo para mensajes del sistema.
5. **Accesible por defecto.** Todo el texto cumple WCAG 2.1 AA (≥ 4.5:1). Íconos y bordes de componentes, ≥ 3:1.

---

## 3. Color

### 3.1 Paleta (tokens)

**Primary · marca**

| Token | Hex | Uso |
|---|---|---|
| Primary50 | `#EFF7F6` | Fondo de miniaturas de traza |
| Primary100 | `#DEEFEE` | primaryContainer: indicador de navegación, chip de meta, pista de la barra de progreso |
| Primary200 | `#BADDDC` | Placeholder de fotos |
| Primary300 | `#95CBCA` | inversePrimary, halo de posición en el mapa. **No usar para texto ni íconos sobre blanco (1.8:1)** |
| Primary400 | `#7FAFAF` | Reservado |
| Primary500 | `#699395` | Trazas de recorridos guardados en el mapa (al 75 %) |
| Primary600 | `#55797B` | Reservado |
| Primary700 | `#415F63` | Ícono sobre placeholder de foto, rótulos de sección |
| Primary800 | `#385357` | Reservado |
| Primary900 | `#2E474B` | **Tono de apoyo:** inverseSurface (alerta de frenada, snackbar), estado presionado |
| **Primary950** | `#1B2E31` | **COLOR PRINCIPAL:** botones, FAB, barra superior, traza en vivo, controles, texto sobre contenedores claros |

**Secondary · logro y selección del corredor (Lima)**

| Token | Hex | Uso |
|---|---|---|
| Secondary100 | `#F2F7DC` | Halo exterior del check de celebración |
| Secondary200 | `#DCEB9E` | secondaryContainer: destacado seleccionado, opción "Iniciar un recorrido", check de meta cumplida, confeti |
| Secondary500 | `#9DB82F` | Solo decoración (2.3:1 sobre blanco) |
| Secondary700 | `#556810` | secondary: ícono de check, pin de inicio del recorrido |

> Secondary es **Lima**: reemplazó al menta `#B8FAB8` (muy saturado, se confundía con Success) y luego a Salvia `#CFE0D2` (sobria pero con poca presencia sobre el verde azulado oscuro). Lima resalta sobre Primary950 (11.1:1) y **Success usa la misma escala**, para que haya un solo verde en toda la app. Otras alternativas evaluadas: Esmeralda `#A8E0C9` y Terracota `#F4C3B0`. Si se cambia Secondary, se cambia la escala completa (100/200/500/700).

**Neutral**

| Token | Hex | Uso |
|---|---|---|
| Neutral0 | `#FFFFFF` | Cards, modales, panel del temporizador, texto sobre primary |
| Neutral50 | `#FAFBFD` | Bottom sheet, botón "Agregar foto" |
| Neutral100 | `#F3F7FF` | Fondo de todas las pantallas |
| Neutral200 | `#E9EDF3` | Barra de navegación inferior, fondo del mapa |
| Neutral300 | `#DDE2EA` | Divisores y bordes decorativos de cards (outlineVariant). **No alcanza como borde de input (1.3:1)** |
| Neutral500 | `#8A939C` | Reservado |
| Neutral600 | `#6C757D` | outline: borde de inputs y chips sin seleccionar, chevrons, íconos. **No usar para texto chico sobre Neutral100 (4.37:1)** |
| Neutral700 | `#495057` | onSurfaceVariant: texto secundario (fechas, etiquetas, unidades) |
| Neutral900 | `#212529` | onSurface: texto principal |

**Estados** (fondo 200 + tono fuerte para texto e íconos)

| Estado | Fondo | Texto e ícono | Uso |
|---|---|---|---|
| Error | `#FECACA` (Error200) | `#912018` (Error800) · `#B42318` (Error600) para bordes, campo inválido y pin de frenada | Botón Finalizar (tonal), "Sin señal GPS", campo inválido, "Descartar" |
| Success | `#DCEB9E` (Success200 = Secondary200) | `#556810` (Success700 = Secondary700) | "Meta cumplida", "Recorrido guardado", indicador GPS bueno |
| Info | `#B0D5FF` (Info200) | `#0F3F75` (Info900) · `#1D5FA8` (Info700) para íconos o switches | Marca "Demo", aviso de modo demo |
| Warning | `#FDE3A7` (Warning200) | `#8A5300` (Warning700) | "Incompleto", "Esperando señal GPS", "Ubicación aproximada" |

### 3.2 Roles de Material 3 (`lightColorScheme`)

| Rol | Token | Hex |
|---|---|---|
| primary | Primary950 | `#1B2E31` |
| onPrimary | Neutral0 | `#FFFFFF` |
| primaryContainer | Primary100 | `#DEEFEE` |
| onPrimaryContainer | Primary950 | `#1B2E31` |
| inversePrimary | Primary300 | `#95CBCA` |
| secondary | Secondary700 | `#556810` |
| onSecondary | Neutral0 | `#FFFFFF` |
| secondaryContainer | Secondary200 | `#DCEB9E` |
| onSecondaryContainer | Primary950 | `#1B2E31` |
| tertiary (modo demo) | Info700 | `#1D5FA8` |
| onTertiary | Neutral0 | `#FFFFFF` |
| tertiaryContainer | Info200 | `#B0D5FF` |
| onTertiaryContainer | Info900 | `#0F3F75` |
| error | Error600 | `#B42318` |
| onError | Neutral0 | `#FFFFFF` |
| errorContainer | Error200 | `#FECACA` |
| onErrorContainer | Error800 | `#912018` |
| background / surface | Neutral100 | `#F3F7FF` |
| onBackground / onSurface | Neutral900 | `#212529` |
| surfaceVariant | Neutral200 | `#E9EDF3` |
| onSurfaceVariant | Neutral700 | `#495057` |
| surfaceContainerLowest | Neutral0 | `#FFFFFF` |
| surfaceContainerLow | Neutral50 | `#FAFBFD` |
| surfaceContainer | Neutral200 | `#E9EDF3` |
| surfaceContainerHigh / Highest | Neutral300 | `#DDE2EA` |
| outline | Neutral600 | `#6C757D` |
| outlineVariant | Neutral300 | `#DDE2EA` |
| inverseSurface | **Primary900** | `#2E474B` |
| inverseOnSurface | Primary100 | `#DEEFEE` |
| scrim | Negro (al 32 % en uso) | `#000000` |
| surfaceTint | Primary950 | `#1B2E31` |

- **El color dinámico (Material You) está desactivado.** La app conserva siempre su marca.
- En los componentes se usan **roles** (`MaterialTheme.colorScheme.primary`), nunca hex directos.
- `inverseSurface` es Primary900 y no Primary950 a propósito: así un aviso del sistema no se confunde con los controles de acción.

### 3.3 Colores extendidos (fuera de M3)

Se exponen con `TrazoTheme.extendedColors` (CompositionLocal):

| Nombre | Valor |
|---|---|
| success / successContainer / onSuccessContainer | Success700 / Success200 / Success700 |
| warning / warningContainer / onWarningContainer | Warning700 / Warning200 / Warning700 |
| info / infoContainer / onInfoContainer | Info700 / Info200 / Info900 |
| traceLive | Primary950, trazo de 6 dp con borde blanco (`traceLiveOutline`) de 10–11 dp |
| traceSaved | Primary500 al 75 %, 4 dp |
| brakePin | Error600: círculo de 22 dp con borde blanco de 3 dp y un guion blanco |
| startPin | Secondary700: círculo de 16 dp con borde blanco de 3 dp |
| locationHalo | Primary300 al 35 % (radio 26 dp) alrededor de un punto Primary950 de 20 dp con borde blanco |

### 3.4 Pares de contraste verificados

| Combinación | Ratio | Resultado |
|---|---|---|
| Blanco sobre Primary950 | 14.17:1 | AA |
| Primary950 sobre Primary100 | 11.94:1 | AA |
| Primary950 sobre Secondary200 | 11.08:1 | AA |
| Secondary200 sobre Primary950 (destacado sobre fondo oscuro) | 11.08:1 | AA |
| Secondary700 sobre blanco | 6.22:1 | AA |
| Secondary700 sobre Secondary200 (check) | 4.87:1 | AA |
| Blanco sobre Primary900 (alerta) | 9.91:1 | AA |
| Primary100 sobre Primary900 (texto de la alerta) | 8.35:1 | AA |
| Neutral900 sobre Neutral100 | 14.37:1 | AA |
| Neutral700 sobre Neutral100 | 7.62:1 | AA |
| Neutral600 sobre blanco | 4.69:1 | AA (justo) |
| Neutral600 sobre Neutral100 | 4.37:1 | Solo íconos y bordes |
| Error600 sobre Error200 | 4.54:1 | AA |
| Error800 sobre Error200 | 5.99:1 | AA |
| Success700 sobre Success200 | 4.87:1 | AA |
| Info900 sobre Info200 | 6.94:1 | AA |
| Warning700 sobre Warning200 | 5.04:1 | AA |
| Neutral300 sobre blanco | 1.30:1 | Falla: solo decorativo |
| Primary300 sobre blanco | 1.80:1 | Falla: nunca texto ni íconos |

---

## 4. Tipografía

Fuente: **Roboto**, con la escala por defecto de Material 3 (`Typography()`), en sp. No se agregan tamaños sueltos.

| Estilo | Tamaño / línea · peso | Uso en Trazo |
|---|---|---|
| displayLarge | 57/64 · Regular | Reservado |
| displayMedium | 45/52 · Regular | **Temporizador** del recorrido en curso (tabular) |
| displaySmall | 36/44 · Regular | Métricas de "¡Meta cumplida!" y "Recorrido finalizado" (tabular) |
| headlineLarge | 32/40 · Regular | Estados vacíos ("Todavía no hay recorridos") |
| headlineMedium | 28/36 · Regular | Título de celebración ("¡Meta cumplida!"), métricas en recuadros |
| headlineSmall | 24/32 · Regular | Título de modales y diálogos |
| titleLarge | 22/28 · Regular | Barra superior ("Trazo", "Guardar recorrido") |
| titleMedium | 16/24 · Medium | Título de card de recorrido, valores de métricas en cards |
| titleSmall | 14/20 · Medium | Encabezado de sección ("Actividad reciente") |
| bodyLarge | 16/24 · Regular | Texto de inputs, párrafos |
| bodyMedium | 14/20 · Regular | Mensajes, avisos, permisos |
| bodySmall | 12/16 · Regular | Fecha, contador de caracteres ("38/50"), ayudas |
| labelLarge | 14/20 · Medium | Botones, unidades junto a cifras grandes |
| labelMedium | 12/16 · Medium | Chips, marcas de estado, etiquetas de métrica, barra de navegación |
| labelSmall | 11/16 · Medium | Marcas dentro de cards compactas |

**Reglas**

- **Cifras tabulares** (`fontFeatureSettings = "tnum"`) en temporizador, distancia, velocidad, frenadas, porcentajes y contadores. En Compose: `style.tabular()`.
- **Métrica = valor + unidad:** el valor va en el estilo grande y la unidad en labelLarge con onSurfaceVariant, separados por 4 dp y alineados por línea base. La etiqueta va arriba, en labelMedium con onSurfaceVariant.
- **Mayúscula solo inicial:** "¡Meta cumplida!" y no "¡META CUMPLIDA!".
- **Formato numérico** con el Locale del dispositivo: "5,2 km", "10,7 km/h", "98 %", tiempo en "mm:ss" (cards) o "hh:mm:ss" (temporizador).
- **Escalado de fuente:** siempre sp. Probar con fuente al 200 %: los títulos de card se truncan con "…" y las métricas pueden pasar a dos líneas.

---

## 5. Espaciado, forma y elevación

### 5.1 Espaciado (grilla de 4 dp)

| Token | dp | Uso |
|---|---|---|
| Spacing.xxs | 2 | Entre etiqueta y valor de una métrica |
| Spacing.xs | 4 | Ícono y texto dentro de chip o marca, valor y unidad |
| Spacing.sm | 8 | Entre chips, entre botones de control, entre recuadros de métricas |
| Spacing.md | 12 | Entre cards, entre elementos flotantes y el borde del mapa |
| Spacing.lg | 16 | **Margen lateral de pantalla**, padding interno de cards y paneles |
| Spacing.xl | 24 | Padding de modales, separación entre secciones |
| Spacing.xxl | 32 | Bloques grandes en "Guardar recorrido" |
| Spacing.xxxl | 48 | Aire alrededor de ilustraciones de estado vacío |

### 5.2 Tamaños

| Elemento | dp |
|---|---|
| Objetivo táctil mínimo | 48 |
| Controles del recorrido (Pausar / Reanudar / Finalizar) | 56 × 56 |
| FAB | 56 × 56 |
| Botón (alto) | 48 |
| Chip de meta (alto) | 40 |
| Chip de destacado (alto) | 32 |
| Marca de estado (alto) | 24 (20 en cards compactas) |
| Campo de texto (alto) | 56 |
| Miniatura de foto | 96–104 |
| Barra superior | 64 (+ inset del sistema) |
| Barra de navegación inferior | 80 |
| Indicador activo de la barra de navegación | 64 × 32, forma de píldora |
| Barra de progreso de meta | 8 de alto, forma de píldora |

### 5.3 Forma (`Shapes` de M3)

| Token | Radio | Uso |
|---|---|---|
| extraSmall | 4 dp | Tooltips |
| small | 8 dp | Chips de meta y destacados |
| medium | 12 dp | Inputs, controles Pausar/Finalizar, miniaturas, avisos, recuadros de métricas |
| large | 16 dp | Cards, panel del temporizador, barra superior flotante del mapa, FAB |
| extraLarge | 28 dp | Modales, tarjeta de celebración, bottom sheet |
| full | 50 % | Botones, marcas de estado, indicador de navegación, botón "Centrar" |

### 5.4 Elevación

La jerarquía se marca primero con el color de la superficie y después con sombra.

| Nivel | Uso | Sombra (referencia web) |
|---|---|---|
| level0 | Cards sobre el fondo Neutral100 (llevan borde Neutral300 de 1 dp) | ninguna |
| level1 | Panel del temporizador sobre el mapa | `0 1px 2px rgba(27,46,49,.18), 0 1px 3px 1px rgba(27,46,49,.10)` |
| level2 | Barra superior flotante y botón "Centrar" del mapa, opción "Iniciar un recorrido" | `0 1px 2px rgba(27,46,49,.18), 0 2px 6px 2px rgba(27,46,49,.10)` |
| level3 | FAB, modales, alerta de frenada | `0 4px 8px 3px rgba(27,46,49,.12), 0 1px 3px rgba(27,46,49,.18)` |

En Compose: `Elevation.level1` (1 dp), `Elevation.level2` (3 dp) y `Elevation.level3` (6 dp), en `ui/theme/Dimens.kt`.

### 5.5 Iconografía

- **Material Symbols Rounded**, 24 dp, peso 400 (trazo de 2 dp), sin relleno.
- El ícono del ítem activo de la barra de navegación va **relleno**.
- Íconos de referencia: `directions_run` (Actividad), `person` (Perfil), `add`, `pause`, `play_arrow`, `stop`, `my_location`, `photo_camera`, `settings`, `check`, `chevron_right`, `arrow_back`, `close`, `route` (distancia), `octagon` con guion (frenada), onda (regularidad), barras de señal (GPS).
- Si un botón tiene solo ícono, lleva `contentDescription` ("Pausar", "Finalizar", "Centrar en mi ubicación", "Configuración").

---

## 6. Componentes

### 6.1 Botones

| Variante | Fondo | Contenido | Uso |
|---|---|---|---|
| Filled (principal) | primary | onPrimary | "Guardar recorrido", "Continuar", "Comenzar". **Uno por pantalla** |
| Outlined | blanco, borde outline (Neutral600) de 1 dp | primary | Acción secundaria: "Cancelar" en modales, "Ir a configuración" (permisos) |
| Tonal | primaryContainer | onPrimaryContainer | Reservado (no se usa para "Cancelar") |
| Text | transparente | primary | "Seguir editando", "Ver todo" |
| Destructivo | error | onError | "Descartar" |
| Deshabilitado | onSurface al 12 % | onSurface al 38 % | Ej.: "Comenzar" sin meta elegida |

- Alto de 48 dp, forma full, padding horizontal de 24 dp (16 dp en Text), texto en labelLarge.
- En las pantallas de formulario, el botón principal va **fijo abajo**, a todo el ancho, con padding de 16 dp y un borde superior outlineVariant.

### 6.2 FAB y controles del recorrido

- **FAB "+":** 56 dp, forma large (16 dp), fondo primary, ícono `add` en onPrimary, elevación level3. Va abajo a la derecha, a 16 dp del borde y por encima de la barra de navegación. **Se deshabilita mientras hay un recorrido en curso.**
- **Opción "Iniciar un recorrido"** (al tocar el FAB): FAB extendido de 56 dp, forma large, secondaryContainer / onSecondaryContainer, ícono `route`.
- **Pausar:** 56 × 56, forma medium, primary con ícono blanco. Al tocarlo cambia a **Reanudar** (`play_arrow`), en primaryContainer / onPrimaryContainer.
- **Finalizar:** 56 × 56, forma medium, errorContainer / onErrorContainer, ícono `stop`.
- **Centrar en mi ubicación:** 48 dp, círculo blanco, ícono primary, elevación level2. Va abajo a la derecha del mapa, 16 dp (`Spacing.lg`) por encima del panel del recorrido; la atribución de los tiles va a su izquierda.

### 6.3 Chips

- **Meta (selección única):** opciones "3 km", "5 km" y "10 km". Alto de 40 dp, forma small. Sin seleccionar: borde outline de 1 dp sobre blanco. Seleccionado: primaryContainer con check de 18 dp.
- **Destacados (selección múltiple):** "Piso parejo", "Seguro", "Iluminado", "Con sombra", y destacados personalizados. Alto de 32 dp, forma small. Seleccionado: **secondaryContainer** con check y texto onSecondaryContainer. Sin seleccionar: borde outline sobre blanco.
- **"Agregar":** chip con borde discontinuo outline, ícono `add` y texto primary.

### 6.4 Marcas de estado (badges)

Alto de 24 dp, forma full, labelMedium, padding horizontal de 10 dp.

| Marca | Fondo / texto |
|---|---|
| Meta cumplida | Success200 / Success700 (Lima) |
| Incompleto | Warning200 / Warning700 |
| Demo | Info200 / Info900 |
| Meta X km (contexto) | primaryContainer / onPrimaryContainer |

### 6.5 Campo de texto (título del recorrido)

- Outlined, 56 dp de alto, forma medium, fondo blanco.
- Etiqueta arriba ("Título del recorrido") en labelMedium. Con foco va en color primary y el borde pasa a 2 dp primary.
- Debajo lleva una ayuda a la izquierda ("Obligatorio", bodySmall onSurfaceVariant) y el contador a la derecha ("38/50", tabular).
- **Error:** etiqueta, borde (2 dp) y ayuda en color error. Mensaje: "Escribí un título". El contador también va en color error.
- Reglas: título obligatorio de 3 a 50 caracteres. Al llegar a 50 se bloquea el ingreso.

### 6.6 Fotos (máximo 2)

- La miniatura mide 104 × 104 dp, con forma medium. Arriba a la derecha lleva el botón "Quitar foto": 32 dp, círculo inverseSurface, ícono `close` blanco.
- **Agregar foto:** también 104 × 104, borde discontinuo outline, fondo Neutral50, ícono `photo_camera` y texto "Agregar foto" en labelMedium primary.
- Contador "1/2" en bodySmall a la derecha del título de la sección. Al llegar a 2 se oculta "Agregar foto" y aparece "Podés agregar hasta 2 fotos".

### 6.7 Avisos (banners en línea)

Padding de 14/16 dp, forma medium, ícono de 20 dp a la izquierda, texto en bodyMedium.

| Tipo | Fondo / contenido | Ícono | Ejemplo |
|---|---|---|---|
| Info | Info200 / Info900 | `info` | "Modo demo activado: los recorridos son simulados." |
| Warning | Warning200 / Warning700 | `warning` | "Esperando señal GPS. El recorrido empieza cuando la precisión sea suficiente." |
| Error | Error200 / Error800 | `error` | "Sin señal GPS. La traza se retoma cuando vuelva la ubicación." |
| Success | Success200 / Success700 | `check` | "Recorrido guardado." |

Los avisos que no requieren acción usan `role=status`. Los de error usan `role=alert` (en Compose, `LiveRegionMode.Polite` o `Assertive`).

### 6.8 Alerta de frenada (snackbar animado)

- **Fondo inverseSurface (Primary900 `#2E474B`)**, forma medium, padding de 12/14 dp, elevación level3.
- **Ícono:** círculo de 36 dp **blanco** con el ícono de frenada en **Primary950**. No es rojo, porque una frenada no es un error. El rojo queda solo para el pin en el mapa.
- **Texto:** título "Frenada detectada" en titleSmall blanco, y detalle "Semáforo o intersección" en bodyMedium inverseOnSurface.
- Aparece sobre el mapa, 12 dp por encima del panel del temporizador. Entra con animación (slide + fade) y se cierra sola a los 4 s.
- Si la detención dura más de 20 s, se reemplaza por un diálogo con las opciones "Frenada" y "Pausa personal".

### 6.9 Card de recorrido

**Card de «Mis recorridos» en Perfil (completa):**
- Fondo blanco, borde outlineVariant de 1 dp, forma large, padding de 16 dp, separación interna de 14 dp.
- Arriba: título en titleMedium, en una sola línea con "…" al desbordar, y la marca de estado a la derecha si es "Incompleto" o "Demo".
- Debajo del título: fecha y meta en bodySmall onSurfaceVariant ("Mar 28 oct · 07:45 · Meta 10 km").
- Grilla de 4 columnas: **Distancia · Tiempo · Frenadas · Regularidad**. Etiqueta en labelMedium onSurfaceVariant y valor en titleMedium tabular.
- Toda la card es tocable y abre el detalle en un modal.

**Card compacta** (sin pantalla asignada desde que Inicio pasó a Perfil):
- Fila con: miniatura de la traza de 64 dp (fondo Primary50, trazo Primary950 de 3 dp), título y fecha, "10,12 km · 58:40" con la marca opcional, y un chevron.

### 6.10 Panel del recorrido en curso

- Card blanca flotante sobre el mapa, a 12 dp de los bordes y por encima de la barra de navegación. Forma large, elevación level1, padding de 16 dp, separación interna de 12 dp.
- **Fila 1:** "Tiempo recorrido" en labelMedium y la marca "Meta 5 km" (primaryContainer).
- **Fila 2:** temporizador en displayMedium tabular, y los controles Pausar y Finalizar (56 dp).
- **Fila 3:** barra de progreso hacia la meta (8 dp, pista primaryContainer, relleno primary) con el texto "1,2 de 5 km" en bodySmall.
- **Fila 4:** separada por un divisor outlineVariant, una grilla de 3 columnas con Distancia, Velocidad y Frenadas.
- **Sin recorrido en curso:** temporizador en 00:00:00, 0 km y 0 km/h, con los controles deshabilitados.

### 6.11 Barras de navegación

- **Barra superior de Perfil:** fondo primary, 64 dp más el inset. "Trazo" en titleLarge blanco y el botón de engranaje (48 dp) a la derecha.
- **Barra superior de pantallas secundarias** ("Guardar recorrido"): fondo surface, botón `arrow_back` de 48 dp y título en titleLarge onSurface.
- **Barra superior de pestañas sin volver** (sin uso desde que Historial pasó a «Mis recorridos» de Perfil): título en titleLarge y subtítulo con el resumen en bodySmall ("12 recorridos · 58,6 km").
- **Barra superior de Actividad (mapa):** flotante, blanca, a 12 dp de los bordes, forma large, elevación level2. Lleva el título "Recorrido en curso" en titleMedium y el indicador de GPS a la derecha.
- **Indicador de GPS:** píldora de 28 dp con barras de señal y el texto "GPS". Con buena señal usa Success200 / Success700. Con señal débil, Warning. Sin señal, Error.
- **Barra de navegación inferior:** 80 dp, fondo surfaceContainer (Neutral200), 2 ítems: Actividad · Perfil.
  - Ítem activo: indicador primaryContainer de 64 × 32, ícono relleno onPrimaryContainer y etiqueta en negrita.
  - Ítems inactivos: ícono con trazo y etiqueta en labelMedium onSurfaceVariant. No se ven "desactivados": siguen siendo tocables.

### 6.12 Mapa

- Fondo Neutral200, calles blancas, parques en `#D6EFD4` (verde de mapa, solo decorativo) y agua en Info200 (si se usa un estilo propio de mapa).
- **Traza en vivo:** primary de 6 dp con borde blanco, puntas redondeadas.
- **Recorridos guardados:** Primary500 al 75 %, 4 dp. Se pueden tocar y abren el detalle.
- **Pines:** inicio en Secondary700, frenada en Error600 y posición actual en Primary950 con halo Primary300 al 35 %. Todos con borde blanco de 3 dp.

### 6.13 Pantalla de celebración ("¡Meta cumplida!")

- Fondo **primary** a pantalla completa, con confeti decorativo (puntos y rayitas en Secondary200 y Primary300, `aria-hidden`).
- Tarjeta blanca centrada, forma extraLarge, padding de 32/24 dp.
- **Check:** círculo Primary950 de 76 dp con el ícono de check blanco de 40 dp, dentro de un halo Secondary100 de 104 dp.
- Título "¡Meta cumplida!" en headlineMedium y subtítulo "Completaste tu meta de 5 km." en bodyMedium onSurfaceVariant.
- Grilla 2 × 2 de recuadros (fondo Neutral100, forma medium) con Distancia, Tiempo, Regularidad y Frenadas en headlineMedium tabular.
- Botón filled "Continuar" a todo el ancho.
- **Variante "Recorrido finalizado"** (meta no alcanzada): mismo layout, sin confeti ni check animado, con la marca "Incompleto".

---

## 7. Patrones de pantalla

| Pantalla | Estructura |
|---|---|
| **Actividad** (pantalla de inicio) | Mapa a pantalla completa, también detrás de la barra de estado (íconos de estado oscuros). Barra superior flotante con GPS. Botón "Centrar". Alerta de frenada cuando corresponde. Panel del recorrido en curso. Barra de navegación (Actividad activo). Sin recorrido: trazas guardadas, panel en cero y FAB "+" (el único de la app) |
| **Perfil** | Barra superior primary con "Trazo" y engranaje. Con scroll: «Mis estadísticas» (titleMedium, encabezado) con 3 filas etiqueta/valor: "Recorridos guardados", "Distancia total" y "Tiempo total" (valor en titleMedium tabular, HH:MM:SS); y «Mis recorridos» con la lista de cards completas (más recientes primero). Sin FAB. Barra de navegación (Perfil activo). **Estado vacío:** ilustración y "Todavía no hay recorridos" |
| **Modal "Iniciar nuevo recorrido"** | Scrim al 32 %. Modal extraLarge con el título (headlineSmall), "Seleccioná una meta:", chips 3/5/10 km, y botones "Cancelar" (outlined) y "Comenzar" (filled, deshabilitado sin meta) |
| **¡Meta cumplida! / Recorrido finalizado** | Ver 6.13 |
| **Guardar recorrido** | Barra superior con volver. Card de resumen (fecha, marca, 3 métricas). Campo de título. "¿Qué destacarías del recorrido?" (opcional) con chips. "Fotos" con contador. Botón fijo "Guardar recorrido". Si el corredor vuelve sin guardar, aparece el diálogo "¿Descartar el recorrido?" con "Descartar" y "Seguir editando" |
| **Detalle (modal)** | Mapa con la traza completa, título, métricas, meta y estado, destacados (se ocultan si no hay), grilla de fotos (se oculta si no hay) y marca "Demo" si corresponde |
| **Configuración** | Secciones "Permisos" (ubicación: Precisa, Aproximada o Denegada; cámara: Permitida o Denegada; botón outlined "Ir a configuración") y "Modo demo" (switch en color tertiary). Durante un recorrido, el switch está deshabilitado y aparece "Finalizá el recorrido para cambiar el modo demo" |

---

## 9. Checklist de accesibilidad

- [ ] El texto cumple ≥ 4.5:1 y los íconos y bordes de componentes ≥ 3:1 (ver 3.4).
- [ ] Los objetivos táctiles miden ≥ 48 dp, y los controles del recorrido 56 dp.
- [ ] Los botones con solo ícono tienen `contentDescription`.
- [ ] El estado no se comunica solo con color: las marcas llevan texto y la frenada lleva ícono y texto.
- [ ] El temporizador **no** se anuncia cada segundo (sin live region). Las alertas sí se anuncian.
- [ ] La interfaz funciona con fuente al 200 % (truncado con "…" y métricas que pueden ocupar dos líneas).
- [ ] La celebración respeta "Quitar animaciones" del sistema: se muestra estática.
- [ ] La traza del mapa tiene borde blanco para leerse sobre cualquier fondo.

---

## 10. Hacer / No hacer

| Hacer | No hacer |
|---|---|
| Usar roles de `MaterialTheme.colorScheme` | Escribir hex sueltos en los composables |
| Usar Primary950 para la acción principal | Usar Primary300 o Neutral300 para texto, íconos o bordes de input |
| Usar Lima (Secondary = Success) para lo que elige o logra el corredor y para los éxitos | Usar otros verdes: el verde de la app es uno solo |
| Usar los colores de estado solo para avisos y marcas | Usar rojo en elementos que no son error (frenada = Primary900 + blanco) |
| Un solo botón filled por pantalla | Poner dos botones filled compitiendo |
| Usar cifras tabulares en métricas | Usar números proporcionales en el temporizador |
| Usar mayúscula solo inicial | Escribir títulos en MAYÚSCULAS |
| Ocultar las secciones vacías del detalle | Mostrar contenedores vacíos |

---

### Ejemplo: alerta de frenada

```kotlin
@Composable
fun BrakeAlert(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.inverseSurface,           // Primary900
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,  // Primary100
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(Neutral0, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_brake),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,               // Primary950
                    modifier = Modifier.size(20.dp),
                )
            }
            Column {
                Text("Frenada detectada", style = MaterialTheme.typography.titleSmall, color = Neutral0)
                Text("Semáforo o intersección", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
```

### Ejemplo: métrica (valor + unidad)

```kotlin
@Composable
fun Metric(label: String, value: String, unit: String? = null, valueStyle: TextStyle = MaterialTheme.typography.titleMedium) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(value, style = valueStyle.tabular(), modifier = Modifier.alignByBaseline())
            if (unit != null) {
                Text(unit, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.alignByBaseline())
            }
        }
    }
}
```
