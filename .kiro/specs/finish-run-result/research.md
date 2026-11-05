# Investigación · finish-run-result

## Análisis de brechas (gap analysis)

_Fecha: 2026-11-04 · Requisitos analizados: `requirements.md` (generados, sin aprobar) · Rama de trabajo: `feat/hu-06-resultado` (parte de `develop` en `f9c5a5a`)_

### Alcance y límites de este análisis

- **Steering:** no existen `product.md`, `tech.md` ni `structure.md`. Solo hay `design-system.md` y `ui-components.md`, que se leyeron completos en las partes relevantes. La falta de esos tres archivos puede restar contexto.
- **Código de esta rama:** revisado directamente (archivos de `app/src/main`).
- **Código de otras ramas:** lo que se sabe de `origin/HU-04-map` (seguimiento del recorrido) sale de la lista de archivos y de la lectura de sus modelos de dominio, casos de uso y repositorio. De `origin/feat/hu-05-frenadas` y `origin/feat/modal-iniciar-recorrido` solo se vio la lista de archivos modificados.
- **No leído:** los requisitos y el diseño de `live-route-tracking` (HU-04) y de `frenadas-pausas-personales` (HU-05). Todo lo que depende de ellos queda marcado como **Unknown**.

### 1. Estado actual

**Esta rama (`feat/hu-06-resultado`)**
- Una sola Activity (`MainActivity`) con Hilt. `TrazoApp` arma un `Scaffold` con `BottomNavBar` y un `NavHost` de **3 rutas**: `home`, `map` y `history`. No existen las rutas `result` ni `saverun`, que el steering sí prevé.
- `MapScreen` es un placeholder de 15 líneas (un `Text("Mapa")`). No hay mapa, panel en curso, controles ni botón «Finalizar».
- `domain/`, `data/` y `di/` solo tienen `.gitkeep`: **no hay modelo de recorrido, repositorio, casos de uso ni persistencia**.
- `ui/components/common/` y `layout/` están vacíos. Lo único reutilizable son `EmptyState`, `StartRunFab` y `TrazoTopBar`, que viven en `screens/home/components/`.
- Tema completo (`Color`, `Type`, `Shape`, `Dimens`, `Theme`). Dependencias: Compose BOM, Material 3, Navigation Compose, Hilt, `hilt-navigation-compose`, Coil, `lifecycle-runtime-compose`. **No hay** Lottie, Room, DataStore, mapas ni librería de ubicación.
- Tests: `HomeViewModelTest` (unitario), `HomeContentTest` y `BottomNavBarTest` (instrumentados).

**`origin/HU-04-map` (no integrada)**
- Modelos en `domain/model/map/`: `RunSession`, `RunState` (`IDLE`, `RUNNING`, `PAUSED`, `FINISHED`), `RunGoal` (enum 3/5/10 km con `distanceKm`), `RunMetrics` (tiempo, distancia, velocidad actual y promedio), `RoutePoint`, `FrozenRunSummary`, `GpsQuality`.
- `RunSessionRepository` expone `activeSession: StateFlow<RunSession?>`, `pauseSession`, `resumeSession`, `finishSession(): Result<FrozenRunSummary>` y `resetToIdle()`. Hay casos de uso `Pause`, `Resume`, `Finish`, `GetActiveRunSession` y `AttachActiveRunSession`.
- `finishSession()` solo acepta sesiones en `RUNNING` o `PAUSED`, detiene los jobs de seguimiento, calcula el tiempo y devuelve un `FrozenRunSummary`. Deja la sesión en `FINISHED`.
- El registro corre en un scope de aplicación (no en la pantalla). La rama también trae `data/location` y módulos Hilt.
- **No se vio** si el repositorio detecta que se alcanzó la meta ni qué hace en ese caso (**Unknown**).

**`origin/feat/hu-05-frenadas`**
- Solo agrega documentación (`frenadas-pausas-personales`: requisitos e investigación). No se vio código de frenadas ni de regularidad.

**`origin/feat/modal-iniciar-recorrido`**
- Agrega `PrimaryButton` y `GoalChip` en `components/common/`, y `StartRunDialog` en `screens/startrun/`. Sirve de precedente para los diálogos centrados.

### 2. Mapa de requisitos y recursos

| Req. | Necesidad técnica | Qué hay | Brecha |
|---|---|---|---|
| 1.1 | Detener el registro al llegar a la meta | `finishSession()` detiene el registro, pero solo a pedido | **Missing**: detección de `distancia ≥ meta` (**Unknown** si HU-04 ya la hace). **Constraint**: si se usa `finishSession()` para detener, la sesión pasa a `FINISHED` y no se puede volver a llamar (falla el `check`), así que «Finalizar» después no puede repetirla |
| 1.2–1.3, 1.5 | Aviso animado en el mapa con botón «Finalizar» | `MapScreen` es un placeholder | **Missing**: el mapa, el panel y el aviso. **Constraint**: el aviso depende de HU-04 |
| 1.5, 5.4 | Detectar «Quitar animaciones» | — | **Research Needed**: Compose no tiene una API directa, se suele leer la escala de duración de animaciones del sistema |
| 1.4 | Aviso solo con meta alcanzada | — | Lógica pura: `distanceMeters ≥ goal.distanceKm × 1000` |
| 2.1 | «Finalizar» tras la meta lleva a resultado sin confirmar | — | **Missing**: decisión de flujo (meta alcanzada / confirmar / demasiado corto) |
| 3.1–3.4 | Diálogo «Todavía no llegaste a tu meta» con dos opciones | `StartRunDialog` en otra rama como referencia. `ConfirmDialog` figura en el catálogo global | **Missing**: el diálogo. Nace en `screens/map/components/`. **Constraint**: el registro sigue corriendo mientras está abierto |
| 4.1–4.6 | Aviso de recorrido corto (< 200 m), descartar o volver | `resetToIdle()` existe en HU-04 | **Missing**: el aviso y la regla de 200 m. Descartar mapea a `resetToIdle()` más navegar al Mapa. El botón «Descartar» usa el estilo destructivo (error) |
| 5.1–5.3, 5.6 | Pantalla de resultado con título, métricas, botón y animación | Nada (`screens/result/` no existe) | **Missing**: `ResultScreen`, `ResultContent`, `ResultViewModel`, `GoalReachedCard`. `MetricValue` (global) tampoco existe aún |
| 5.1 | Mostrar regularidad | No existe en ningún modelo | **Missing / Unknown**: tipo, unidad y formato dependen de HU-05 |
| 5.5 | Mostrar valores sin recalcular | `FrozenRunSummary` trae distancia y tiempo | Sin brecha para distancia y tiempo |
| 6.1 | Abrir «Guardar recorrido» | Ruta `saverun` no existe | **Missing**: ruta y pantalla (otra spec: `save-run`) |
| 6.2 | Conservar distancia, tiempo, velocidad promedio, meta | `FrozenRunSummary` los tiene | Sin brecha de datos |
| 6.2 | Conservar frenadas y regularidad | No existen | **Missing**: HU-05 |
| 6.3 | Conservar fecha y hora de inicio, modo demo y traza | Traza: sí (`routePoints`). Inicio: solo marca **monotónica** (sirve para duraciones, no como fecha) | **Missing**: fecha y hora de inicio real y marca de modo demo (**Unknown** dónde se originan) |
| 6.4 | Indicar si se alcanzó la meta | Se deriva de distancia vs. meta | Lógica pura; mejor calcularla una sola vez |
| 6.1–6.2 | Pasar el recorrido entre pantallas | — | **Research Needed**: la traza es grande para argumentos de navegación. Hace falta un contenedor compartido (repositorio en memoria o ViewModel con alcance de grafo) |
| Todos | Textos | `strings.xml` | **Missing**: todos los textos van en recursos (regla del proyecto) |
| Accesibilidad | Fuente al 200 %, estado no solo por color | Reglas en `design-system.md` | **Constraint** de verificación |

### 3. Ambigüedades que los requisitos no cierran

1. **Pestañas de abajo:** no se define si la barra de navegación inferior se ve en la pantalla de resultado. Impacta la navegación (**Research Needed** / decisión de UX).
2. **«Atrás» en el resultado:** los requisitos no dicen qué pasa al volver desde «¡Meta cumplida!» o «Recorrido finalizado» sin tocar «Continuar». El recorrido no está guardado y se podría perder.
3. **Finalizar en pausa:** HU-04 permite finalizar desde `PAUSED`. Los requisitos no mencionan la pausa. Se asume que aplica la misma lógica.
4. **Modo demo y 200 m:** los requisitos no distinguen modo demo. Se asume que la regla de 200 m aplica igual.
5. **Aviso de meta con la app en segundo plano:** el registro corre en un scope de aplicación, pero los requisitos no dicen qué ve el corredor al volver a la app tras alcanzar la meta (se asume el aviso).

### 4. Opciones de implementación

#### Opción A: Extender el módulo de seguimiento (HU-04)
- Agregar a `RunSessionRepository` la detección de meta (estado «meta alcanzada»), el guardado de datos para el resultado y los campos que faltan en `FrozenRunSummary`. La UI de `map/` y `result/` solo consume el estado.
- ✅ Una sola fuente de verdad. El registro se detiene en el mismo lugar donde se mide.
- ❌ Obliga a cambiar el contrato de HU-04 antes de integrarse (otra rama y otro equipo). Mezcla reglas de cierre con medición.

#### Opción B: Capa nueva en esta feature
- Crear un caso de uso puro `DecideFinishAction` (meta alcanzada / confirmar antes de la meta / demasiado corto) y un `ResultViewModel` propio. La detección de meta la hace la UI observando `activeSession`.
- ✅ Lógica de decisión testeable sin Android. Menos acoplada a HU-04.
- ❌ La detección de meta en la UI es frágil si la pantalla no está activa. Duplica reglas que podrían vivir en el dominio.

#### Opción C: Híbrida
- **Dominio nuevo y chico:** `DecideFinishAction` y la regla de 200 m, en `domain/usecases/`.
- **Cambios mínimos en HU-04:** que el repositorio detenga el registro y exponga «meta alcanzada». Que `FrozenRunSummary` incluya fecha de inicio y modo demo, y las métricas de HU-05.
- **UI nueva:** aviso y diálogos en `screens/map/components/`, y `screens/result/` completa.
- **Traspaso a «Guardar recorrido»:** un contenedor en memoria compartido, no argumentos de navegación.
- ✅ Reparte bien las responsabilidades y deja lo testeable en dominio.
- ❌ Requiere coordinar con HU-04 y HU-05.

### 5. Complejidad y riesgo

- **Esfuerzo: M (3–7 días)**, **si** HU-04 y HU-05 ya están integradas. Sin ellas sube a **L**, porque faltan el mapa, el panel, el modelo de recorrido y las métricas.
- **Riesgo: Medio.** La lógica es simple (comparaciones y dos diálogos), pero depende de dos ramas sin integrar, de campos que todavía no existen (regularidad, fecha de inicio, modo demo) y de decisiones de navegación sin cerrar.

### 6. Recomendaciones para la fase de diseño

- **Enfoque sugerido:** Opción C. Dejar la regla de decisión pura en dominio, con tests unitarios sin Android.
- **Orden de integración:** confirmar si HU-04 entra a `develop` antes que esta feature. Si no, el diseño debe definir una interfaz mínima (`RunSessionRepository`) para trabajar contra un doble.
- **Decisiones a tomar en el diseño:**
  - Qué estado representa «meta alcanzada, registro detenido» sin romper `finishSession()`.
  - Cómo se traspasa el recorrido de `result` a `saverun` (contenedor compartido vs. ViewModel con alcance de grafo).
  - Qué pasa con «Atrás» y con la barra inferior en el resultado.
  - Cómo se implementa la animación de celebración (animaciones nativas de Compose vs. agregar una librería; el repositorio no tiene ninguna hoy).
- **Research Needed:**
  - Cómo detectar «Quitar animaciones» en Compose.
  - Formato y tipo de la regularidad (HU-05).
  - Origen de la fecha de inicio y del modo demo (HU-04/Configuración).
  - Si la detección de meta de HU-04 ya existe.
- **Componentes:** todo nace en `screens/<pantalla>/components/` y se mueve a `common/` solo cuando lo use una segunda pantalla (regla de `ui-components.md`).

---

## Ampliación: cruce con HU-04 y HU-05.1

_Fecha: 2026-11-04 · Fuentes leídas en modo solo lectura: `live-route-tracking` (`requirements.md` y `design.md`, rama `origin/HU-04-map`) y `frenadas-pausas-personales` (`requirements.md` y `research.md`, rama `origin/feat/hu-05-frenadas`). Dato aportado por el equipo (documento «Trazo v0.6»): la regularidad es un porcentaje (por ejemplo 80 %) y no existe cuando el celular no tiene acelerómetro._

### A. Qué cierra esta lectura

| Unknown del análisis anterior | Resultado |
|---|---|
| ¿HU-04 detecta la meta? | **Parcial.** HU-04 pide mostrar una animación de celebración al alcanzarla (Req. 8) con un componente `GoalCelebrationOverlay`. **No se pudo confirmar** en el código del repositorio que detenga el registro. HU-04 deja explícitamente **pendiente** si alcanzar la meta finaliza solo o si hay que tocar «Finalizar» |
| Origen de la fecha de inicio real | **No está en HU-04.** Sus modelos solo manejan marcas monotónicas. La spec no menciona fecha y hora de inicio |
| Origen del modo demo | **No aparece** en ningún documento leído de HU-04 ni de HU-05.1 |
| Tipo y formato de la regularidad | **Porcentaje** (dato del equipo). HU-05.1 la deja fuera de su alcance y la asigna a **HU-05.2**, de la que **no hay documentos** en las ramas revisadas |
| Navegación posterior a «Finalizar» | HU-04 la deja **pendiente** (¿directo a `save-run` o pantalla intermedia?) y declara fuera de su alcance la pantalla de resultados. **HU-06 resuelve esa decisión**: hay pantalla intermedia |

### B. Superposiciones y conflictos con HU-04

HU-04 propone, **pendiente de aprobación del equipo**, un flujo de finalización que choca en varios puntos con HU-06:

| Tema | HU-04 (propuesta) | HU-06 (requisitos) | Consecuencia |
|---|---|---|---|
| Confirmar al tocar «Finalizar» | **Siempre** muestra un diálogo de confirmación (`FinishConfirmDialog`) | Solo si la meta no se alcanzó (Req. 3). Con la meta cumplida no pide confirmación (Req. 2) | Hay que acordar un único diálogo. El de HU-06 tiene el mensaje «Todavía no llegaste a tu meta» y las opciones «Finalizar recorrido» / «Cancelar» |
| Recorrido menor a 200 m | No lo contempla | Aviso y descarte (Req. 4) | Regla nueva de HU-06 |
| Qué pasa al alcanzar la meta | Animación de celebración, sin finalizar | Se detiene el registro y aparece un aviso con botón «Finalizar» (Req. 1) | **Doble dueño** de lo que se ve en el mapa al llegar: `GoalCelebrationOverlay` (HU-04) vs. el aviso de HU-06 |
| Quién finaliza | `FinishRunUseCase` (detiene y devuelve `FrozenRunSummary`) | HU-06 necesita detener antes (en la meta) y finalizar después | Hace falta un estado intermedio «meta alcanzada, registro detenido» |
| Pantalla de resultados | Fuera de su alcance | Dentro del alcance (Req. 5) | Sin conflicto, pero HU-04 espera que otro la haga |
| Destino tras descartar | No lo define | Vuelve al Mapa sin recorrido en curso (Req. 4.5) | El estado `IDLE` de HU-04 lo cubre (`resetToIdle()`) |

**Acuerdo de equipo necesario:** la propuesta de HU-04 está sin aprobar. Si se aprueba tal cual, el diálogo de confirmación iría **siempre** y los Req. 2.1 de HU-06 («sin pedir confirmación») quedarían en contradicción. Hay que decidirlo antes del diseño.

### C. Qué ofrece HU-05.1 a esta feature

- HU-05.1 registra cada **frenada** con su ubicación y permite al corredor reclasificarla como **pausa personal**, lo que la excluye del conteo. El resultado es una lista de frenadas por recorrido.
- El **conteo de frenadas** que necesita `save-run` (y la velocidad promedio ya presente en `RunMetrics`) saldrá de esa lista. La presentación de frenadas en resultados e historial queda **fuera** de HU-05.1, o sea que el contrato de datos de salida aún no existe.
- HU-05.1 solo actúa **mientras hay un recorrido en curso**. Si el registro se detiene al alcanzar la meta (Req. 1.1 de HU-06), la detección debería parar en ese momento.
- La alerta de frenada (a los 8 s) y el diálogo de clasificación (a los 20 s) pueden coincidir con el aviso de meta, con el diálogo de «Todavía no llegaste a tu meta» o con el aviso de recorrido corto. **Ninguna spec define la prioridad ni qué pasa con el diálogo pendiente.**
- Ambas specs necesitan una utilidad para «Quitar animaciones» (HU-05.1 Req. 3.2, HU-06 Req. 1.5 y 5.4). Conviene **una sola utilidad compartida**.
- Convención útil: HU-05.1 usa el reloj inyectado y `kotlinx-coroutines-test` (hoy ausente del proyecto) para probar umbrales de tiempo. Sirve para probar la regla de 200 m y las transiciones de HU-06.

### D. Regularidad: impacto en los requisitos de HU-06

Con el dato del equipo (porcentaje, ausente sin acelerómetro) aparecen brechas **en los propios requisitos**:

1. **Req. 5.1** exige mostrar la regularidad en la pantalla de resultado, pero **no define qué se muestra cuando no hay acelerómetro**. Hace falta un criterio (por ejemplo, ocultar el dato o mostrar «—» o «No disponible»). **Decisión de producto pendiente.**
2. **Req. 6.2** exige conservar la regularidad hacia «Guardar recorrido». El valor debe poder ser **«sin dato»** (no cero). Un 0 % significaría un piso muy irregular, no la ausencia de sensor. Esto también afecta el contrato con `save-run`.
3. **Formato:** porcentaje entero («80 %»), con espacio y sin decimales según el ejemplo del documento. **Research Needed:** confirmar si se redondea y cuál es el rango (0–100).
4. **Accesibilidad:** el valor ausente tiene que anunciarse con texto, no solo con un guion.
5. **Dependencia:** HU-05.2 (dueña de la regularidad y del sensor) **no tiene documentos** en las ramas leídas. HU-06 solo puede mostrar lo que HU-05.2 entregue.

### E. Nuevas preguntas abiertas

1. ¿Se aprueba que el diálogo de confirmación de HU-04 se reemplace por la lógica de tres casos de HU-06 (sin confirmación con meta cumplida, confirmación sin meta, aviso si es corto)?
2. ¿Quién es dueño del aviso al alcanzar la meta: el `GoalCelebrationOverlay` de HU-04 o el aviso de HU-06? ¿Es el mismo componente?
3. ¿Qué se muestra de la regularidad cuando no existe el acelerómetro (pantalla de resultado y datos hacia `save-run`)?
4. ¿Qué prioridad tienen la alerta o el diálogo de frenada frente al aviso de meta y a los diálogos de cierre?
5. ¿De dónde salen la fecha y hora de inicio real y la marca de modo demo que `save-run` espera?

### F. Efecto sobre las opciones, el esfuerzo y el riesgo

- **Opciones A/B/C:** sin cambios. La Opción C sigue siendo la sugerida, ahora con dos acuerdos previos (cierre y contrato de datos) además del orden de integración.
- **Esfuerzo:** se mantiene **M** si HU-04 está integrada, y **L** si no.
- **Riesgo:** se mantiene **Medio**, con dos puntos de coordinación concretos: el flujo de finalización de HU-04 (sin aprobar) y la regularidad de HU-05.2 (sin documentación).
- **Posible ajuste de requisitos:** agregar criterios para el caso «regularidad no disponible» en los Req. 5 y 6 antes de pasar al diseño. No modifiqué `requirements.md`.
