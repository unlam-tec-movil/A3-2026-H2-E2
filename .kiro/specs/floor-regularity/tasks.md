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

> **Orden de trabajo:** el dominio, el sensor compartido y el aviso no dependen de otras personas
> (tareas 1 a 3). La integración (tarea 4) necesita acuerdos abiertos: el binding de `MotionSource` y
> `MonotonicClock` con el módulo de `stop-detection` (su tarea 4.3; resuelto en la 4.2: los provee
> `MotionSensorModule`) y el adaptador del estado del
> recorrido de HU-04. Esta spec no modifica `stop-detection`, `FrozenRunSummary` ni `MapScreen`.

- [x] 1. Fundaciones: modelos, configuración y puertos del dominio

- [x] 1.1 Definir los modelos, la configuración y los puertos de la regularidad
  - Configuración con tramo de 10 s, umbral de vibración baja (valor inicial provisional, marcado
    como sin respaldo) y hueco máximo entre muestras
  - Clase de tramo (vibración baja o alta), instantánea del recorrido en curso (id y estado) y estado
    de regularidad con tramos completos, tramos de vibración baja y disponibilidad del sensor
  - Porcentaje entero derivado: `null` sin tramos o sin sensor; 8 de 10 tramos da 80
  - Contratos de salida y puertos: repositorio de regularidad, fuente del estado del recorrido y
    disponibilidad del acelerómetro
  - Compila sin dependencias de Android y las pruebas del porcentaje (8 de 10, sin tramos, redondeo)
    pasan
  - _Boundary: RegularityConfig, RegularityState, ActiveRunStateSource, MotionSensorAvailability, RegularityRepository_
  - _Requirements: 3.3, 3.4, 3.9, 4.2, 4.3, 5.4, 5.6_

- [x] 2. Núcleo: medidor, sensor compartido y repositorio

- [x] 2.1 (P) Medidor de tramos de 10 segundos
  - Agrupa las muestras en tramos consecutivos y clasifica cada tramo cerrado por el desvío estándar
    de la magnitud contra el umbral
  - Un tramo abierto no se cuenta; un hueco mayor al máximo entre muestras descarta el tramo; el
    descarte manual deja el medidor sin tramo abierto
  - Pruebas con tiempo explícito: 10 tramos con 8 de vibración baja; tramo de 9,9 s sin cerrar; hueco
    de muestras; descarte manual; umbral a ambos lados
  - _Boundary: RegularityMeter_
  - _Depends: 1.1_
  - _Requirements: 3.1, 3.2, 3.7, 3.8, 3.9, 7.2_

- [x] 2.2 (P) Compartir la lectura del acelerómetro entre consumidores
  - Un único listener mientras haya al menos un colector; se da de baja cuando se va el último y
    vuelve a registrarse si aparece otro
  - Sin acelerómetro el flujo queda vacío y los colectores no fallan
  - Pruebas con un origen falso que cuenta registros: dos colectores registran una vez; al cancelar
    ambos se da de baja; volver a colectar registra de nuevo
  - _Boundary: SharedMotionSource_
  - _Requirements: 5.3, 5.7, 6.1, 6.7_

- [x] 2.3 (P) Informar si el dispositivo tiene acelerómetro
  - Devuelve verdadero solo si existe el sensor; falso si no hay gestor de sensores
  - La prueba instrumentada confirma el valor en el emulador y no lanza excepciones
  - _Boundary: AndroidMotionSensorAvailability_
  - _Depends: 1.1_
  - _Requirements: 5.1, 5.7_

- [x] 2.4 Repositorio de regularidad: estado del recorrido, sensor y tramos
  - Mide solo con el recorrido activo; en pausa manual, frenada o pausa personal descarta el tramo
    abierto y conserva los tramos cerrados
  - Reinicia el estado cuando cambia el id del recorrido; lo conserva al reanudar y tras finalizar o
    descartar, hasta el siguiente recorrido
  - Deja de colectar el sensor cuando el recorrido pasa a pausa manual, finaliza o se descarta
  - Sin sensor: sin tramos y porcentaje `null`, sin fallas
  - Pruebas con tiempo virtual y fakes: solo cuenta en activo; pausa y reanudación; id nuevo; el valor
    sigue disponible al pasar a «sin recorrido»; sin sensor
  - _Boundary: RegularityRepositoryImpl_
  - _Depends: 2.1, 2.2, 2.3_
  - _Requirements: 1.4, 2.2, 2.3, 3.5, 3.6, 4.1, 4.4, 4.5, 5.3, 5.4, 5.7, 6.2, 6.3, 6.6_

- [x] 3. Verificación sobre stop-detection y aviso

- [x] 3.1 (P) Verificar con pruebas que la pausa manual y la reanudación no afectan a las frenadas
  - Sin modificar `stop-detection`: pausar durante una detención de menos de 8 s no registra frenada
    ni pausa personal; las frenadas previas se conservan al pausar y al reanudar
  - Al reanudar, la fase vuelve a «movimiento» y la quietud anterior no se arrastra
  - Las pruebas nuevas pasan junto con las existentes de `stop-detection`
  - _Boundary: StopDetectionRepositoryImpl (solo pruebas)_
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 7.4_

- [x] 3.2 (P) Aviso de falta de acelerómetro
  - Textos del aviso y de accesibilidad en español rioplatense y un ícono de sensor apagado
  - Componente con ícono y texto, anunciado como región viva, legible con fuente al 200 % y con
    vista previa; no se cablea en la pantalla del Mapa
  - La prueba instrumentada confirma título, detalle e ícono presentes
  - _Boundary: MotionSensorMissingNotice, strings_
  - _Requirements: 5.1, 5.2, 5.6_

- [x] 4. Integración

- [x] 4.1 Adaptar el estado del recorrido de HU-04
  - Convierte la sesión activa en instantánea con id y estado, y la sesión inactiva en ausencia de
    recorrido
  - No publica ni cambia estados: solo lee
  - La prueba con un repositorio falso cubre activo, pausado, detenido, pausa personal y sin sesión
  - _Boundary: RunSessionActiveRunStateSource_
  - _Depends: 1.1_
  - _Requirements: 1.4, 3.5_

- [x] 4.2 Coordinar el binding del sensor y del reloj con stop-detection
  - Acordar con quien tenga `stop-detection` (Persona 4) que su módulo de inyección no bindee el
    flujo de muestras y que el reloj monotónico tenga un único proveedor
  - Dejar anotado el resultado en este archivo antes de seguir con la 4.3
  - La decisión queda registrada y `stop-detection` tiene un único binding de muestras
  - _Boundary: coordinación (sin código)_
  - _Requirements: 6.1_
  - **Nota 4.2 (decisión registrada):** `MotionSensorModule` de `floor-regularity` es el único
    proveedor de `MotionSource` (`SharedMotionSource`) y de `MonotonicClock`. El módulo de
    `stop-detection` (su tarea 4.3) solo bindea `StopDetectionRepository` y los adaptadores, sin
    bindear `MotionSource` ni `MonotonicClock`. La 4.3 de esta spec debe proveer ambos.

- [x] 4.3 Cablear la inyección
  - El flujo de muestras inyectado a ambos consumidores es el compartido; la disponibilidad, el
    adaptador de estado y el repositorio quedan enlazados
  - La compilación de Hilt pasa sin bindings duplicados
  - _Boundary: MotionSensorModule_
  - _Depends: 2.2, 2.3, 2.4, 4.1, 4.2_
  - _Requirements: 6.1, 6.7_

- [x] 4.4 Prueba de ciclo de vida con ambos consumidores
  - Con el sensor compartido real sobre un origen falso, `stop-detection` y regularidad activos: un
    único listener en activo; cero en pausa manual; uno al reanudar; cero al finalizar y al descartar
  - La prueba pasa y cubre los escenarios E8 a E12
  - _Boundary: SharedMotionSource, RegularityRepositoryImpl, StopDetectionRepositoryImpl (solo pruebas)_
  - _Depends: 4.3_
  - _Requirements: 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 7.3_

- [ ] 5. Validación en dispositivo

- [ ] 5.1 Calibrar el umbral de vibración baja
  - Correr en un teléfono real sobre piso parejo y sobre piso irregular y ajustar el umbral;
    registrar el valor final y cuánto influye la quietud previa a una frenada
  - El umbral acordado reemplaza al provisional y las pruebas siguen pasando
  - Nota: desde que `TrazoApplication` inyecta `RegularityMonitor`, la regularidad se mide en la app real y aparece en el Resultado, que es lo que hacía falta para calibrar en un teléfono
  - _Boundary: RegularityConfig_
  - _Depends: 4.3_
  - _Requirements: 3.9_

- [ ] 5.2 Verificar en dispositivo el apagado del sensor y el caso sin acelerómetro
  - Con un recorrido real: pausar, reanudar, finalizar y descartar; el sensor se da de baja en cada
    caso. En un dispositivo o emulador sin acelerómetro el aviso se puede mostrar y el recorrido sigue
  - Cobertura de las pruebas de esta spec mayor a 80 %
  - _Depends: 4.3_
  - _Requirements: 5.1, 5.7, 6.2, 6.3, 6.4, 7.1_

- [x] 6. Acuerdos con otras personas (sin código)

- [x] 6.1 Cerrar los acuerdos abiertos
  - Cerrados el 2026-11-23: ver «Acuerdos cerrados» en `requirements.md` y `design.md`
  - Persona 3: cómo llega el valor al resumen en `finishSession()` y frenadas «sin dato» o 0
  - `save-run`: cuándo lee la regularidad (antes de iniciar el siguiente recorrido)
  - Pantallas del Mapa y resultado: quién muestra el aviso y cómo se presenta «sin dato»
  - Los acuerdos quedan escritos en `requirements.md` y `design.md`
  - _Requirements: 4.1, 4.4, 4.6, 5.5_

## Implementation Notes

- El umbral de 2,0 m/s² es provisional y no está acordado.
- El requisito 4.6 («sin dato» con texto) lo cumple la presentación de otras personas; esta spec solo
  entrega `null`.
- `MotionSensorMissingNoticeTest` (3.2) compila pero no se ejecutó: falta correrla en emulador o dispositivo.
