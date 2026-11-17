# Implementation Plan

> Revisado contra el código real (HU-04 ya mergeada). Ya existen y **no se rehacen**: la decisión de
> cierre y la guarda de 200 m (`LiveRouteTrackingViewModel.onFinishRequested`), `FinishConfirmDialog`,
> el aviso de recorrido corto y el descarte (`MapScreen`), `FinishRunUseCase`, `suppressed` y
> `GoalCelebrationOverlay`. Quedan solo el botón «Finalizar» del aviso, la pantalla de resultado y la
> ruta. Modo demo: fuera de alcance (el resultado ignora `isDemoMode`).
>
> Tareas eliminadas respecto de la versión anterior: `FinishPolicy`, `CloseRunUseCase`,
> `DiscardRunUseCase`, `FinishRunViewModel`, `FinishRunOverlay`, `GoalReachedNotice`,
> `TooShortRunDialog` y la utilidad `rememberReduceMotion()` (ya existe).

- [ ] 1. Fundación
- [ ] 1.1 Fake del repositorio y reutilización de `rememberReduceMotion()`
  - `kotlinx-coroutines-test` y `ui-test-junit4` ya están en `app/build.gradle.kts`: no tocar dependencias
  - Crear (o reutilizar el del test de `LiveRouteTrackingViewModelTest`) un fake de `RunSessionRepository` con `getFinishedSummary(id)` controlable y registro de llamadas
  - Confirmar que `rememberReduceMotion()` de `ui/components/layout/GoalCelebrationOverlay.kt` sirve para el resultado; no crear otra
  - Los tests de la spec compilan y corren en JVM con el fake
  - _Requirements: 5.4, 5.5_
- [x] 1.2 (P) Textos e ícono del resultado
  - Crear `strings_finish_run.xml` con los textos `result_*` (títulos «¡Meta cumplida!» y «Recorrido finalizado», «Completaste tu meta de %1$s.», métricas, «Continuar», «No disponible», «Incompleto»), con voseo rioplatense
  - No agregar textos del aviso, del diálogo de confirmación ni del recorrido corto: ya están como `map_*` en `strings.xml`
  - Agregar el ícono `ic_goal_check`
  - Los textos y el ícono se pueden referenciar desde Compose y no hay textos fijos en el código
  - _Boundary: strings_finish_run.xml, ic_goal_check_
  - _Requirements: 5.1, 5.2, 5.3_

- [ ] 2. Mapa: botón «Finalizar» en el aviso de meta existente
- [ ] 2.1 Acordar el cambio con Persona 3
  - Confirmar con Persona 3 el parámetro opcional `onFinish` en `GoalCelebrationOverlay` y su conexión en `MapScreen`, ambos archivos suyos
  - Si no lo acepta, 1.1 y 1.3 quedan cubiertos solo por el «Finalizar» de `RunControls`; marcar 2.2 y 2.3 como cancelados
  - _Requirements: 1.1, 1.3_
- [ ] 2.2 Agregar «Finalizar» a `GoalCelebrationOverlay`
  - Parámetro `onFinish: (() -> Unit)? = null`; con valor, mostrar «Finalizar» (`map_finish`) junto a «Seguir corriendo»; sin valor, el componente queda igual
  - Objetivo táctil de 48 dp; el texto envuelve a fuente 200 %; no cambiar animación ni textos existentes
  - UI test: con `onFinish` aparece «Finalizar» y lo invoca; sin `onFinish` no aparece; con «Quitar animaciones» sigue sin animar
  - _Depends: 2.1_
  - _Requirements: 1.1, 1.3, 1.5_
- [ ] 2.3 Conectar «Finalizar» del aviso en `MapScreen`
  - Pasar `onFinish = { viewModel.onCelebrationDismissed(); viewModel.onFinishRequested() }` al overlay
  - Al tocarlo, el aviso se cierra y se abre la confirmación que corresponde (con meta alcanzada, «¿Estás seguro de que deseás finalizar?»); el registro sigue hasta confirmar
  - Si ya hay un test del ViewModel para este flujo, reutilizarlo; si falta, agregar uno: tocar «Finalizar» desde el aviso muestra `showFinishConfirmDialog` y no llama a `finishSession()`
  - _Depends: 2.2_
  - _Requirements: 1.2, 1.3, 2.1, 2.2_

- [ ] 3. Resultado: dominio y estado
- [x] 3.1 (P) Formateo de los valores del resultado
  - Distancia en km con coma decimal desde `RunMetrics.distanceMeters` (metros, Double), tiempo `HH:MM:SS` desde `elapsedTimeSeconds` y regularidad con espacio y sin decimales, en `es-AR`; solo formatea, no recalcula
  - Tests: `5200.0` m → `5,2 km`, `1930` s → `00:32:10`, `80` → `80 %`
  - Los tests del formateador pasan
  - _Boundary: ResultFormatter_
  - _Requirements: 5.1, 5.5_
- [x] 3.2 (P) Caso de uso de lectura del resumen
  - `GetFinishedRunSummaryUseCase` en `domain/usecases/`, con `@Inject`, que delega en `RunSessionRepository.getFinishedSummary(id)` y devuelve `FrozenRunSummary?`
  - No limpia nada (la interfaz no tiene `remove`)
  - Test con fake: devuelve el resumen del id y `null` si es desconocido
  - _Boundary: GetFinishedRunSummaryUseCase_
  - _Requirements: 5.5, 6.2, 6.3, 6.4_
- [x] 3.3 Estado de la pantalla de resultado
  - `ResultViewModel` que lee `runId` de `SavedStateHandle`, obtiene el resumen con el caso de uso y arma `ResultUiState` con `ResultFormatter`; usa `summary.goalReached` y `summary.goal` (`domain.model.map.RunGoal`); la regularidad queda `null`; ignora `isDemoMode`
  - Si el resumen es `null`, emite `LeaveToMap`
  - Tests con fake: con meta, sin meta, regularidad ausente, id desconocido; las métricas salen de `metrics` sin recalcular
  - Dado un id, el estado refleja título, distancia y tiempo del resumen
  - _Depends: 3.1, 3.2_
  - _Requirements: 5.1, 5.2, 5.3, 5.5, 6.4_

- [ ] 4. Resultado: pantalla
- [ ] 4.1 Pantalla y tarjeta de resultado
  - `ResultScreen` + `ResultContent` stateless: fondo primario, `GoalReachedCard` con título «¡Meta cumplida!» (check animado una vez) o «Recorrido finalizado» sin animación y marca «Incompleto»; recuadros de distancia, tiempo y regularidad («No disponible» sin dato); botón «Continuar» a todo el ancho; frenadas no se muestran
  - «Completaste tu meta de X.» usa `goalLabel(goal)` de `ui/components/layout/GoalLabel.kt`
  - Con `rememberReduceMotion()` la tarjeta de meta cumplida se ve estática con el mismo contenido; sin botón de volver y con «atrás» sin efecto; título como encabezado accesible
  - UI tests: ambos títulos, métricas, «No disponible», sin check en «Recorrido finalizado», «Continuar» invoca el callback con el id
  - La pantalla se ve completa con fuente al 200 % y no depende solo del color
  - _Depends: 1.2, 3.3_
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 6.1, 6.5_

- [ ] 5. Integración
- [ ] 5.1 Ruta `result/{runId}` y navegación desde el Mapa
  - En `TrazoApp`, reemplazar el `TODO (Persona 4)` de `onNavigateToResults`: navegar a `result/${summary.id}` con `launchSingleTop` (el efecto `NavigateToResults` hoy lleva el `FrozenRunSummary`; solo se usa su `id`)
  - Agregar `composable("result/{runId}")` con un único argumento `String` y ocultar la barra inferior en esa ruta
  - Confirmar con el equipo que esta spec es dueña de ese `TODO` (hoy lo nombra a Persona 4)
  - Al finalizar un recorrido se abre el resultado con el id correcto y la barra inferior no se ve
  - _Boundary: TrazoApp navigation_
  - _Depends: 4.1_
  - _Requirements: 2.3, 3.3, 5.6_
- [ ] 5.2 «Continuar» hacia «Guardar recorrido»
  - Conectar `onContinue(runId)` para navegar a `saverun/$runId`
  - La ruta `saverun` no existe y `save-run` no tiene `design.md`: hasta que exista, «Continuar» queda conectado a un callback vacío
  - Pedir a `save-run` que lea con `getFinishedSummary(id)` y defina cuándo se limpia el resumen
  - _Depends: 5.1_
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 6. Validación
- [x] 6.1 Prueba de flujo con fakes
  - `ResultViewModel` con un resumen congelado: con meta → «¡Meta cumplida!»; sin meta → «Recorrido finalizado»
  - Regresión de 4.6: con menos de 200 m `onFinishRequested()` abre el aviso corto y no llama a `finishSession()`, así que no se llega al resultado (ya cubierto por `LiveRouteTrackingViewModelTest`; solo agregar si falta)
  - _Depends: 3.3_
  - _Requirements: 2.3, 3.3, 4.6, 5.2, 5.3, 6.4_

- [ ] 7. Bloqueadas por otras specs (sin trabajo en esta spec hasta que se destraben)
- [ ] 7.1 Prioridad del cierre sobre la alerta de frenada (7.1–7.3) — bloqueada por `stop-detection`
  - Productor listo: `LiveRouteTrackingUiState.suppressed` ya refleja aviso de meta, confirmación y aviso corto
  - Falta el consumidor: no hay código de `stop-detection` en `main`. Cuando exista su overlay, conectar `suppressed` y resolver la consulta pendiente como frenada (una sola vez, al pasar a visible)
  - _Requirements: 7.1, 7.2, 7.3_
- [ ] 7.2 Frenadas y regularidad hacia «Guardar recorrido» (6.2) — bloqueada por `stop-detection` y HU-05.2
  - `FrozenRunSummary` no trae frenadas ni regularidad; definir con esas specs y con `save-run` cómo llegan
  - _Requirements: 6.2_
- [ ] 7.3 Limpieza del resumen — pregunta abierta a Persona 3
  - `RunSessionRepository` no tiene `remove`; definir quién limpia `finishedSummaries` y cuándo
