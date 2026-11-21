# Implementation Plan

> Revisado contra el código real (HU-04 ya mergeada). Ya existen y **no se rehacen**: la decisión de
> cierre y la guarda de 200 m (`LiveRouteTrackingViewModel.onFinishRequested`), `FinishConfirmDialog`,
> el aviso de recorrido corto y el descarte (`MapScreen`), `FinishRunUseCase`, `suppressed` y
> `GoalCelebrationOverlay`. Para esta spec quedan la pantalla de resultado y su ruta. El botón
> «Finalizar» del aviso de meta y el manejo de errores de `finishSession()` pasan a **Persona 3
> (HU-04)** por acuerdo (sección 2). Modo demo: fuera de alcance (el resultado ignora `isDemoMode`).
>
> Tareas eliminadas respecto de la versión anterior: `FinishPolicy`, `CloseRunUseCase`,
> `DiscardRunUseCase`, `FinishRunViewModel`, `FinishRunOverlay`, `GoalReachedNotice`,
> `TooShortRunDialog` y la utilidad `rememberReduceMotion()` (ya existe en `ui/theme/ReduceMotion.kt`,
> PR #105 de Rocío; no se crea otra).
>
> Acuerdos con Persona 3: (1) ella hace el botón «Finalizar» del aviso y el reintento ante fallo de
> `finishSession()`; (2) agrega `removeFinishedSummary(id)` a `RunSessionRepository` y lo llama
> `save-run` (HU-07), no esta spec; (3) `rememberReduceMotion()` vive en `ui/theme/ReduceMotion.kt` y
> ella borra su versión provisoria; (4) la ruta `result/{runId}` y el cableado de `TrazoApp.kt` son de
> esta spec, y ella no toca ese archivo.

- [ ] 1. Fundación
- [x] 1.1 Fake del repositorio y uso de `rememberReduceMotion()` de `ui/theme`
  - `kotlinx-coroutines-test` y `ui-test-junit4` ya están en `app/build.gradle.kts`: no tocar dependencias
  - Crear (o reutilizar el del test de `LiveRouteTrackingViewModelTest`) un fake de `RunSessionRepository` con `getFinishedSummary(id)` controlable y registro de llamadas
  - El fake implementa `removeFinishedSummary(id)` cuando Persona 3 lo agregue a la interfaz, pero ningún test de esta spec lo llama
  - Usar `rememberReduceMotion()` de `ui/theme/ReduceMotion.kt` (PR #105); no crear otra ni importar la provisoria de `GoalCelebrationOverlay.kt`
  - Los tests de la spec compilan y corren en JVM con el fake
  - _Requirements: 5.4, 5.5_
- [x] 1.2 (P) Textos e ícono del resultado
  - Crear `strings_finish_run.xml` con los textos `result_*` (títulos «¡Meta cumplida!» y «Recorrido finalizado», «Completaste tu meta de %1$s.», métricas, «Continuar», «No disponible», «Incompleto»), con voseo rioplatense
  - No agregar textos del aviso, del diálogo de confirmación ni del recorrido corto: ya están como `map_*` en `strings.xml`
  - Agregar el ícono `ic_goal_check`
  - Los textos y el ícono se pueden referenciar desde Compose y no hay textos fijos en el código
  - _Boundary: strings_finish_run.xml, ic_goal_check_
  - _Requirements: 5.1, 5.2, 5.3_

- [ ] 2. Mapa: botón «Finalizar» en el aviso de meta existente — **a cargo de Persona 3 (HU-04); no se implementa en esta spec**
- [ ] 2.1 (Persona 3) Acuerdo y manejo de errores de `finishSession()`
  - Acordado: Persona 3 agrega el botón y lo conecta al diálogo de confirmación existente; los archivos (`GoalCelebrationOverlay`, `MapScreen`, ViewModel) son suyos
  - Persona 3 también corrige el manejo de errores de `finishSession()` en `onConfirmFinish()` para poder reintentar sin perder la sesión (hoy no usa `onFailure`)
  - _Requirements: 1.1, 1.3_
- [x] 2.2 (Persona 3) Botón «Finalizar» en el aviso de meta
  - El aviso de meta alcanzada muestra «Finalizar» y lo conecta al diálogo de confirmación existente
  - Hecho a partir del commit `1198e04` de Persona 3, que había quedado anterior al refactor: `GoalCelebrationOverlay(onFinish)` y la conexión en `MapScreen`; se borró la `rememberReduceMotion()` provisoria
  - _Depends: 2.1_
  - _Requirements: 1.1, 1.3_
- [x] 2.3 (Persona 3) Reintento si falla `finishSession()`
  - Si `finishSession()` falla, el corredor puede reintentar sin perder la sesión
  - Hecho a partir del commit `1198e04`: el diálogo queda abierto con `isFinishing` (botones deshabilitados) y, si falla, `finishFailed` muestra `map_finish_error` y «Reintentar» (`map_retry`). A diferencia del commit, no se muestra el mensaje de la excepción
  - _Depends: 2.1_
  - _Requirements: 2.3, 3.3_

- [ ] 3. Resultado: dominio y estado
- [x] 3.1 (P) Formateo de los valores del resultado
  - Distancia en km con coma decimal desde `RunMetrics.distanceMeters` (metros, Double), tiempo `HH:MM:SS` desde `elapsedTimeSeconds` y regularidad con espacio y sin decimales, en `es-AR`; solo formatea, no recalcula
  - Tests: `5200.0` m → `5,2 km`, `1930` s → `00:32:10`, `80` → `80 %`
  - Los tests del formateador pasan
  - _Boundary: ResultFormatter_
  - _Requirements: 5.1, 5.5_
- [x] 3.2 (P) Caso de uso de lectura del resumen
  - `GetFinishedRunSummaryUseCase` en `domain/usecases/`, con `@Inject`, que delega en `RunSessionRepository.getFinishedSummary(id)` y devuelve `FrozenRunSummary?`
  - Solo lee: no llama a `removeFinishedSummary(id)` (lo llama `save-run`)
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
- [x] 4.1 Pantalla y tarjeta de resultado
  - `ResultScreen` + `ResultContent` stateless: fondo primario, `GoalReachedCard` con título «¡Meta cumplida!» (check animado una vez) o «Recorrido finalizado» sin animación y marca «Incompleto»; recuadros de distancia, tiempo y regularidad («No disponible» sin dato); botón «Continuar» a todo el ancho; frenadas no se muestran
  - «Completaste tu meta de X.» usa `goalLabel(goal)` de `ui/components/layout/GoalLabel.kt`
  - Con `rememberReduceMotion()` (de `ui/theme/ReduceMotion.kt`) la tarjeta de meta cumplida se ve estática con el mismo contenido; sin botón de volver y con «atrás» sin efecto; título como encabezado accesible
  - UI tests: ambos títulos, métricas, «No disponible», sin check en «Recorrido finalizado», «Continuar» invoca el callback con el id
  - La pantalla se ve completa con fuente al 200 % y no depende solo del color
  - _Depends: 1.2, 3.3_
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 6.1, 6.5_

- [ ] 5. Integración
- [x] 5.1 Ruta `result/{runId}` y navegación desde el Mapa
  - Esta spec (HU-06) es dueña de la ruta y del cableado en `TrazoApp.kt`; Persona 3 no toca ese archivo
  - En `TrazoApp`, reemplazar el `TODO (Persona 4)` de `onNavigateToResults`: navegar a `result/${summary.id}` con `launchSingleTop` (el efecto `NavigateToResults` hoy lleva el `FrozenRunSummary`; solo se usa su `id`)
  - Agregar `composable("result/{runId}")` con un único argumento `String` y ocultar la barra inferior en esa ruta
  - Al finalizar un recorrido se abre el resultado con el id correcto y la barra inferior no se ve
  - _Boundary: TrazoApp navigation_
  - _Depends: 4.1_
  - _Requirements: 2.3, 3.3, 5.6_
- [x] 5.2 «Continuar» hacia «Guardar recorrido»
  - «Continuar» llama a un callback `onContinue(runId)` expuesto por la pantalla; la ruta `saverun/{runId}` es de `save-run` (HU-07) y se conecta cuando exista
  - Hasta entonces el callback queda con el parche temporal de la 5.3; después navega a `saverun/$runId`
  - `save-run` lee con `getFinishedSummary(id)` y es quien llama `removeFinishedSummary(id)` al guardar o descartar; esta spec no lo llama
  - _Depends: 5.1_
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
- [x] 5.3 Parche temporal: Continuar vuelve al Mapa (aprobado por Rocío)
  - En `TrazoApp`, «Continuar» reutiliza la navegación de `onLeave` (al Mapa con `popUpTo(RESULT_ROUTE) { inclusive = true }` y `launchSingleTop`) mediante un lambda local, para que la pantalla tenga salida (el «atrás» está deshabilitado)
  - Se reemplaza por `navigate("saverun/$runId")` cuando exista la ruta de `save-run` (HU-07)
  - El escenario E5 (Continuar al guardado) queda **parcial** hasta `save-run`; el requisito 6.1 no se cumple aún
  - _Depends: 5.2_
  - _Requirements: 6.1 (parcial)_

- [ ] 6. Validación
- [x] 6.1 Prueba de flujo con fakes
  - `ResultViewModel` con un resumen congelado: con meta → «¡Meta cumplida!»; sin meta → «Recorrido finalizado»
  - Regresión de 4.6: con menos de 200 m `onFinishRequested()` abre el aviso corto y no llama a `finishSession()`, así que no se llega al resultado (ya cubierto por `LiveRouteTrackingViewModelTest`; solo agregar si falta)
  - _Depends: 3.3_
  - _Requirements: 2.3, 3.3, 4.6, 5.2, 5.3, 6.4_

- [ ] 7. Bloqueadas o a cargo de otras specs (sin trabajo en esta spec hasta que se destraben)
- [ ] 7.1 Prioridad del cierre sobre la alerta de frenada (7.1–7.3) — bloqueada por `stop-detection`
  - Productor listo: `LiveRouteTrackingUiState.suppressed` ya refleja aviso de meta, confirmación y aviso corto
  - Falta el consumidor: no hay código de `stop-detection` en `main`. Cuando exista su overlay, conectar `suppressed` y resolver la consulta pendiente como frenada (una sola vez, al pasar a visible)
  - _Requirements: 7.1, 7.2, 7.3_
- [x] 7.2 Frenadas y regularidad hacia «Guardar recorrido» (6.2)
  - `FrozenRunSummary` trae `brakeCount: Int` y `regularityPercent: Int?` (0..100, `null` sin tramos o sin sensor); `save-run` los lee con `getFinishedSummary(id)`
  - Los junta `FinishRunUseCase` (`domain/usecases`) con `BrakeRepository`, `StopMonitor` y `RegularityMonitor`, y se los pasa a `finishSession(brakeCount, regularityPercent)`. Una frenada abierta al finalizar (se guarda recién después, por `closedStops`) se cuenta desde el monitor, y se une por id con las guardadas para no contarla dos veces
  - El Resultado muestra la regularidad con `ResultFormatter.formatRegularity`; las frenadas no se muestran ahí
  - `RegularityMonitor` arranca al iniciar la app (`TrazoApplication` lo inyecta), así que la regularidad ya se mide en la app real
  - _Requirements: 6.2_
- [ ] 7.3 Limpieza del resumen — acordado: a cargo de Persona 3 (repositorio) y `save-run` (HU-07)
  - Persona 3 agrega `removeFinishedSummary(id)` a `RunSessionRepository`; `getFinishedSummary(id)` mantiene su nombre
  - `save-run` (HU-07) lo llama al terminar de guardar o al descartar el recorrido
  - HU-06 **no lo llama**: la pantalla de Resultado solo lee. El descarte de un recorrido corto usa `resetToIdle()` sin crear resumen, así que no hay nada que limpiar
  - Sin trabajo de código en esta spec; solo mantener el fake de test alineado con la interfaz
