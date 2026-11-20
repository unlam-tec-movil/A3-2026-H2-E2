# Implementation Plan

> **Revisión 2026-11-20 · convención de la cátedra (PR #114 y `refactor/convencion-catedra`).**
> Esta nota manda sobre lo que se contradiga más abajo. Clases renombradas, movidas o eliminadas y
> su reemplazo:
>
> - `RegularityRepository` / `RegularityRepositoryImpl` → `RegularityMonitor`, clase concreta en
>   `domain/services/`.
> - `StopDetectionRepository` / `StopDetectionRepositoryImpl` / `SensorStopMonitor` (data) →
>   `StopMonitor` (interfaz) y `SensorStopMonitor` (implementación), ambos en `domain/services/`.
>   Las paradas cerradas salen por `StopMonitor.closedStops` y las guarda `RecordStopsUseCase`.
> - Adaptadores eliminados: `RunSessionRunningRunSource`, `RunSessionStatePublisher`,
>   `RunSessionStopLocationProvider` y `RunSessionActiveRunStateSource`, con sus interfaces
>   `RunningRunSource`, `RunStatePublisher`, `StopLocationProvider` y `ActiveRunStateSource`. Su
>   lógica (incluidas las traducciones D1-D3 de `publishStopState`) vive dentro de los monitores.
> - `GpsPointFilterPolicy` → reglas del modelo en `domain/model/map/GpsSignalRules.kt`
>   (`RoutePoint.isAccurate()`, `GpsSignalRules.isSignalLost`, `ActiveSession.isSignalLostAt`).
> - Interfaces de `domain/repository` → `data`, junto a su implementación: `RunSessionRepository` y
>   `BrakeRepository` en `data/repository/`, `MotionSource` y `MotionSensorAvailability` en
>   `data/sensors/`, `LocationClient` y `LocationStatusReader` en `data/location/`,
>   `MonotonicClock` en `data/clock/`. El paquete `domain/repository` ya no existe.
> - Nuevo `LocationRepository` (`data/repository/`): la UI y los casos de uso leen ubicación,
>   calidad de señal y estado del permiso por ahí, no por `LocationClient`.
> - Los monitores se proveen desde `di/MonitorModule.kt` (`@Singleton`, `@ApplicationScope`).

> **Orden de trabajo:** dominio, sensor y UI no dependen de HU-04 (tareas 1 a 3). HU-04 ya está en
> `develop` y el contrato se verificó contra el código. La 4.1 está desbloqueada. La 4.3 se
> implementa ahora; la 4.4 y la verificación manual de transiciones esperan los cambios de Persona 3
> en `develop` (discrepancias D1–D3 de `design.md`, «Discrepancias con el código real»).

> **Revisión 2026-11-19:** el repositorio de la tarea 2.4 se reorganizó según «Guardar una parada»
> (ver la revisión al inicio de `design.md`): `SensorStopMonitor` + `SaveStopUseCase` +
> `BrakeRepository` bobo. La frenada se guarda al cerrar la parada y ya no se borra.

- [ ] 1. Fundaciones: utilidad compartida, pruebas con tiempo virtual y contratos del dominio

- [x] 1.1 Crear la utilidad compartida «Quitar animaciones»
  - Una función componible que informa si el corredor tiene desactivadas las animaciones del sistema
  - Queda disponible para esta spec, `finish-run-result` y la celebración de Persona 3; avisar a
    Persona 3 cuando esté en la rama
  - Con «Quitar animaciones» activo la función devuelve verdadero; con la escala normal, falso
  - _Boundary: ReduceMotion_
  - _Requirements: 3.2_

- [x] 1.2 Habilitar las pruebas con tiempo virtual
  - Hecha por Persona 3: ya agregó la dependencia de pruebas de corrutinas (`kotlinx-coroutines-test`)
    al catálogo de versiones como dependencia solo de test; esta spec no toca Gradle
  - Un test JVM de ejemplo que usa tiempo virtual compila y pasa con las pruebas JVM del proyecto
  - _Boundary: Gradle (catálogo de versiones y módulo app)_
  - _Requirements: 2.1, 4.1_

- [x] 1.3 Definir los modelos, la configuración y los puertos del dominio
  - Modelos: estado de movimiento, tipo de detención, ubicación de la frenada, evento de frenada,
    muestra de movimiento, fases de la máquina de estados y estado agregado con sus derivados
  - Configuración calibrable con los umbrales y tiempos (8 s, 20 s, confirmación de 1,5 s, tick de
    500 ms, ventana de 1 s, caducidad de muestras de 2 s)
  - Puertos: fuente de movimiento, recorrido en curso, ubicación, publicación de estado del recorrido
    (función `suspend` con `Result`) y reloj monotónico; contrato de salida con el estado y la
    clasificación de la detención actual
  - El dominio compila sin importar Android, `data` ni `ui`; `movement` y la consulta pendiente se
    derivan de la fase en un test unitario
  - _Boundary: Domain (model/stops, repository)_
  - _Requirements: 1.1, 2.2, 2.4, 4.1_

- [ ] 2. Núcleo: detección, sensor y repositorio

- [x] 2.1 (P) Estimar si el corredor está quieto con una ventana de aceleración
  - Decide «quieto» con la desviación de la magnitud en una ventana de 1 s, sin depender de la
    orientación del teléfono
  - Con la ventana incompleta el resultado es «en movimiento»
  - Tests: serie constante → quieto; desviación mayor al umbral → en movimiento; ventana de menos de
    1 000 ms → en movimiento
  - _Boundary: StillnessEstimator_
  - _Requirements: 1.1, 1.2_

- [x] 2.2 (P) Máquina de estados: de movimiento a frenada registrada
  - Reductor puro con el tiempo recibido en cada observación y ids de frenada inyectados
  - A los 8 000 ms desde que comenzó la quietud pasa a «Frenada» y registra una única frenada con la
    ubicación recibida (o sin ella si no hay)
  - Una detención menor a 8 s no registra nada y vuelve a «Movimiento continuo»
  - Tests: 7 999 ms retoma sin frenada; 8 000 ms registra una frenada con ubicación; 30
    observaciones seguidas quietas registran una sola
  - _Boundary: StopDetector_
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3_

- [x] 2.3 Máquina de estados: reanudación, consulta de 20 s y clasificación
  - Salir de la quietud exige 1 500 ms de movimiento continuo; un pico corto no parte la detención
  - Al retomar el movimiento se conserva la frenada ya registrada
  - A más de 20 000 ms (estricto, desde el inicio de la quietud) la consulta queda pendiente
  - «Frenada» mantiene el registro; «Pausa personal» quita la frenada y su ubicación y evita
    registrarla de nuevo mientras siga la misma detención; retomar sin responder cierra la consulta y
    mantiene la frenada
  - Tests: picos de 500 ms no resetean; 20 000 ms sin consulta y 20 001 ms con consulta; clasificar
    sin consulta pendiente no cambia el estado
  - _Depends: 2.2_
  - _Requirements: 2.3, 2.4, 4.1, 4.2, 4.3, 4.4, 4.7_

- [x] 2.4 Repositorio de detección: orquestación de sensor, reloj y recorrido
  - Lee el sensor solo mientras hay un recorrido en curso y lo deja de leer al pasar a pausa manual o
    al terminar; cada observación usa el reloj monotónico y un tick de 500 ms
  - Sin muestras por más de 2 s trata al corredor como «en movimiento» para no registrar frenadas
    falsas tras un corte del sensor
  - Conserva las frenadas al reanudar con el mismo id y al terminar; las vacía con un id distinto
  - Las actualizaciones (observaciones y clasificación) son atómicas
  - Tests con tiempo virtual y fakes: sin recorrido no se lee el sensor; 8 s quietos registran una
    frenada con ubicación; mismo id conserva y otro id vacía; un sensor mudo tras «quieto» no
    registra frenada; clasificación concurrente con ticks no pierde actualizaciones
  - _Depends: 1.2, 2.1, 2.3_
  - _Requirements: 1.4, 2.1, 2.2, 2.3, 2.4, 4.2, 4.3, 4.4_

- [x] 2.5 Publicar el estado del recorrido desde el repositorio
  - Al cambiar de categoría la fase publica `ACTIVE` (movimiento o quietud), `STOPPED` (frenada) o
    `PERSONAL_PAUSED` (pausa personal) a través del puerto de publicación
  - Nunca publica sobre una pausa manual; si el puerto la rechaza, el estado del recorrido manda
  - Tests con un publicador falso: frenada → `STOPPED`; pausa personal → `PERSONAL_PAUSED`; retomar →
    `ACTIVE`; con pausa manual no se publica `ACTIVE`
  - _Depends: 2.4_
  - _Requirements: 2.1, 4.3_

- [x] 2.6 (P) Exponer el acelerómetro como flujo de muestras
  - Flujo frío que registra el sensor al colectar y lo da de baja al cancelar, con la magnitud de la
    aceleración y sin permisos
  - Sin acelerómetro el flujo no emite y solo deja un aviso en el log; el recorrido no se afecta
  - Se verifica en un dispositivo o emulador: con un colector activo llegan muestras y al cancelar se
    da de baja
  - _Boundary: AccelerometerMotionSource_
  - _Requirements: 1.1, 1.4_

- [ ] 3. Interfaz: alerta, consulta y superposición

- [x] 3.1 Agregar los recursos de la alerta y la consulta
  - Textos con prefijo `map_` («Frenada detectada», «Semáforo o intersección», pregunta de la
    consulta y sus dos opciones) e ícono de frenada
  - Los recursos compilan y no quedan textos fijos en los componentes
  - _Boundary: res (strings, drawable)_
  - _Requirements: 3.1, 3.4, 4.1_

- [x] 3.2 (P) Alerta animada de frenada
  - Superficie inversa con círculo e ícono, título y detalle; se anuncia a accesibilidad y comunica
    con texto e ícono, no solo color
  - Entra con deslizamiento y desvanecimiento; con «Quitar animaciones» aparece sin transición
  - Previews con el tema; test instrumentado: muestra los dos textos, el ícono y la región en vivo
  - _Depends: 1.1, 3.1_
  - _Boundary: BrakeAlert_
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 3.3 (P) Diálogo de consulta de la detención
  - Pregunta con los botones «Frenada» y «Pausa personal», de al menos 48 dp de alto, que se
    reordenan al crecer la fuente
  - No se cierra por toque exterior ni por atrás; solo al elegir o cuando la detención termina
  - Test instrumentado con fuente al 200 %: botones visibles y sin truncar; cada clic entrega el tipo
    correcto
  - _Depends: 3.1_
  - _Boundary: StopClassificationDialog_
  - _Requirements: 4.1, 4.5, 4.6_

- [x] 3.4 (P) Estado de interfaz de la detección con `suppressed`
  - Alerta cuando aparece una frenada nueva, oculta a los 4 s; diálogo visible mientras haya consulta
    pendiente
  - Con `suppressed` verdadero no se muestran alerta ni diálogo, la frenada igual queda registrada y
    una consulta pendiente se resuelve como frenada; una frenada ocurrida durante la supresión no
    genera alerta tardía
  - Tests con tiempo virtual: alerta se limpia a los 4 000 ms; el diálogo sigue la consulta; con
    `suppressed` se cumple lo anterior
  - _Boundary: StopDetectionViewModel_
  - _Requirements: 3.1, 3.5, 4.1, 4.5, 4.7_

- [x] 3.5 Superposición con alerta y diálogo
  - Pieza que une el estado de interfaz con la alerta y el diálogo y recibe `suppressed` por
    parámetro; sin estado propio en la parte visual, con previews
  - Con `suppressed` verdadero no existen ni la alerta ni el diálogo; falso los muestra según el
    estado
  - _Depends: 3.2, 3.3, 3.4_
  - _Requirements: 3.1, 4.1_

- [ ] 4. Integración con HU-04 y Mapa (La 4.3 se implementa ahora; la 4.4 y la verificación manual de transiciones esperan los cambios de Persona 3 en `develop`)

- [x] 4.1 Adaptar el recorrido y la ubicación de HU-04
  - El «recorrido en curso» entrega el id mientras la sesión está activa, detenida o en pausa
    personal, y `null` en reposo, pausa manual o finalizada (la sesión finalizada es `Idle`; no
    existe `RunState.FINISHED`)
  - La ubicación de la frenada es el último punto de la traza de la sesión activa, o `null`
  - Tests: `Idle` y `PAUSED` → `null`; `ACTIVE`, `STOPPED` y `PERSONAL_PAUSED` → id
  - Ya no depende de nada pendiente: usa `activeSession`, `RunSession.ActiveSession.routePoints` y
    `RunState`, verificados en el código
  - _Depends: 2.4_
  - _Requirements: 1.4, 2.2_

- [x] 4.2 Implementar la publicación de estado sobre `RunSessionRepository`
  - Se implementa con un repositorio falso que replica los orígenes acordados (D1-D3). La verificación manual contra el repositorio real queda pendiente hasta que los cambios de Persona 3 estén en develop.
  - El adaptador llama a las funciones `suspend` del repositorio y devuelve su `Result<Unit>`
    (orígenes acordados): `STOPPED` → `markStopped()` (solo desde `ACTIVE`; deja temporizador y GPS
    corriendo, D3); `ACTIVE` desde `STOPPED` o `PERSONAL_PAUSED` → `markMoving()` (D2; no acepta
    `PAUSED`); `PERSONAL_PAUSED` desde `STOPPED` → `pausePersonally()` (D1; acepta `ACTIVE` y
    `STOPPED`). Ya no usa `resumeSession()`
  - Antes de llamar lee `activeSession.value`: solo actúa si es `ActiveSession` con ese id; en
    `PAUSED` no publica nada, así que no llama a `markMoving()`
  - Si una llamada devuelve `Result.failure`, el repositorio la ignora y reevalúa en el siguiente
    ciclo; la pausa manual (`PAUSED`) nunca se pisa
  - Tests con un repositorio falso que replica los orígenes permitidos del real: cada traducción;
    no pisa `PAUSED`; `Result.failure` se ignora y se reintenta en el siguiente ciclo
  - Verificación manual cuando los cambios de Persona 3 estén en `develop`: en `STOPPED` el
    temporizador y el GPS siguen y en `PERSONAL_PAUSED` se detienen
  - _Depends: 2.5, 4.1_
  - _Requirements: 2.1, 4.3_

- [x] 4.3 Cablear la inyección del repositorio de detección
  - La 4.3 se implementa ahora; la 4.4 y la verificación manual de transiciones esperan los cambios de Persona 3 en `develop`
  - Los adaptadores sobre `RunSessionRepository` inyectan `Provider<RunSessionRepository>` y lo obtienen al usarse, para evitar un ciclo de Hilt cuando `RunSessionRepositoryImpl` inyecte `Optional<StopDetectionRepository>`
  - Módulo con solo los enlaces de `StopDetectionRepository` y de los adaptadores; usa el
    `@ApplicationScope` ya existente
  - Decisión (4.2 de `floor-regularity`): `MotionSource` (`SharedMotionSource`) y `MonotonicClock`
    los provee únicamente el `MotionSensorModule` de `floor-regularity`; esta 4.3 no bindea ninguno
    de los dos
  - La app compila y el repositorio se inyecta como único en toda la aplicación
  - _Depends: 2.6, 4.1, 4.2_
  - _Requirements: 1.4_

- [ ] 4.4 Montar la superposición en el Mapa por parámetro
  - Bloqueada por dependencia con la 4.3: espera el aviso de Persona 3 (cambios D1–D3 en `develop`)
  - Coordinar con Persona 3 (dueña del Mapa): se suma la pieza con `suppressed` sin reescribir la
    pantalla; la alimenta el cierre de HU-06
  - En el Mapa con un recorrido en curso aparece la alerta sobre el panel del temporizador y, a los
    20 s, la consulta
  - _Depends: 3.5, 4.3_
  - _Requirements: 3.1, 4.1_

- [ ] 5. Validación en dispositivo

- [ ] 5.1 Calibrar el umbral de quietud
  - Medir la desviación caminando, trotando y parado en un semáforo, y ajustar la configuración
  - Con los valores definitivos, parado se detecta como frenada a los 8 s y caminando o trotando no;
    queda anotado el valor elegido en `research.md`
  - Verificar con TalkBack el anuncio de la alerta y con «Quitar animaciones» la aparición sin
    transición
  - _Depends: 4.4_
  - _Requirements: 1.1, 1.2, 3.2, 3.3_

## Implementation Notes

- 3.4: `StopDetectionViewModel` quedó sin `@HiltViewModel`/`@Inject` hasta la 4.3; la 4.3 agregó las anotaciones y el binding de `StopDetectionRepository`.
- 3.5: `StopDetectionOverlay` usa `hiltViewModel()` por defecto; con el cableado de la 4.3 ya resuelve el ViewModel (el `TODO(4.3)` se quitó). `StopDetectionOverlayContent` (stateless) está probado en `StopDetectionOverlayTest` (emulador, pasa).
- 3.2 y 3.3: los tests instrumentados (`BrakeAlertTest`, `StopClassificationDialogTest`) se corrieron y pasaron en el emulador (commit 72a8408).
- `FrozenRunSummary`: acordado con Persona 3. Ella agrega y define los campos de frenadas y regularidad; HU-05 produce el conteo de frenadas y HU-05.2 la regularidad. Queda abierto solo cómo llega el dato al resumen en `finishSession()` (si lo lee desde `StopDetectionRepository` o se lo pasa quien finaliza), a definir con Persona 3 al integrar. No bloquea ninguna tarea de esta spec; ver `design.md`.

