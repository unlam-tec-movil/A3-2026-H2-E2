# Persona 4 · Frenadas y resultado (`stop-detection` HU-05.1 y `finish-run-result` HU-06)

## En 30 segundos

- La reunión **resolvió cómo se termina un recorrido**. Tu HU-06 tiene que cambiar.
- **Frenadas:** usá los nombres de Persona 3 (`ACTIVE`, `RunSession` nunca nulo).
- El aviso de **menos de 200 m se mantiene** (tu R4 queda como está).

## Lo que se decidió para vos

- **Llegar a la meta no detiene el recorrido.** Animación y se puede seguir corriendo.
- **«Finalizar» siempre pide confirmación**, con dos mensajes:
  - Sin meta cumplida: «Todavía no llegaste a tu meta. ¿Estás seguro de que deseás finalizar?»
  - Con meta cumplida: «¿Estás seguro de que deseás finalizar?»
  - **Menos de 200 m:** aviso «demasiado corto» en lugar de la confirmación (tu R4 se mantiene).
- **Después de confirmar:** tu pantalla de resultado y, con «Continuar», a guardar.
- **Estados:** Persona 3 suma estados para separar frenada, pausa manual y pausa personal.
- **Fecha y hora de inicio:** las guarda Persona 3 (HU-04), no `start-run`.
- **Guardado y fotos:** sí hay fotos. `save-run` es de Persona 1.

## Tus próximos pasos

**Resultado (`finish-run-result`)**

1. [ ] Reescribir los requisitos del cierre:
   - **R1:** llegar a la meta **no** detiene el registro (sacar R1.1). El aviso con «Finalizar» se
     puede mantener.
   - **R2:** con meta cumplida **sí** se confirma: «¿Estás seguro de que deseás finalizar?».
   - **R3:** cambiar el texto a «Todavía no llegaste a tu meta. ¿Estás seguro de que deseás
     finalizar?».
   - **R4 (< 200 m):** se mantiene igual.
   - Corregir la frase que dice que la fecha de inicio la define `start-run`: la guarda HU-04.
2. [ ] Aprobar los requisitos y seguir con `/kiro-spec-design finish-run-result`,
   `/kiro-spec-tasks` y `/kiro-impl`.

**Frenadas (`stop-detection`)**

3. [ ] Corregir el diseño:
   - Sacar el supuesto de «servicio en segundo plano». No hay.
   - Usar `ACTIVE` y `RunSession` sellada, y los estados nuevos de Persona 3.
   - Agregar al overlay un parámetro para **ocultar** la alerta durante los avisos de cierre.
4. [ ] `/kiro-validate-design stop-detection`, aprobar y `/kiro-spec-tasks stop-detection`.
5. [ ] `/kiro-impl stop-detection`. Empezá por el dominio y la UI: no dependen de Persona 3.
6. [ ] Calibrar el umbral en un celular: caminando, trotando y parado.

## Tenés que hablar con…

| Persona | Tema | Qué hay que decidir |
|---|---|---|
| Persona 3 | Estados nuevos | Nombres de frenada detectada y pausa personal |
| Persona 3 | Menos de 200 m | Se mantiene. ¿Quién lo muestra: el diálogo de Persona 3 o un componente tuyo? |
| Persona 3 | Diálogo de «Finalizar» | Lo tiene Persona 3 (`FinishConfirmDialog`). ¿Quién pone los textos? |
| Persona 3 | Pasar el resumen | ¿Por navegación o en un lugar compartido? La traza es grande |
| Persona 3 y 1 | Datos del resumen | Demo, frenadas y regularidad: ¿quién los aporta? |
| Persona 1 | Regularidad «sin dato» | Sin acelerómetro mandás «sin dato», nunca 0 % |

## Cuidado con estos archivos

- `MapScreen.kt` es de Persona 3: montás tus avisos por parámetro.
- `ui/theme/ReduceMotion.kt` lo creás vos. Lo usan tus dos specs y la celebración de Persona 3.
- `TrazoApp.kt`: ruta `result` (la de guardar es de Persona 1).
- `libs.versions.toml`: `kotlinx-coroutines-test` (Persona 3 también lo necesita).

<details>
<summary><b>Detalle (si lo necesitás)</b></summary>

### Cómo quedaron las contradicciones con Persona 3

| # | Tema | Cómo quedó |
|---|---|---|
| C1 | Meta alcanzada | ✅ Se sigue corriendo. Cambia tu R1.1 |
| C2 | Confirmar «Finalizar» | ✅ Siempre, dos mensajes. Cambian tus R2 y R3. R4 (< 200 m) se mantiene |
| C3 | Después de finalizar | ✅ Tu pantalla de resultado y después guardar |
| C4 | Estados | ✅ Persona 3 suma estados. 🟡 Nombres |
| C8 | Segundo plano | ✅ Sin servicio, pantalla encendida. Corregir los Riesgos de tu diseño |
| C9 | Nombres y forma | ✅ `ACTIVE` y `RunSession` sellada. Tu adaptador usaba `RUNNING` y nulo (rama vieja) |
| C10 | Fecha de inicio | ✅ La guarda HU-04 |
| H4 | Pin de frenada | 🟡 Sin dueño |

Ejemplo del adaptador con el contrato nuevo:

```kotlin
activeSession.map { s -> (s as? ActiveSession)?.takeIf { it.state == RunState.ACTIVE }?.id }
```

### Otros temas

| # | Tema | Cómo quedó |
|---|---|---|
| C6 | Fotos | ✅ Sí hay fotos (las pide `save-run`) |
| H2 | Regularidad (HU-05.2) | 🟡 Sin dueño |
| H3 | Modo demo | 🟡 Sin dueño |
| H5 | Prioridad de avisos | Tu HU-06 R7 oculta la alerta de frenada durante el cierre; el overlay no tiene esa entrada |

### Para el diseño de `finish-run-result`

- Reutilizar `rememberReduceMotion()`.
- Título del resultado según la meta: «¡Meta cumplida!» o «Recorrido finalizado».
- Datos para `save-run`: distancia, tiempo, velocidad promedio, frenadas, regularidad (`null` =
  «sin dato»), meta, meta cumplida, fecha y hora de inicio, demo y traza.
- Definir «Atrás» y la barra de abajo en la pantalla de resultado.
- Confirmar que «Finalizar» desde pausa funciona igual.

</details>
