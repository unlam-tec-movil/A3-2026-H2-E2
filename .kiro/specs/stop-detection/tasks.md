# Implementation Plan

> **Orden de trabajo:** dominio, sensor y UI no dependen de HU-04 (tareas 1 a 3). Las tareas de
> integración (4.x) requieren el contrato de HU-04 de Persona 3; la 4.3 además espera la respuesta a
> los supuestos pendientes con el líder de `design.md` («Riesgos y preguntas abiertas»).

- [ ] 1. Fundaciones: utilidad compartida, pruebas con tiempo virtual y contratos del dominio

- [ ] 1.1 Crear la utilidad compartida «Quitar animaciones»
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

- [ ] 1.3 Definir los modelos, la configuración y los puertos del dominio
  - Modelos: estado de movimiento, tipo de detención, ubicación de la frenada, evento de frenada,
    muestra de movimiento, fases de la máquina de estados y estado agregado con sus derivados
  - Configuración calibrable con los umbrales y tiempos (8 s, 20 s, confirmación de 1,5 s, tick de
    500 ms, ventana de 1 s, caducidad de muestras de 2 s)
  - Puertos: fuente de movimiento, recorrido en curso, ubicación, publicación de estado del recorrido
    y reloj monotónico; contrato de salida con el estado y la clasificación de la detención actual
  - El dominio compila sin importar Android, `data` ni `ui`; `movement` y la consulta pendiente se
    derivan de la fase en un test unitario
  - _Boundary: Domain (model/stops, repository)_
  - _Requirements: 1.1, 2.2, 2.4, 4.1_

- [ ] 2. Núcleo: detección, sensor y repositorio

- [ ] 2.1 (P) Estimar si el corredor está quieto con una ventana de aceleración
  - Decide «quieto» con la desviación de la magnitud en una ventana de 1 s, sin depender de la
    orientación del teléfono
  - Con la ventana incompleta el resultado es «en movimiento»
  - Tests: serie constante → quieto; desviación mayor al umbral → en movimiento; ventana de menos de
    1 000 ms → en movimiento
  - _Boundary: StillnessEstimator_
  - _Requirements: 1.1, 1.2_

- [ ] 2.2 (P) Máquina de estados: de movimiento a frenada registrada
  - Reductor puro con el tiempo recibido en cada observación y ids de frenada inyectados
  - A los 8 000 ms desde que comenzó la quietud pasa a «Frenada» y registra una única frenada con la
    ubicación recibida (o sin ella si no hay)
  - Una detención menor a 8 s no registra nada y vuelve a «Movimiento continuo»
  - Tests: 7 999 ms retoma sin frenada; 8 000 ms registra una frenada con ubicación; 30
    observaciones seguidas quietas registran una sola
  - _Boundary: StopDetector_
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3_

- [ ] 2.3 Máquina de estados: reanudación, consulta de 20 s y clasificación
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

- [ ] 2.4 Repositorio de detección: orquestación de sensor, reloj y recorrido
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

- [ ] 2.5 Publicar el estado del recorrido desde el repositorio
  - Al cambiar de categoría la fase publica `ACTIVE` (movimiento o quietud), `STOPPED` (frenada) o
    `PERSONAL_PAUSED` (pausa personal) a través del puerto de publicación
  - Nunca publica sobre una pausa manual; si el puerto la rechaza, el estado del recorrido manda
  - Tests con un publicador falso: frenada → `STOPPED`; pausa personal → `PERSONAL_PAUSED`; retomar →
    `ACTIVE`; con pausa manual no se publica `ACTIVE`
  - _Depends: 2.4_
  - _Requirements: 2.1, 4.3_

- [ ] 2.6 (P) Exponer el acelerómetro como flujo de muestras
  - Flujo frío que registra el sensor al colectar y lo da de baja al cancelar, con la magnitud de la
    aceleración y sin permisos
  - Sin acelerómetro el flujo no emite y solo deja un aviso en el log; el recorrido no se afecta
  - Se verifica en un dispositivo o emulador: con un colector activo llegan muestras y al cancelar se
    da de baja
  - _Boundary: AccelerometerMotionSource_
  - _Requirements: 1.1, 1.4_

- [ ] 3. Interfaz: alerta, consulta y superposición

- [ ] 3.1 Agregar los recursos de la alerta y la consulta
  - Textos con prefijo `map_` («Frenada detectada», «Semáforo o intersección», pregunta de la
    consulta y sus dos opciones) e ícono de frenada
  - Los recursos compilan y no quedan textos fijos en los componentes
  - _Boundary: res (strings, drawable)_
  - _Requirements: 3.1, 3.4, 4.1_

- [ ] 3.2 (P) Alerta animada de frenada
  - Superficie inversa con círculo e ícono, título y detalle; se anuncia a accesibilidad y comunica
    con texto e ícono, no solo color
  - Entra con deslizamiento y desvanecimiento; con «Quitar animaciones» aparece sin transición
  - Previews con el tema; test instrumentado: muestra los dos textos, el ícono y la región en vivo
  - _Depends: 1.1, 3.1_
  - _Boundary: BrakeAlert_
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 3.3 (P) Diálogo de consulta de la detención
  - Pregunta con los botones «Frenada» y «Pausa personal», de al menos 48 dp de alto, que se
    reordenan al crecer la fuente
  - No se cierra por toque exterior ni por atrás; solo al elegir o cuando la detención termina
  - Test instrumentado con fuente al 200 %: botones visibles y sin truncar; cada clic entrega el tipo
    correcto
  - _Depends: 3.1_
  - _Boundary: StopClassificationDialog_
  - _Requirements: 4.1, 4.5, 4.6_

- [ ] 3.4 (P) Estado de interfaz de la detección con `suppressed`
  - Alerta cuando aparece una frenada nueva, oculta a los 4 s; diálogo visible mientras haya consulta
    pendiente
  - Con `suppressed` verdadero no se muestran alerta ni diálogo, la frenada igual queda registrada y
    una consulta pendiente se resuelve como frenada; una frenada ocurrida durante la supresión no
    genera alerta tardía
  - Tests con tiempo virtual: alerta se limpia a los 4 000 ms; el diálogo sigue la consulta; con
    `suppressed` se cumple lo anterior
  - _Boundary: StopDetectionViewModel_
  - _Requirements: 3.1, 3.5, 4.1, 4.5, 4.7_

- [ ] 3.5 Superposición con alerta y diálogo
  - Pieza que une el estado de interfaz con la alerta y el diálogo y recibe `suppressed` por
    parámetro; sin estado propio en la parte visual, con previews
  - Con `suppressed` verdadero no existen ni la alerta ni el diálogo; falso los muestra según el
    estado
  - _Depends: 3.2, 3.3, 3.4_
  - _Requirements: 3.1, 4.1_

- [ ] 4. Integración con HU-04 y Mapa (bloqueada hasta el contrato de Persona 3)

- [ ] 4.1 Adaptar el recorrido y la ubicación de HU-04
  - El «recorrido en curso» entrega el id mientras la sesión está activa, detenida o en pausa
    personal, y `null` en reposo, pausa manual o finalizada
  - La ubicación de la frenada es el último punto de la traza de la sesión activa, o `null`
  - Tests: reposo, `PAUSED` y `FINISHED` → `null`; `ACTIVE`, `STOPPED` y `PERSONAL_PAUSED` → id
  - _Depends: 2.4_
  - _Requirements: 1.4, 2.2_

- [ ] 4.2 Implementar la publicación de estado sobre `RunSessionRepository`
  - Usa las transiciones acordadas con Persona 3: `markStopped()` (`ACTIVE` → `STOPPED`),
    `pausePersonally()` (`ACTIVE` o `STOPPED` → `PERSONAL_PAUSED`, solo se llama desde `STOPPED`) y
    `markMoving()` (`STOPPED` o `PERSONAL_PAUSED` → `ACTIVE`); cualquier otro origen devuelve
    `Result.failure`
  - **Dependiente de los supuestos pendientes con el líder** (temporizador y GPS en `STOPPED` y en
    `PERSONAL_PAUSED`, finalizar desde los cuatro estados, controles por estado); si cambian, se
    revisa esta tarea
  - Si una transición devuelve `Result.failure`, el repositorio la ignora y reevalúa en el siguiente
    ciclo; la sesión es la del id y la pausa manual (`PAUSED`) nunca se pisa
  - Un test con el repositorio real o falso verifica que no pisa `PAUSED` y que `Result.failure` se
    ignora y se reintenta en el siguiente ciclo
  - _Depends: 2.5, 4.1_
  - _Requirements: 2.1, 4.3_

- [ ] 4.3 Cablear la inyección del repositorio de detección
  - Módulo con el reloj monotónico, el alcance de aplicación de HU-04 y los enlaces de repositorio,
    sensor y adaptadores
  - La app compila y el repositorio se inyecta como único en toda la aplicación
  - _Depends: 2.6, 4.1, 4.2_
  - _Requirements: 1.4_

- [ ] 4.4 Montar la superposición en el Mapa por parámetro
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
