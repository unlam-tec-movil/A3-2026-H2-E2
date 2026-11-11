# Persona 3 · Recorrido en vivo (`live-route-tracking`, HU-04)

## En 30 segundos

- **R9 (finalizar) y R10 (guardado) están aprobados.** El resto del diseño, todavía no.
- La reunión decidió varias cosas que **cambian tu diseño** (lista de abajo).
- Todo el equipo espera **tu contrato** (la forma de los datos del recorrido). Publicalo primero.

## Lo que se decidió para vos

- **Finalizar:** siempre con confirmación. Dos mensajes:
  - Sin meta cumplida: «Todavía no llegaste a tu meta. ¿Estás seguro de que deseás finalizar?»
  - Con meta cumplida: «¿Estás seguro de que deseás finalizar?»
  - **Menos de 200 m:** en lugar de confirmar, aviso «demasiado corto» (HU-06 R4). Se puede
    descartar o volver; no se guarda.
- **Meta alcanzada:** animación y **se sigue corriendo**. No termina sola (R8.2 queda resuelto).
- **Después de confirmar:** pantalla de resultado (Persona 4) y después guardar (R9.4 resuelto).
- **Estados:** quedan los tuyos y **sumás otros** para separar frenada, pausa manual y pausa
  personal.
- **Fecha y hora de inicio:** las guardás **vos** al arrancar.
- **GPS:** no se arranca sin señal. Persona 2 espera la señal y recién ahí te llama.
- **Permisos:** los pide el «+» de Persona 2. **Tu `MapScreen` ya no los pide.**
- **Botón «+»:** exponés un booleano «hay recorrido en curso». Solo vuelve a `false` al finalizar.
- **Nombres:** `ACTIVE` y `RunSession` sellada (no nula). Persona 4 se adapta a vos.
- **Guardado y fotos:** los hace Persona 1 en `save-run`. Vos solo entregás el resumen.

## Tus próximos pasos

1. [ ] Pasar a `requirements.md`: R9 y R10 aprobados (sacar las marcas «PENDIENTE»), los dos
   mensajes en R9.1, R9.4 = pantalla de resultado, R8.2 = se sigue corriendo.
2. [ ] Cambiar el diseño (lista de abajo).
3. [ ] **Acordar con Persona 4** los nombres de los estados nuevos y quién muestra el aviso de
   < 200 m.
4. [ ] `/kiro-validate-design live-route-tracking` y aprobar el diseño.
5. [ ] **Publicar el contrato primero** (modelos y `RunSessionRepository`), antes de la pantalla.
6. [ ] `/kiro-spec-tasks live-route-tracking` y `/kiro-impl live-route-tracking`.

## Cambios en tu diseño

1. **Estados nuevos** en `RunState`, para separar (nombres a acordar con Persona 4):
   - pausa manual (`PAUSED`, ya existe),
   - frenada detectada (HU-05.1),
   - pausa personal (cuando el corredor elige «Pausa personal» en el aviso de 20 s).
2. **Booleano «hay recorrido en curso»** (propuesta: `isRunInProgress`). `true` desde
   `attachActiveSession` hasta `finishSession`. Pausas, frenadas y sin señal siguen en `true`.
3. **Meta cumplida:** guardá si ya se llegó a la meta. Lo necesitan el diálogo y la pantalla de
   resultado.
4. **`FinishConfirmDialog`:** recibe el mensaje según la meta (los dos textos de arriba). Con
   menos de 200 m no se muestra: va el aviso «demasiado corto».
   - **Descartar** = detener, borrar y volver a Mapa sin recorrido (`resetToIdle()` sirve). El
     booleano «hay recorrido en curso» vuelve a `false`.
5. **Fecha y hora real de inicio:** guardala en `attachActiveSession` y sumala al resumen.
6. **Arranque:** el temporizador arranca en `attachActiveSession`, que ahora llega **con señal**.
   Revisá el aviso de «sin señal» en los primeros 15 s: ya no aplica al arrancar.
7. **Sacar la pedida de permisos** de `MapScreen`. Dejá un parámetro para el «+» de Persona 2.
8. **`FrozenRunSummary`:** sumá fecha real y meta cumplida. Falta acordar demo, frenadas y
   regularidad con Persona 4.
9. **R1.5:** el diseño lo marca «acordado», pero en requisitos sigue pendiente. Y no menciona
   «Pausar».
10. **Librerías:** falta elegir la del mapa y la de ubicación.
11. **Pantalla encendida solo en Mapa:** al cambiar de pestaña se puede apagar (R7.2).
12. **Celebración:** debe respetar «Quitar animaciones» (`rememberReduceMotion()` de Persona 4).

## Tenés que hablar con…

| Persona | Tema | Qué hay que decidir |
|---|---|---|
| Persona 4 | Estados nuevos | Nombres de frenada detectada y pausa personal |
| Persona 4 | Menos de 200 m | Se mantiene. ¿Quién lo muestra: tu diálogo o un componente de Persona 4? |
| Persona 4 | Pasar el resumen | ¿Por navegación o en un lugar compartido? La traza es grande |
| Persona 4 y 1 | Datos del resumen | Demo, frenadas y regularidad: ¿quién los aporta? |
| Persona 2 | Arranque | Confirmar que te llama recién con señal |
| Personas 1 y 2 | Botón «+» | Nombre del booleano |

## Cuidado con estos archivos

- `MapScreen.kt`: sos el dueño. Dejá parámetros para el «+», los permisos y los avisos de Persona 4.
- `domain/` y `data/`: tu contrato. Cualquier cambio, avisalo.
- `libs.versions.toml`: librerías del mapa y de ubicación.

<details>
<summary><b>Detalle (si lo necesitás)</b></summary>

### Tu contrato según el diseño (antes de los cambios)

```kotlin
interface RunSessionRepository {
    val activeSession: StateFlow<RunSession>   // Idle o ActiveSession
    fun attachActiveSession(session: InitializedRunSession)
    suspend fun pauseSession(): Result<Unit>
    suspend fun resumeSession(): Result<Unit>
    suspend fun finishSession(): Result<FrozenRunSummary>
    fun resetToIdle()
}
// RunState: IDLE, ACTIVE, PAUSED, FINISHED   ← sumar los estados nuevos
// FrozenRunSummary: sessionId, goal, totalTimeSeconds, totalDistanceKm, avgSpeedKmh,
//                   routePoints, finishedMonotonicTimestamp   ← sumar fecha real y meta cumplida
```

Ojo: es distinto del código de la rama `origin/HU-04-map` (`RUNNING`, `StateFlow<RunSession?>`).
Se decidió usar el tuyo.

### Cómo quedaron las contradicciones

| # | Tema | Cómo quedó |
|---|---|---|
| C1 | Meta alcanzada | ✅ Animación y se sigue corriendo |
| C2 | Confirmar «Finalizar» | ✅ Siempre, dos mensajes. Con < 200 m, aviso «demasiado corto» |
| C3 | Después de finalizar | ✅ Resultado y después guardar |
| C4 | Estados | ✅ Sumar estados. 🟡 Nombres |
| C5–C7 | Guardado, fotos, traza en Mapa | ✅ R10 aprobado; lo implementa `save-run` (Persona 1). La trazabilidad 10.5 todavía nombra `HistoryScreen` y `MapLayer` |
| C9 | Nombres | ✅ `ACTIVE` y `RunSession` sellada |
| C10 | Fecha de inicio | ✅ La guardás vos |
| C11 | GPS y permisos | ✅ Sin señal no arranca; permisos en el «+» |
| H1 | «+» deshabilitado | ✅ Booleano tuyo |
| H4 | Pin de frenada | 🟡 Sin dueño |

### Detalles de forma

- Línea 533 de `design.md`: encabezado roto (`#implement\`RunSessionRepository\``).
- Dice «8 escenarios», pero los requisitos tienen 10.
- `MetricFormatter` aparece en la trazabilidad pero no en la estructura de archivos.
- `MapUiState.kt` va en archivo aparte; `structure.md` pide ponerlo arriba del ViewModel.
- Falta el texto del aviso de permiso revocado.
- `kotlinx-coroutines-test` se usa en los tests pero no está en el catálogo (Persona 4 también lo suma).
- `research.md` tiene enlaces a `C:/Users/delri/...`.
- `spec.json`: actualizar `updated_at`.

</details>
