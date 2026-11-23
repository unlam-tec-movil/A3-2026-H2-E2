# Implementation Plan

> **Ya implementado antes de esta fase:** requisitos 1 a 4, 5.1, 5.2, 5.4 y 6 a 8.1 (modal,
> metas, permisos y avisos de ubicación, PR #98 a #101). Estas tareas cubren la fase 3 y reubican esa
> lógica en `StartRunViewModel` sin cambiar el comportamiento.

- [x] 1. Fundaciones

- [x] 1.1 Unificar la meta con la de HU-04
  - Borrar `domain/model/RunGoal` y usar `domain/model/map/RunGoal` en el modal, el ViewModel y sus
    tests
  - El modal sigue mostrando «3 km», «5 km» y «10 km»
  - El proyecto compila sin `domain/model/RunGoal` y los tests del modal pasan
  - _Requirements: 1.3, 2.1, 2.2, 5.3_

- [x] 1.2 (P) Puerto de estado de la ubicación y su adaptador
  - Interfaz `LocationStatusReader` en `domain/repository/`
  - Adaptador Android en `data/location/` que reutiliza `readLocationStatus`
  - Módulo `StartRunModule` que bindea el puerto y provee `kotlin.time.Clock`
  - La app compila con Hilt resolviendo `LocationStatusReader` y `Clock`
  - _Boundary: LocationStatusReader, AndroidLocationStatusReader, StartRunModule_
  - _Requirements: 6.1, 7.1, 8.1_

- [x] 1.3 (P) Esperar la primera ubicación precisa
  - Caso de uso `AwaitPreciseLocationUseCase` sobre `LocationClient`, con umbral de 30 m
  - Test JVM: ignora una ubicación de 31 m, devuelve la de 30 m y propaga el error del flujo
  - _Boundary: AwaitPreciseLocationUseCase_
  - _Requirements: 8.2, 8.3, 10.1_

- [x] 2. Lógica de inicio en el ViewModel

- [x] 2.1 Estado del «+», opción y flujo de ubicación
  - `StartRunUiState` con `isRunInProgress`, `isOptionVisible`, `isDialogVisible`, `selectedGoal`,
    `locationIssue` y `waitingGoal`; derivados `isFabEnabled`, `isWaitingForGps`, `isConfirmEnabled`
  - `isRunInProgress` se observa desde `RunSessionRepository`
  - Eventos `onFabClick`, `onOptionDismiss`, `onOptionClick`, `onPermissionResult`,
    `onGoalSelected`, `onDismiss`, `onLocationIssueDismiss`; efecto `RequestLocationPermission`
  - Tests JVM: el «+» alterna la opción; con recorrido en curso queda deshabilitado y sin opción; al
    finalizar se habilita; permiso negado emite el efecto y después abre el modal o el aviso;
    aproximada y ubicación apagada muestran el aviso; casos del modal existentes siguen pasando
  - _Depends: 1.1, 1.2_
  - _Requirements: 1.1, 1.2, 1.4, 2.1, 2.2, 2.3, 3.1, 3.2, 4.1, 4.2, 4.3, 6.1, 6.2, 6.3, 7.1, 7.2, 8.1, 9.2, 9.3, 9.4, 9.5, 9.6_

- [x] 2.2 Esperar la señal y arrancar el registro
  - `onConfirm` con meta: cierra el modal, guarda `waitingGoal`, emite `NavigateToMap` y lanza la
    espera; sin meta no hace nada
  - Con la ubicación precisa: arma `InitializedRunSession` (UUID, meta, `MonotonicClock`, `Clock`),
    llama a `AttachActiveRunSessionUseCase` y limpia la espera
  - `onCancelWaiting` cancela sin arrancar; un error del flujo corta la espera y muestra el aviso
  - Tests JVM: con 50 m no arranca; con 20 m arranca con la meta y los instantes de los relojes
    fijos; durante la espera el «+» está deshabilitado; cancelar no arranca; error muestra el aviso;
    una sola entrega por «Comenzar»
  - _Depends: 1.3, 2.1_
  - _Requirements: 3.1, 5.1, 5.2, 5.3, 8.2, 8.3, 8.4, 8.5, 9.7, 10.1, 10.2, 10.3_

- [x] 3. Componentes

- [x] 3.1 (P) Mover el «+» a los componentes comunes
  - `StartRunFab` pasa a `ui/components/common/` sin cambios visuales ni de API
  - Test instrumentado `StartRunFabTest` con los casos del FAB que estaban en `HomeContentTest`
  - El FAB se ve igual en Inicio y el test instrumentado compila
  - _Boundary: StartRunFab_
  - _Requirements: 9.1, 9.2, 9.3, 9.5_

- [x] 3.2 (P) Aviso «Esperando señal GPS»
  - `GpsWaitingNotice` en `ui/screens/startrun/components/` con indicador, texto, meta y «Cancelar»
    (48 dp), textos en `strings_start_run.xml`, indicador quieto con «Quitar animaciones»
  - Tiene preview y usa solo tokens de `ui/theme`
  - _Boundary: GpsWaitingNotice_
  - _Requirements: 8.2, 8.4_

- [x] 4. Integración

- [x] 4.1 Inicio con el slot del «+»
  - `HomeContent` recibe `startRunFab: @Composable (Modifier) -> Unit`; `HomeUiState` queda con
    `hasRecentRuns`; `HomeViewModel` sin eventos del FAB
  - Ajustar `HomeViewModelTest` y `HomeContentTest`
  - Inicio dibuja el «+» recibido abajo a la derecha
  - _Depends: 3.1_
  - _Requirements: 9.1_

- [x] 4.2 Mapa con el «+» y el aviso de espera
  - `MapScreen` suma los slots opcionales `startRunFab` y `gpsWaitingNotice`; `MapLayer` suma
    `belowCenterButton`. De arriba hacia abajo, a la derecha: «Centrar», «+» y el panel del
    temporizador; el aviso de espera va sobre el panel
  - Sin slots, el Mapa se ve igual que hoy
  - _Depends: 3.1, 3.2_
  - _Requirements: 8.2, 9.1_

- [x] 4.3 Cablear el flujo en `TrazoApp`
  - Quitar la lectura de ubicación de `TrazoApp`; colectar `StartRunEffect` (lanzar el pedido de
    permisos y navegar a Mapa)
  - Pasar `StartRunFab` a Inicio y Mapa y `GpsWaitingNotice` a Mapa con el estado del ViewModel
  - Desde Inicio o Mapa, «+» → opción → modal → «Comenzar» lleva a Mapa con la espera visible
  - _Depends: 2.2, 4.1, 4.2_
  - _Requirements: 5.2, 5.4, 6.1, 8.2, 9.1, 9.4_

- [x] 5. Validación

- [x] 5.1 Verificación automática
  - `ktlintCheck`, `testDebugUnitTest`, `assembleDebug` y `assembleDebugAndroidTest` pasan
  - _Depends: 4.3_
  - _Requirements: 1.1, 9.1, 10.1_

- [ ]* 5.2 Prueba en dispositivo
  - Con un celular: el recorrido arranca al tener señal, el «+» se deshabilita en Inicio y Mapa, y
    vuelve a habilitarse al finalizar
  - _Depends: 5.1_
  - _Requirements: 8.3, 9.5, 9.6, 10.1_
