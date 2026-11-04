# Investigación · frenadas-pausas-personales

## Análisis de brechas (gap analysis)

_Fecha: 2026-11-04 · Requisitos analizados: `requirements.md` (generados, sin aprobar) · Rama de trabajo: `feat/hu-05-frenadas`_

### 1. Estado actual

**Contexto de steering**
- Existen solo `design-system.md` y `ui-components.md`. **Faltan `product.md`, `tech.md` y `structure.md`** (los archivos base que pide `CLAUDE.md`). Las reglas de estructura salen de `design-system.md` y `ui-components.md`.

**Qué hay en la rama actual (`feat/hu-05-frenadas`)**
- `domain/` y `data/` (incluido `data/sensors/`, `data/location/`) son solo `.gitkeep`: **no hay ningún modelo, repositorio, caso de uso ni sensor**.
- `ui/screens/map/MapScreen.kt` es un placeholder con un `Text("Mapa")`. No hay ViewModel, UiState ni `Content`.
- `ui/components/common/` y `layout/` están vacíos. No existe ningún componente global.
- Hilt está configurado (`TrazoApplication`, `MainActivity`, `HomeViewModel` con `@HiltViewModel`). No hay módulos `@Module` en `di/`.
- `AndroidManifest.xml` no declara permisos ni `<uses-feature>`.
- `strings.xml` solo tiene `app_name`.
- Tests: `HomeViewModelTest` (JUnit 4 puro) y tests instrumentados con `ui-test-junit4`. No hay coroutines-test, MockK ni Robolectric.
- Dependencias: Compose BOM 2025.04.01, Material 3, `material-icons-core`, Navigation Compose, Hilt, Coil, `lifecycle-runtime-compose`. **No hay `kotlinx-coroutines-test`**, ni Room, ni Play Services Location.

**Qué hay en la rama hermana `origin/HU-04-map` (no integrada en esta rama ni en `develop`)**
- Modelos de recorrido en `domain/model/map/`: `RunState` (`IDLE`, `RUNNING`, `PAUSED`, `FINISHED`), `RunSession` (con `routePoints`, `metrics`, `resumedAtMonotonicTimestamp`), `RoutePoint`, `RawLocationPoint` (lat, lon, precisión, velocidad y timestamp monotónico).
- Contratos: `RunSessionRepository` (`activeSession: StateFlow<RunSession?>`), `LocationClient` (`getLocationUpdates(intervalMillis): Flow<RawLocationPoint>`), casos de uso `Pause/Resume/FinishRunUseCase`.
- Implementación: `data/location/AndroidLocationClient.kt`, `data/di/LocationModule.kt`, y permisos `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`.
- `MapScreen` sigue siendo un placeholder en esa rama.

**Lo que dice el diseño sobre esta funcionalidad** (`design-system.md` y la skill `trazo-design-system`, §6.8)
- Componente `BrakeAlert` en `screens/map/components/`: fondo `inverseSurface` (Primary900), círculo blanco de 36 dp con ícono de frenada en Primary950, forma medium, elevación level3, `liveRegion = Polite`. Entra con slide + fade y **se cierra sola a los 4 s**. Se coloca 12 dp sobre el panel del temporizador.
- «Si la detención dura más de 20 s, se reemplaza por un diálogo con las opciones "Frenada" y "Pausa personal"».
- Pin de frenada en el mapa: Error600, círculo de 22 dp con borde blanco de 3 dp y un guion blanco (`brakePin`). Es el único uso permitido del rojo fuera de errores.
- Referencia a ícono `octagon` con guion (frenada) y un `R.drawable.ic_brake` en el ejemplo. **El drawable no existe en el repo.**
- Checklist de accesibilidad: probar con fuente al 200 % y con «Quitar animaciones»; las alertas sí se anuncian.
- `ui-components.md` ya lista `ConfirmDialog` (common, `AlertDialog`, confirmación de dos opciones) como componente global previsto, pero **no existe todavía**.

### 2. Mapa de requisitos y recursos

| Req. | Necesidad técnica | Qué hay | Brecha |
|---|---|---|---|
| 1.1–1.2 | Máquina de estados `Movimiento continuo` / `Frenada` | Nada | **Missing**: modelo de estado y lógica de transición (dominio puro, testeable sin Android) |
| 1.1 | Umbral de detención sobre el acelerómetro | Nada | **Missing** + **Research Needed**: el requisito delega el umbral al diseño (ver §4) |
| 1.3 | Temporizador de 8 s con cancelación al retomar el movimiento | Nada | **Missing**: debounce basado en timestamps de los eventos del sensor |
| 1.4 | Solo actúa con recorrido en curso | `RunState.RUNNING` / `RunSessionRepository.activeSession` solo en `origin/HU-04-map` | **Constraint**: depende de HU-04 (**Unknown**: fecha de integración y si `PAUSED` cuenta como «en curso») |
| 2.1 | Pasar a `Frenada` a los 8 s | Nada | **Missing** (lógica de dominio) |
| 2.2 | Registrar la ubicación de la detención | `RawLocationPoint` / `LocationClient` solo en `origin/HU-04-map` | **Constraint**: hace falta la última ubicación conocida. **Unknown**: qué se guarda si no hay fix (precisión baja o sin señal) |
| 2.3 | Una sola frenada por detención continua | Nada | **Missing**: identidad de la frenada (id) y bandera de «ya registrada». Cuidar la idempotencia ante eventos repetidos |
| 2.4 | Conservar la frenada al retomar el movimiento | Nada | **Missing**: la lista de frenadas vive más que el estado instantáneo |
| 3.1 | Alerta animada con el texto «Se detectó una frenada por semáforo o intersección» | `BrakeAlert` descrito en el diseño, sin código | **Missing**: composable, `ic_brake`, strings. **Conflicto de texto** (ver §4) |
| 3.2 | Sin animación con «Quitar animaciones» | — | **Missing** + **Research Needed**: cómo leer la preferencia en Compose (`ANIMATOR_DURATION_SCALE` vía `Settings.Global`; ver §4) |
| 3.3 | Anunciar la alerta a accesibilidad | Patrón en el diseño: `liveRegion = Polite` | Sin brecha técnica |
| 3.4 | Texto + ícono, no solo color | Diseño ya lo prevé (círculo blanco + ícono + texto) | Sin brecha de diseño; **Missing** el `ic_brake` |
| 4.1 | Diálogo a los 20 s con «Frenada» / «Pausa personal» | `ConfirmDialog` previsto pero inexistente | **Missing**: diálogo propio de dos opciones (no es un confirm/cancel clásico). **Conflicto**: «más de 20 s» vs. el diseño «20 s» (ver §4) |
| 4.2 | «Frenada» conserva el registro | Nada | **Missing** (evento del ViewModel + dominio) |
| 4.3–4.4 | «Pausa personal» elimina la frenada y su ubicación de las registradas | Nada | **Missing**: operación de descarte sobre la lista. **Unknown**: ¿se descarta la ya registrada a los 8 s o se registra recién al confirmar? (el req. 2.2 la registra a los 8 s, así que el descarte es una baja) |
| 4.5 | Cerrar el diálogo al elegir | — | Sin brecha técnica |
| 4.6 | Texto accionable, fuente al 200 %, objetivos de 48 dp | Reglas generales del diseño | **Constraint**: botones con `heightIn(min = 48.dp)` y texto sin truncar; verificar con fuente al 200 % |
| Todos | Sensor: acelerómetro | `data/sensors/` vacío; sin dependencias nuevas necesarias (`SensorManager` es del framework) | **Missing**: `SensorClient`/`AccelerometerDataSource` como `callbackFlow`. **Constraint**: la gestión del sensor (registro/baja, rate) es de HU-05.2 |
| Todos | Hilo y ciclo de vida: detectar solo en recorrido en curso | — | **Unknown** + **Research Needed**: si la detección corre con la app en segundo plano o pantalla apagada (ver §4) |

### 3. Opciones de implementación

#### Opción A: Extender componentes existentes (todo dentro de Mapa)
- Poner la lógica de detección en el futuro `MapViewModel`, el listener del acelerómetro dentro de `MapScreen` / ViewModel y la alerta y el diálogo en `screens/map/components/`.
- ✅ Menos archivos, encaja con la regla «lo nuevo nace en la pantalla».
- ❌ `MapScreen` y su ViewModel **todavía no existen** (son placeholder en todas las ramas): se estaría construyendo sobre algo que HU-04 va a definir, con riesgo de conflictos de merge.
- ❌ Mezcla la regla de negocio (8 s / 20 s) con el estado de la pantalla; difícil de testear sin Android.
- ❌ HU-05.2 (pausa, regularidad, sensor) va a necesitar el mismo sensor y quedaría acoplada.

#### Opción B: Componentes nuevos y desacoplados
- **Dominio** (`domain/model/stops/`, `domain/usecases/` o clase de dominio): `MovementState`, `BrakeEvent`/`Stop` (id, ubicación, estado), `StopDetector` con un reloj inyectado. Función pura de transición, sin dependencias de Android.
- **Datos** (`data/sensors/`): `AccelerometerDataSource` que expone un `Flow` de muestras de movimiento, más un `@Module` Hilt.
- **Presentación** (`screens/map/components/`): `BrakeAlert` y `StopClassificationDialog`; el ViewModel de Mapa solo consume el estado del detector y expone `BrakeAlertUi` / `isClassificationVisible`.
- ✅ Regla de negocio testeable con JUnit puro (reloj falso), alineada con `HomeViewModelTest`.
- ✅ La interfaz del sensor la puede reutilizar HU-05.2 (regularidad) sin tocar esta spec.
- ❌ Más archivos y contratos que definir; hay que coordinar con HU-04 (ubicación y estado del recorrido) y con HU-05.2 (dueña del sensor).

#### Opción C: Híbrida (recomendable para evaluar en el diseño)
- Núcleo en dominio (Opción B) con **puertos mínimos**: `MotionSource` (flujo de «moviéndose / quieto» o de magnitudes), `LocationProvider` (última ubicación) y `RunStatusProvider` (recorrido en curso). Mientras HU-04 / HU-05.2 no estén integradas, usar implementaciones temporales (fakes o un `AccelerometerDataSource` simple) detrás de esas interfaces.
- UI (alerta y diálogo) en `screens/map/components/`, conectada al ViewModel de Mapa cuando exista; mientras tanto con `@Preview` y estado de prueba, como pide `ui-components.md` (`Content` con previews antes del ViewModel).
- Fases: (1) dominio + tests; (2) UI con previews; (3) sensor real; (4) integración con HU-04 al mergear.
- ✅ Permite avanzar sin esperar a HU-04, con una frontera clara para integrar.
- ❌ Requiere definir bien los puertos para no rehacer cuando HU-04 / HU-05.2 definan los suyos.

### 4. Preguntas abiertas, conflictos y «Research Needed»

**Conflictos entre requisitos y documentos de diseño (a resolver antes de diseñar)**
1. **Texto de la alerta.** Los requisitos (3.1) piden «Se detectó una frenada por semáforo o intersección». El sistema de diseño (§6.8) define título «Frenada detectada» + detalle «Semáforo o intersección». Hay que decidir cuál manda (los requisitos son la fuente de verdad de la historia; el título + detalle del diseño podría armarse como una sola frase o actualizarse el diseño).
2. **Cierre de la alerta.** El diseño dice que se cierra sola a los 4 s y que a los 20 s se reemplaza por el diálogo. Los requisitos no definen cuánto dura la alerta ni qué pasa si el diálogo aparece mientras la alerta está visible. **Research Needed / decisión de diseño.**
3. **«Más de 20 s» vs «20 s».** El requisito 4.1 usa «más de 20 segundos»; el diseño, «20 s». Definir si el umbral es `> 20 s` o `≥ 20 s` (¿20 s desde el inicio de la detención o 20 s desde que se registró la frenada, es decir 28 s?). **Ambigüedad real**: los escenarios 4–6 dicen «sin movimiento más de 20 segundos», lo que apunta a 20 s totales desde que se quedó quieto.
4. **Qué pasa si el corredor no responde al diálogo.** Los requisitos no dicen si se cierra, persiste o la frenada queda como está. El requisito 2.4 sugiere que, al retomar el movimiento, la frenada se conserva; falta definir el destino del diálogo en ese caso (¿se cierra solo al retomar el movimiento?). **Unknown.**
5. **Inconsistencia de nomenclatura.** El ítem «Frenada» del diálogo y el estado «Frenada» comparten nombre. Revisar con UX si «Frenada» como opción puede confundirse (dentro del alcance solo hay que respetar los textos pedidos).

**Research Needed (para la fase de diseño)**
- **Umbral de detención** (parámetro delegado al diseño): qué señal del acelerómetro usar (`TYPE_ACCELEROMETER` con magnitud menos gravedad, `TYPE_LINEAR_ACCELERATION` si existe, o varianza en una ventana), valores de umbral y ventana de suavizado. Hay que **calibrar con datos reales** de carrera/caminata/parado: el acelerómetro de un corredor tiene vibración incluso al estar parado en un semáforo (trotar en el lugar, bolsillo o brazalete). Tasa de muestreo (`SENSOR_DELAY_NORMAL` / `UI` / `GAME`) frente a batería.
- **Respaldo con GPS:** `RawLocationPoint.speedMetersPerSecond` podría ayudar a confirmar la detención y reducir falsos positivos. Evaluar si se combina con el acelerómetro o se usa solo el acelerómetro (como pide la historia).
- **Ejecución en segundo plano / pantalla apagada:** el corredor suele llevar el teléfono en el bolsillo con la pantalla apagada. Los sensores dejan de entregar eventos en algunos dispositivos si no hay un servicio en primer plano o un `WakeLock`; relacionado con el tipo de servicio que defina HU-04 para el rastreo. Probablemente requiera un foreground service (permiso `FOREGROUND_SERVICE_*` en API 34+).
- **Permisos:** el acelerómetro no requiere permiso, pero en Android 12+ (API 31) el muestreo por encima de 200 Hz exige `HIGH_SAMPLING_RATE_SENSORS`. Con tasas normales no hace falta.
- **Dispositivos sin acelerómetro:** definir el comportamiento (la detección queda inactiva sin romper el recorrido). `minSdk = 24`.
- **Preferencia «Quitar animaciones»:** en Compose no hay API directa. Opciones: leer `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` (patrón habitual) o `AccessibilityManager`. El checklist del diseño lo exige, así que hay que fijar un `rememberReduceMotion()`/utilidad compartida con la celebración de «Meta cumplida».
- **Anuncio de accesibilidad:** `liveRegion = Polite` en la alerta; para el diálogo, el foco y el título deben anunciarse al abrirse. Verificar con TalkBack.
- **Persistencia de las frenadas:** esta spec solo las registra; la persistencia y el conteo pertenecen a «Guardar recorrido» y resultados. Falta acordar el contrato de datos de salida (`List<BrakeEvent>`) con `save-run`, que espera «frenadas» como dato del recorrido finalizado.
- **Pin de frenada en el mapa:** el diseño lo menciona, pero la spec no lo pide (queda fuera del alcance: «presentación de frenadas en resultados e historial»). Confirmar si el pin en vivo entra en esta historia o en HU-04.

### 5. Complejidad y riesgo

- **Esfuerzo: M (3–7 días).** La lógica de estados es pequeña, pero hay que construir desde cero el sensor, el dominio, la alerta, el diálogo y las pruebas, y falta calibrar el umbral con pruebas reales en dispositivo.
- **Riesgo: Medio.** Tecnología conocida (`SensorManager`, Compose, Hilt), pero con dos incógnitas: la **calibración del umbral** (falsos positivos / negativos con el movimiento del corredor) y la **dependencia con HU-04 / HU-05.2** (estado del recorrido, ubicación, dueño del sensor, ejecución en segundo plano). Si el sensor debe correr en segundo plano, el riesgo sube a Alto.

### 6. Recomendaciones para la fase de diseño

- **Enfoque preferido:** Opción C (híbrida). Núcleo de dominio puro con reloj inyectado y puertos para sensor, ubicación y estado del recorrido; UI en `screens/map/components/` (`BrakeAlert`, `StopClassificationDialog`).
- **Decisiones clave a tomar en el diseño:**
  1. Fuente y fórmula del umbral de detención (y si se complementa con la velocidad del GPS).
  2. Semántica exacta de los 8 s y los 20 s (medidos desde el inicio de la quietud) y de «más de» (estricto).
  3. Máquina de estados completa: `MOVING` → `STOPPED(brakeRegistered)` → `CLASSIFYING` → (`confirmed` | `personalPause`) → `MOVING`.
  4. Modelo `BrakeEvent` (id, ubicación, timestamp, estado) y contrato con `save-run`.
  5. Qué pasa con el diálogo y la alerta si se retoma el movimiento, o si el recorrido se pausa o finaliza.
  6. Resolver el conflicto de texto de la alerta (§4.1) y la duración de la alerta (§4.2).
  7. Utilidad compartida para «Quitar animaciones».
- **Coordinar antes de implementar:** integración con `origin/HU-04-map` (`RunState`, `RunSessionRepository`, `LocationClient`), y con HU-05.2 (propiedad del sensor y su ciclo de vida).
- **Pruebas:** agregar `kotlinx-coroutines-test` (hoy ausente) para probar los temporizadores de 8 s / 20 s con tiempo virtual; tests de Compose para el diálogo con fuente al 200 % y objetivos de 48 dp.
- **Archivos de steering faltantes:** conviene generar `product.md`, `tech.md` y `structure.md` con `/kiro-steering` para que el diseño no dependa solo de los documentos de UI.
