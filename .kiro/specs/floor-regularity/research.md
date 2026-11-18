# Research & Design Decisions · floor-regularity

## Summary

- **Alcance de la investigación**: extensión de un sistema existente (descubrimiento liviano). No hay
  dependencias externas nuevas; todo sale del código de la rama `feat/hu-05-frenadas`.
- **Hallazgos clave**:
  - `StopDetectionRepositoryImpl` ya lee el sensor solo mientras el id del recorrido en curso no es
    `null`, y la pausa manual lleva ese id a `null`: en `PAUSED` la detección deja de leer, no
    registra frenadas y conserva la lista (verificado en el código y en el diseño de `stop-detection`).
  - `AccelerometerMotionSource` es un flujo **frío**: cada colector registra un listener propio. Si
    el repositorio de regularidad colectara el mismo flujo, habría dos listeners. Hace falta
    compartir la lectura.
  - `RunningRunSource` de `stop-detection` publica solo el id; la regularidad necesita además el
    `RunState` (para contar solo `ACTIVE`), así que no se puede reutilizar tal cual y no se modifica.
  - Todavía no existe ningún módulo Hilt de `stop-detection` (tarea 4.3 pendiente): `MotionSource` y
    `MonotonicClock` aún no tienen binding.

## Research Log

### Compartir el acelerómetro entre dos consumidores
- **Contexto**: requisito 6.1 (una sola lectura) y 6.7 (dar de baja cuando nadie lo necesita).
- **Fuentes**: `AccelerometerMotionSource.kt`, `StopDetectionRepositoryImpl.kt`,
  `MotionSource.kt`.
- **Hallazgo**: el operador `shareIn` con `SharingStarted.WhileSubscribed()` de kotlinx.coroutines
  mantiene una sola colección del flujo origen mientras haya al menos un suscriptor y la cancela
  cuando el último se va. Eso da conteo de referencias sin código propio.
- **Implicancia**: un decorador de `MotionSource` (`SharedMotionSource`) alcanza. No cambia el puerto
  ni el dominio de `stop-detection`: solo cambia qué implementación se inyecta.

### Qué tramos cuentan y cuándo hay lectura
- Estado de los consumidores por `RunState`:

  | `RunState`         | `stop-detection` lee | Regularidad lee | Sensor |
  |--------------------|:--------------------:|:---------------:|:------:|
  | `ACTIVE`           | sí                   | sí              | activo |
  | `STOPPED`          | sí                   | no              | activo |
  | `PERSONAL_PAUSED`  | sí                   | no              | activo |
  | `PAUSED`           | no                   | no              | apagado |
  | `Idle` (fin/descarte) | no                | no              | apagado |

- `STOPPED` y `PERSONAL_PAUSED` mantienen el sensor activo por `stop-detection` (necesita saber
  cuándo el corredor retoma), pero la regularidad no incorpora esas muestras.

### Zona gris de quietud previa a la frenada
- Los primeros 8 s de una detención son `ACTIVE` (fase `Still`), así que esas muestras entrarían en
  un tramo. Un tramo con quietud tiene desvío bajo y podría inflar el porcentaje. Se acepta como
  límite conocido del enfoque (los tramos que cruzan el paso a `STOPPED` se descartan por requisito
  3.6) y se mide en calibración (tarea 5.1).

## Architecture Pattern Evaluation

| Opción | Descripción | Fortalezas | Riesgos | Decisión |
|--------|-------------|------------|---------|----------|
| A. Segundo `collect` del flujo frío | La regularidad llama a `MotionSource.samples()` por su cuenta | Sin código extra | Dos listeners; la consigna lo prohíbe | Descartada |
| B. Decorador compartido de `MotionSource` | `shareIn` + `WhileSubscribed`, binding único | Un listener, baja automática, dominio de `stop-detection` intacto | Hay que coordinar el binding Hilt con `stop-detection` | **Elegida** |
| C. Que `stop-detection` reenvíe las muestras | Un solo consumidor reparte | Un único colector | Acopla la regularidad a `stop-detection` y obliga a cambiar su dominio | Descartada |

## Design Decisions

### Decisión: gestor del ciclo de vida del sensor
- **Contexto**: la consigna pide definir quién gestiona el sensor compartido.
- **Elegido**: `SharedMotionSource` es el único gestor. Cada consumidor decide **cuándo colectar**
  según el estado del recorrido; el sensor está activo si y solo si hay al menos un consumidor
  colectando.
- **Trade-off**: el apagado depende de que ambos consumidores dejen de colectar. Se cubre con una
  prueba de integración que recorre finalizar, descartar y pausar.

### Decisión: tiempo de los tramos
- **Elegido**: el medidor usa el reloj monotónico inyectado en el momento en que llega cada muestra
  (igual que `stop-detection`), no el timestamp del sensor. Así los tramos y los cambios de estado
  usan la misma base de tiempo y se prueban con tiempo virtual.

### Decisión: valor disponible después de finalizar
- **Elegido**: el estado de regularidad se mantiene al terminar o descartar y se reinicia solo
  cuando empieza un recorrido con otro id (mismo criterio que las frenadas de `stop-detection`).
  `finishSession()` puede leerlo sin esperar ningún cálculo adicional porque el porcentaje se
  actualiza cada vez que se cierra un tramo.

### Decisión: umbral de vibración baja
- **Elegido**: desvío estándar de la magnitud por tramo contra un umbral configurable. El valor
  inicial es **provisional y sin respaldo** (2,0 m/s²); se calibra en un teléfono real (tarea 5.1).
  No se presenta como acordado.

## Risks & Mitigations

- **Doble binding Hilt** de `MotionSource` y `MonotonicClock` con el módulo de `stop-detection`
  (tarea 4.3 pendiente): acordar quién provee qué antes de integrar (tarea 4.3 de esta spec).
- **Quietud previa a la frenada infla el porcentaje**: medir en calibración; si resulta relevante,
  excluir tramos que crucen una fase de quietud (requeriría exponer esa fase; se discute con
  `stop-detection`, no se resuelve acá).
- **Umbral sin calibrar**: queda configurable y marcado como provisional.
