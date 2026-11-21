# Análisis de brecha: profile-runs

_Fecha: 2026-11-21 · Requisitos: `requirements.md` (generados, sin aprobar)_

## 1. Estado actual del código

**Ya existe**
- `domain/model/saved/SavedRun.kt`: título, fecha de inicio, meta, meta cumplida, demo, distancia, tiempo, velocidad promedio, traza, frenadas, regularidad (`Int?`), destacados y rutas de fotos. Tiene `brakeCount`.
- `data/repository/SavedRunRepository` con `runs: Flow<List<SavedRun>>` (más reciente primero) y `save()`. La implementación es `InMemorySavedRunRepository` (`@Singleton` en `SavedRunModule`); se pierde al cerrar la app.
- Pestaña Perfil (`ui/screens/profile/`): `ProfileScreen`/`ProfileContent`, `ProfileViewModel` (hoy con estado fijo en cero y sin dependencias), `ProfileStatRow`, `EmptyState`, `ProfileStatsFormatter`. «Mis estadísticas» y el estado vacío de «Mis recorridos» ya se ven. `ProfileUiState.hasRecentRuns` no tiene lista de recorridos.
- Recursos: `img_empty_runs`, strings `profile_*`, `Sizes.traceSavedWidth` y `traceSaved` (Primary500 al 75 %) en el tema, ya previstos para las trazas guardadas.
- Mapa: `MapLayer` (MapCompose) dibuja un solo trazo en vivo (`route` + `route-outline`) y el pin del corredor. `MapScreen` usa `LiveRouteTrackingViewModel`, que ya expone `hasActiveRun` e `isRunInProgress`.
- Ubicación: `LocationRepository.readLocationStatus()` devuelve `LocationStatus` (`READY` = permiso preciso + ubicación activa, `APPROXIMATE_ONLY`, etc.).
- Convención de tests: JVM con fakes manuales (`app/src/test`) y Compose UI Test sobre los `Content` (`androidTest`).

**No existe**
- Ningún escritor de `SavedRun`: la pantalla `saverun` está en la estructura pero `save-run` solo tiene requisitos. Hoy no hay recorridos reales para mostrar.
- Modal de detalle, card de recorrido, `StatusBadge` (el steering lo ubica en `components/common/`), componente de fotos.
- Trazas guardadas en el mapa y selección por toque.

## 2. Requisito → recurso, con brechas

| Req. | Qué hay | Brecha |
|------|---------|--------|
| 1 Lista | `SavedRunRepository.runs` ya ordenado; `SavedRun` trae todos los campos de la card | **Missing**: `ProfileViewModel` no lee el repositorio; falta `RunCard`, formateo de fecha/duración/regularidad y `StatusBadge`. **Constraint**: 1.8 (persistir al cerrar la app) no se cumple con la implementación en memoria. |
| 2 Vacío | Ilustración, texto y totales en cero ya implementados | Solo regresión: el estado vacío debe depender de la lista real, no de `hasRecentRuns` fijo. |
| 3 Detalle | `SavedRun` con traza, velocidad promedio, meta, destacados, fotos | **Missing**: modal, mapa con la traza completa (MapLayer está acoplado a la vista en vivo), carga de fotos (Coil 2.7 está en el catálogo), reglas de ocultar secciones. **Unknown**: forma del mapa del detalle (¿encuadre automático a la traza?). |
| 4 Estadísticas | Formato (`ProfileStatsFormatter`) y filas ya listos | **Missing**: calcular totales desde `runs`, excluyendo `isDemo`. Los incompletos suman sin cambios. |
| 5 Trazas en Actividad | Trazo en vivo, `isRunInProgress`, `LocationStatus.READY`, `traceSaved` | **Missing**: dibujar N trazas, detectar el toque sobre una traza (MapCompose no tiene click en paths; hay que resolver el toque por distancia a los segmentos o con otra técnica), estado «ubicación precisa» disponible para el mapa. **Unknown**: ver 3 abajo. |
| 6 Accesibilidad | Patrones del steering (48 dp, `ReduceMotion`, `semantics`) | Aplicar en componentes nuevos; el mapa necesita descripción accesible. |

## 3. Hallazgos que afectan los requisitos

1. **Persistencia (R1.8)**. El repositorio es en memoria y Room está «previsto» pero no está en el catálogo (`tech.md`: sumar dependencia exige avisar al equipo). Hay que decidir: pasar a Room en esta spec, dejarlo a `save-run`, o relajar 1.8. Hoy lo contradice `InMemorySavedRunRepository`.
2. **Tiempo sin pausas personales (R4.4)**. `elapsedTimeSeconds` sale de `elapsedAt`, que suma solo tramos abiertos: las pausas manuales y personales no cuentan, y las frenadas sí (stop-detection D3). Por eso el tiempo total ya excluye pausas personales sin cálculo extra, siempre que `save-run` guarde ese valor. Mi suposición de que el recorrido trae «tiempo de pausas personales» en el Boundary Context sobra: conviene quitarla de los requisitos.
3. **Precisión de ubicación (R5.1/5.4)**. Solo existe una lectura puntual (`readLocationStatus()`), sin flujo que avise cuando cambia el permiso. Para el mapa hay que decidir cuándo se evalúa (al entrar a la pestaña, al volver a primer plano, continuo). **Research Needed.**
4. **Datos para el detalle**. `SavedRun.regularityPercent` es `Int?` (sin acelerómetro es nulo) y la card/detalle necesitan una representación para «sin dato». Los requisitos no la definen.
5. **Fotos**. `photoPaths` es `List<String>` sin tope; el «máx. 2» no está garantizado por el modelo. Quién lo impone (guardado o detalle) queda por definir con `save-run`.
6. **Fechas**. La card muestra fecha; el formato (`Mar 28 oct`) viene de `save-run` y no hay formateador en el código.
7. **Regla de capas**. `ui/components` no conoce modelos de `domain`: la card y el detalle necesitan modelos de UI (`RunCardUi`, `RunDetailUi`), y el modal compartido entre Perfil y Actividad debe vivir en `ui/components/` (lo usan dos pantallas), no en `profile/components/`.

## 4. Opciones de implementación

**A. Extender lo existente**: `ProfileViewModel` lee el repositorio; `MapLayer` suma un parámetro para trazas guardadas.
- ✅ Pocos archivos nuevos; sigue los patrones.
- ❌ `MapLayer` ya mezcla trazo en vivo, pin y botones; agregar trazas y toques la hincha. Reusarlo para el mapa del detalle exige desacoplar botones y pin.

**B. Componentes nuevos**: `SavedRunsMapLayer` y `RunDetailMap` aparte; modal propio.
- ✅ Separación clara, fácil de probar.
- ❌ Duplica la creación de `MapState`, tiles y atribución.

**C. Híbrida (recomendada)**
- Extraer de `MapLayer` el armado del mapa (estado, tiles, atribución) a una pieza reutilizable; `MapLayer` y el mapa del detalle la usan.
- `ProfileViewModel` y una lógica pura de totales (excluir demo) en `domain` testeable sin Android.
- `RunDetailSheet` en `ui/components/layout/` (lo abren Perfil y Actividad), con `RunDetailUi`.
- Trazas guardadas en Actividad como parámetro nuevo opcional de `MapLayer`, evaluado por el ViewModel del mapa (`hasActiveRun`, estado de ubicación, lista).
- Persistencia: tratarla como spec o tarea separada (ver hallazgo 1).

## 5. Esfuerzo y riesgo

- **Esfuerzo: M–L (5–8 días)**. Perfil y detalle son rutinarios (M); la selección de trazas en el mapa y el mapa del detalle suman la parte difícil.
- **Riesgo: Medio**. Alto en el toque sobre trazas (MapCompose no lo ofrece directo, y las trazas superpuestas no tienen regla definida); bajo en el resto, que sigue patrones existentes.

## 6. Recomendaciones para el diseño

1. Definir con el equipo la **persistencia** antes del diseño (R1.8) y **quién escribe `SavedRun`** (dependencia de `save-run`): sin eso no hay datos reales para probar.
2. Tomar la **opción C**.
3. Corregir el Boundary Context: no pedir «tiempo de pausas personales» al recorrido guardado (hallazgo 2).
4. Decidir la regla para **trazas superpuestas** y el **encuadre del mapa del detalle**.

## Research Needed
- MapCompose 3.0.0: ¿hay clic en paths o hay que resolver el toque por geometría? ¿Cómo encuadrar una traza (`scrollTo` / `centerOnMarker`)?
- Observación de cambios de permiso de ubicación para el mapa (hallazgo 3).
- Modal (`ModalBottomSheet` o diálogo a pantalla completa) con un mapa dentro: gestos de arrastre vs. gestos del mapa.
- Room: versión compatible con AGP 9.4 y Kotlin 2.3 si se decide persistir aquí.

---

# Diseño: registro de descubrimiento (2026-11-21)

**Tipo de descubrimiento**: ligero (extensión de código existente). Sin búsqueda externa; se usó el análisis de brecha de arriba y la lectura de `MapLayer`, `TrazoApp`, `LiveRouteTrackingViewModel` y `SavedRun`.

## Decisiones de síntesis
- **Un solo dueño del modal**: `TrazoApp` crea `RunDetailViewModel` y reparte `onRunClick(id)`. Evita dos modales y que una pantalla importe de otra.
- **Generalización**: el armado de `MapState` se extrae a `rememberTrazoMapState()` para el mapa de Actividad y el del detalle.
- **Simplificación**: sin dependencias nuevas, sin cambios en `SavedRun` ni en `SavedRunRepository`; totales y búsqueda de traza como funciones puras junto al modelo.
- **Boundary**: la spec solo lee; la persistencia (1.8) queda como dependencia de `save-run`.

## Decisiones tomadas en diseño que los requisitos no fijaban
1. «Ubicación precisa» = `LocationStatus.READY`, releído al reanudar la pestaña y al terminar un recorrido.
2. Regularidad nula se muestra «–» con descripción accesible «sin datos».
3. Fecha de la card con `SimpleDateFormat` (minSdk 24, sin desugaring): `Mar 28 oct`.
4. Detalle como diálogo a pantalla completa, no hoja inferior, por el choque de gestos con el mapa.

## Research Needed (se resuelve al implementar)
- Callback de toque de MapCompose 3.0.0 y su sistema de coordenadas (se asume el plano normalizado que ya usa `WebMercator.normalize`).
- Encuadre de una traza con `scrollTo(x, y, scale)` y cálculo de la escala para un rectángulo dado.
- Si `LocationStatus.DEVICE_LOCATION_OFF` debería contar como «precisa»: el diseño asume que no.
- Que `save-run` guarde máximo 2 fotos: el detalle recorta a 2 por seguridad.

## Riesgos
- Medio: toque por geometría (tolerancia en dp y zoom) y mapa dentro del diálogo.
- Bajo: Perfil, totales y detalle siguen patrones existentes.
