# Research & Design Decisions: live-route-tracking (HU-04)

## Summary

- **Feature**: `live-route-tracking` (HU-04 — Seguir el recorrido en vivo)
- **Discovery Scope**: Greenfield / Extension (Integración de capa de dominio, fuente de ubicación y
  UI en Scaffold existente)
- **Key Findings**:
    - Arquitectura Hexagonal estricta: separación de lógica de seguimiento y filtrado GPS en
      `domain/`, obtención de ubicaciones de hardware en `data/location/`, y presentación en
      `ui/screens/map/` con Jetpack Compose y `StateFlow`.
    - La sesión en curso debe residir en un gestor en memoria (`RunSessionRepository` /
      `RunSessionManager`) provisto como `@Singleton` por Hilt, garantizando que el cambio de
      pestañas en `BottomNavBar` (`restoreState = true`, `saveState = true`) no reinicie ni destruya
      el estado de seguimiento.
    - Reglas GPS inmutables: filtro de precisión a 30 metros, temporizador de 15 segundos sin
      lecturas válidas desde el último punto GPS válido para disparar "Sin señal GPS", e
      independencia absoluta entre el indicador visual de barras y el filtro de precisión.
    - Límites de responsabilidades claros con funcionalidades adyacentes: `start-run` provee el
      inicio y la selección de meta (3, 5, 10 km); `save-run` es dueña exclusiva del formulario de
      guardado (título obligatorio, destacados, hasta 2 fotos) y la persistencia local en Room;
      `live-route-tracking` se limita a proveer el resumen inmutable (`FrozenRunSummary`) al
      finalizar.

---

## Research Log

### 1. Gestión de estado durante la navegación entre pestañas

- **Context**: Requisito 7.1–7.4: el corredor puede cambiar a las pestañas "Historial" o "Inicio" y
  volver a "Mapa" sin perder el estado (activo o pausado), con tiempo y distancia acumulándose si
  está activo o congelados si está pausado.
- **Sources Consulted**: Arquitectura de navegación Jetpack Compose (`TrazoApp.kt`), ciclo de vida
  de ViewModels en NavHost con `saveState = true`.
- **Findings**: Los composables de pantalla en Compose dentro de un `NavHost` pueden ser destruidos
  o recreados si no están retenidos en el back stack o si el scope del ViewModel es local al
  composable de destino. Para garantizar la continuidad del temporizador y el registro GPS aún
  cuando `MapScreen` no esté visible, la orquestación del tracking debe vivir en la capa de
  datos/dominio (`RunSessionManager` como singleton en Hilt). `MapViewModel` se conecta como
  observador reactivo de este gestor.
- **Implications**: `MapViewModel` delega el ciclo de vida del tracking a casos de uso que operan
  sobre `RunSessionRepository`. La navegación no detiene corrutinas del tracking a menos que se
  invoque explícitamente `pause` o `finish`.

### 2. Filtrado GPS y temporizador de pérdida de señal

- **Context**: Requisitos 3.1–3.5 y 4.1–4.3: descartar puntos con precisión > 30 m y detectar 15
  segundos consecutivos sin puntos válidos desde la última lectura válida.
- **Sources Consulted**: Android `Location` API (`getAccuracy()`, `getTime()`), patrones de flujos
  reactivos con Kotlin Coroutines / `Flow`.
- **Findings**:
    - Un punto GPS descartado (> 30 m) no actualiza la marca de tiempo de "última ubicación válida".
    - El temporizador de 15 segundos debe resetearse únicamente al ingresar un punto cuya precisión
      sea ≤ 30 m.
    - La emisión del aviso "Sin señal GPS" se gestiona de manera reactiva mediante un timer o
      timeout en el flujo de ubicación válida (`timeout(15.seconds)` o un loop temporizado en
      corrutina).
    - El indicador de barras de señal GPS evalúa la calidad independientemente (fuerza satelital o
      métricas provistas por el proveedor), desacoplado del umbral estricto de 30 metros de
      precisión.
- **Implications**: Se crea una política de filtrado explícita en `domain/` (`GpsPointFilterPolicy`)
  aislada de librerías de Android, facilitando pruebas unitarias exhaustivas con JUnit 4.

### 3. Delimitación de interfaces con `start-run` y `save-run`

- **Context**: Requisitos 1.3, 9.3, 9.4, 10.1–10.5: inicio desde el FAB "+", confirmación de
  finalización y paso al guardado.
- **Sources Consulted**: `requirements.md`, referencias de sistema de diseño
  `trazo-design-system.md`.
- **Findings**:
    - `start-run` es responsable de abrir el modal con las opciones de meta (3, 5, 10 km) y entregar
      una nueva sesión a `RunSessionManager`.
    - `save-run` es responsable de la pantalla "Guardar recorrido", el ingreso del título
      obligatorio (entre 3 y 50 caracteres), selección de destacados (`HighlightChip`), captura de
      hasta 2 fotos (`PhotoPicker`) y la escritura en SQLite/Room (`RunDao`).
    - `live-route-tracking` es responsable de congelar las métricas finales y la lista de puntos de
      la traza, emitiendo un modelo inmutable `FrozenRunSummary` hacia el contrato de guardado.
    - Sobre la navegación post-finalización: dado que está pendiente definir si se va directo a
      `save-run` o se muestra antes la pantalla de resultados («¡Meta cumplida!» / «Recorrido
      finalizado»), el diseño debe desacoplar la confirmación de la ruta concreta, ofreciendo un
      punto de enganche de navegación parametrizable.
- **Implications**: Cero duplicación de entidades de persistencia de Room o lógica de cámara en
  `live-route-tracking`.

---

## Architecture Pattern Evaluation

| Patrón / Opción                           | Descripción                                                                                                                                                                | Fortalezas                                                                                                                                              | Riesgos / Limitaciones                                                                                                                                                                        | Decisión                                                          |
|-------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|
| **Hexagonal (Ports & Adapters) con Hilt** | Dominio puro con casos de uso y modelos de negocio. Puertos para obtención de ubicación (`LocationClient`) y persistencia en memoria. UI desacoplada mediante `StateFlow`. | Totalmente desacoplado del SDK de Android en el dominio; alta testeabilidad unitaria con JUnit 4; respeta las directrices de `AGENTS.md` y `README.md`. | Requiere crear interfaces y adaptadores explícitos.                                                                                                                                           | **Seleccionado**: patrón estándar del proyecto.                   |
| **Tracking dentro de ViewModel**          | Mantener toda la lógica del temporizador y GPS dentro de `MapViewModel`.                                                                                                   | Menos clases iniciales.                                                                                                                                 | El cambio de pantalla o recreación del ViewModel interrumpe o complica la persistencia del estado en pestañas; viola responsabilidad única.                                                   | **Descartado**.                                                   |
| **Android Foreground Service pesado**     | Servicio foreground de Android persistente con notificación continua.                                                                                                      | Soporta tracking con pantalla apagada en segundo plano por horas.                                                                                       | Trazo v0.6 establece explícitamente en el alcance que la pantalla se mantiene activa (`FLAG_KEEP_SCREEN_ON`) y que no se garantiza tracking prolongado en background; sobrecarga de permisos. | **Descartado**: se mantiene pantalla activa según especificación. |

---

## Design Decisions

### Decision: Ubicación del temporizador y cómputo de métricas en `domain`

- **Context**: El temporizador y el cálculo de distancia/velocidad deben congelarse en pausa y
  continuar en reanudación, incluso si el usuario navega fuera de la pestaña "Mapa".
- **Alternatives Considered**:
    1. Conteo basado en ticks de UI en Compose (`LaunchedEffect`).
    2. Ticker desacoplado en `RunSessionManager` calculando tiempo activo acumulado (elapsed real
       time) y sumando distancias de segmentos válidos.
- **Selected Approach**: Opción 2. El dominio acumula la duración activa en milisegundos mediante
  marcas de tiempo monótonas (`SystemClock.elapsedRealtime()` o abstracción de reloj de dominio)
  cuando el estado es `ACTIVE`. Al pasar a `PAUSED`, se consolida el bloque de tiempo transcurrido y
  se detiene el avance.
- **Rationale**: Garantiza precisión matemática inmune a retardos en la recomposición de Compose o
  cambios de pestaña.
- **Trade-offs**: Requiere inyectar una abstracción de reloj (`TimeProvider`) para pruebas
  deterministas.

### Decision: Desacoplamiento de navegación post-finalización

- **Context**: Requisito 9.4 y decisión pendiente con el equipo sobre si se transiciona directo a
  `save-run` o si se muestra la pantalla de resultados intermedia («¡Meta cumplida!» / «Recorrido
  finalizado»).
- **Alternatives Considered**:
    1. Forzar una ruta fija hacia `save-run`.
    2. Forzar una ruta fija hacia `result`.
    3. Emitir un evento de navegación desacoplado en `MapViewModel`
       (`onRunFinalized(summary: FrozenRunSummary)`) configurable en `TrazoApp.kt`.
- **Selected Approach**: Opción 3. `MapViewModel` expone una acción o efecto único
  `MapUiEffect.NavigateToFinalizationFlow(summary)` que el host de navegación maneja, permitiendo
  alterar el destino sin modificar el ViewModel ni los casos de uso.
- **Rationale**: Cumple rigurosamente con no asumir la decisión pendiente y mantiene el código listo
  para cualquiera de las dos resoluciones del equipo.

---

## Risks & Mitigations

- **Riesgo**: Pérdida de puntos por retardo en la recolección del `Flow` de ubicación al cambiar de
  pestaña.
    - *Mitigación*: `RunSessionManager` ejecuta la recolección en un `CoroutineScope` de aplicación
      (`@ApplicationScope`), asegurando que la recepción y filtrado continúen activos si el
      recorrido está en estado `ACTIVE`.
- **Riesgo**: Ruido GPS genere oscilaciones artificiales de velocidad o distancia.
    - *Mitigación*: Descarte estricto de puntos con precisión > 30 m; cálculo de distancia acumulada
      mediante fórmula de Haversine / `distanceTo()` únicamente entre puntos consecutivos válidos;
      velocidad instantánea provista por el sensor o calculada sobre ventana temporal segura.
- **Riesgo**: Finalización accidental durante la carrera con guantes o dedos húmedos.
    - *Mitigación*: Diálogo de confirmación modal (`AlertDialog` / `FinishConfirmDialog`)
      obligatorio antes de proceder a la finalización definitiva.

---

## References

- [Google Android Location API Documentation](https://developer.android.com/develop/sensors-and-location/location)
- [Jetpack Compose Navigation & State Restoration](https://developer.android.com/guide/navigation/navigation-compose)
- [Trazo Design System Reference](file:///C:/Users/delri/AndroidStudioProjects/A3-2026-H2-E2/.claude/skills/trazo-design-system/references/trazo-design-system.md)
- [HU-04 Requirements Document](file:///C:/Users/delri/AndroidStudioProjects/A3-2026-H2-E2/.kiro/specs/live-route-tracking/requirements.md)