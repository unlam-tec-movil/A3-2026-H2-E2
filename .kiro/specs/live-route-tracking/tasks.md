# Implementation Plan

> **Plan armado después de implementar (2026-11-23).** HU-04 se construyó antes de tener este
> archivo; las tareas registran lo que ya está en `develop`, con el código y los tests que lo
> cubren, para que `/kiro-spec-status` refleje el avance real. Las rutas son las de después del
> refactor #128 (ver la revisión al inicio de `design.md`).
>
> El requisito 10 lo implementan otras specs: el formulario y el guardado en Room son de `save-run`
> (10.1 a 10.4) y las trazas guardadas en el mapa, de `profile-runs` (10.5). HU-04 solo entrega el
> resumen congelado (10.6).

- [x] 1. Dominio de la sesión

- [x] 1.1 Sesión de recorrido, estados y métricas
  - `RunSession` (`Idle` o `ActiveSession`), `RunState` (`ACTIVE`, `PAUSED`, `STOPPED`,
    `PERSONAL_PAUSED`), `RunMetrics`, `RunGoal` y `InitializedRunSession`, en `domain/model/map/`
  - La sesión calcula tiempo, distancia y velocidades con el reloj monotónico; la pausa manual, la
    pausa personal y la frenada son estados distintos
  - Tests: `RunSessionRulesTest`, `GeoDistanceTest`
  - _Requirements: 2.3, 2.4, 2.5, 2.6, 2.7, 5.1, 5.3, 5.5_

- [x] 1.2 Filtro de precisión y pérdida de señal
  - `GpsSignalRules` y `RoutePoint.isAccurate()`: descarta lecturas de más de 30 m y avisa «Sin señal
    GPS» a los 15 s sin una ubicación válida, contados desde la última válida o desde que se reanudó
  - La calidad de señal (`GpsQuality`) se calcula aparte del filtro de precisión, y un punto impreciso
    no saca a la sesión de «Sin señal»
  - Tests: `GpsSignalRulesTest`, `RunSessionRulesTest`
  - _Requirements: 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3_

- [x] 1.3 Meta alcanzada, regla de los 200 m y resumen congelado
  - `goalReached` se marca al llegar a la distancia de la meta y queda en el resumen
  - `RunSession.isShortRun` (menos de `MIN_FINISH_DISTANCE_METERS`, 200 m) decide si se puede finalizar
  - `toFrozenSummary` congela métricas, traza, inicio real y meta en `FrozenRunSummary`
  - Tests: `RunSessionRulesTest`
  - _Requirements: 8.3, 9.1, 9.3, 9.5_

- [x] 2. Datos: ubicación y repositorio de la sesión

- [x] 2.1 Ubicación del dispositivo
  - `LocationClient` y `AndroidLocationClient` (`data/location/`) emiten `RoutePoint`;
    `LocationRepository` expone ubicación, calidad de señal y estado del permiso a la UI
  - Tests: `AndroidLocationClientTest`, `LocationRepositoryImplTest`
  - _Requirements: 2.1, 3.1_

- [x] 2.2 Repositorio de la sesión
  - `RunSessionRepositoryImpl` (único en la app) recibe la sesión de `start-run` con
    `attachActiveSession`, sigue la ubicación y el temporizador fuera de la pantalla, y aplica pausa,
    reanudación, frenada y pausa personal
  - `finishSession(brakes, regularityPercent)` congela el resumen; `getFinishedSummary(id)` lo
    entrega a la pantalla de resultado y a `save-run`; `resetToIdle()` descarta sin guardar
  - Tests: `RunSessionRepositoryImplTest`
  - _Requirements: 1.7, 3.6, 7.1, 7.2, 7.3, 9.6, 10.6_

- [x] 3. ViewModel del Mapa

- [x] 3.1 Estado de la pantalla
  - `LiveRouteTrackingViewModel` combina la sesión, la ubicación sin recorrido, los diálogos y las
    trazas guardadas en `LiveRouteTrackingUiState`; sin recorrido muestra métricas en cero y los
    controles sin acción
  - `canPause`, `canResume` y `canFinish` según el estado; `suppressed` tapa la alerta de frenada
    durante el cierre
  - Tests: `LiveRouteTrackingViewModelTest`, `LiveRouteTrackingViewModelFlowsTest`,
    `LiveRouteTrackingUiStateTest`
  - _Requirements: 1.1, 1.2, 1.4, 2.1, 2.2, 5.2, 5.4, 7.4_

- [x] 3.2 Finalizar, recorrido corto y celebración
  - «Finalizar» abre el aviso de recorrido corto (menos de 200 m) o la confirmación, con el texto
    según la meta; cancelar o volver deja el recorrido como estaba
  - Confirmar llama a `FinishRunUseCase`; si falla, la sesión sigue y el diálogo ofrece reintentar.
    Si sale bien, navega al resultado
  - La celebración aparece una vez por recorrido y se rearma al volver a `Idle`
  - Tests: `LiveRouteTrackingViewModelTest`, `LiveRouteTrackingViewModelFlowsTest`
  - _Requirements: 8.1, 8.2, 9.1, 9.2, 9.3, 9.4, 9.6, 9.7_

- [x] 4. Pantalla del Mapa (Actividad)

- [x] 4.1 Mapa, panel y controles
  - `MapScreen` conecta el ViewModel y los efectos; `MapContent` dibuja sin estado propio.
    Componentes en `ui/screens/map/components/`: `MapLayer` (traza, posición y «Centrar»),
    `RunPanel`, `RunControls` (56 dp), `GpsIndicator` y `FinishConfirmDialog`
  - Métricas con coma decimal (`RunValueFormatter`) y descripciones accesibles en controles,
    «Centrar» y aviso de señal
  - El «+» y el aviso de espera de GPS llegan armados desde `start-run` por slots; `MapScreen` no pide
    permisos
  - Tests: `MapScreenTest`, `MapLayerTest`, `RunPanelTest`, `RunControlsTest`, `GpsIndicatorTest`,
    `FinishConfirmDialogTest`
  - _Requirements: 1.3, 1.6, 6.1, 6.2, 11.1, 11.4, 11.5, 11.6_

- [x] 4.2 Pantalla encendida y celebración accesible
  - Con un recorrido en curso la pantalla no se apaga mientras se ve el Mapa (`FLAG_KEEP_SCREEN_ON`)
  - `GoalCelebrationOverlay` respeta «Quitar animaciones» (`rememberReduceMotion`) sin dejar de
    registrar la meta
  - Tests: `MapScreenTest`, `GoalCelebrationOverlayTest`
  - _Requirements: 7.5, 7.6, 8.4, 11.3_

- [ ] 5. Validación en dispositivo

- [ ]* 5.1 Recorrido real
  - Con un celular: la traza y las métricas avanzan al correr, «Sin señal GPS» aparece a los 15 s sin
    señal y se va al recuperarla, la pantalla no se apaga en el Mapa y el recorrido sigue al cambiar
    de pestaña
  - _Requirements: 2.1, 3.3, 3.5, 7.2, 7.5_

## Implementation Notes

- Requisito 1.5 (aspecto de los controles sin recorrido): quedó así, sin acción y con el estilo
  deshabilitado de Material 3.
- Requisito 11.2 (solo metas de 3, 5 y 10 km) lo garantiza `RunGoal`, que no admite otros valores.
