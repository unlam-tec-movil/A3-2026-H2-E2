# Notas de equipo · Trazo

_Actualizado: 2026-11-06

## Leé primero

Esta carpeta compara las specs de `.kiro/specs/` y te dice **qué tenés que hacer vos**.

1. Abrí **tu archivo** (tabla de abajo).
2. Leé «En 30 segundos» y «Tus próximos pasos». Con eso alcanza.
3. Lo que se decidió en la reunión está abajo. **Ya no se discute**: hay que pasarlo a las specs.

| Persona   | Tu archivo                                                                                     | Spec                                  | Historias                          |
|-----------|------------------------------------------------------------------------------------------------|---------------------------------------|------------------------------------|
| Persona 1 | [persona-1-home-recent-runs.md](persona-1-home-recent-runs.md)                                 | `home-recent-runs`, `save-run`        | HU-01, HU-07                       |
| Persona 2 | [persona-2-start-run.md](persona-2-start-run.md)                                               | `start-run`                           | HU-02, HU-03                       |
| Persona 3 | [persona-3-live-route-tracking.md](persona-3-live-route-tracking.md)                           | `live-route-tracking`                 | HU-04                              |
| Persona 4 | [persona-4-stop-detection-finish-run-result.md](persona-4-stop-detection-finish-run-result.md) | `stop-detection`, `finish-run-result` | HU-05.1, HU-06                     |
| Todos     | [todos-config-ktlint.md](todos-config-ktlint.md)                                               | —                                     | Configurar Android Studio y ktlint |

## Lo que se decidió ✅

1. **Al tocar «Finalizar» siempre se pide confirmación**, con dos mensajes:
    - No llegó a la meta: «Todavía no llegaste a tu meta. ¿Estás seguro de que deseás finalizar?»
    - Llegó a la meta y siguió corriendo: «¿Estás seguro de que deseás finalizar?»
    - Llegar a la meta **no** termina el recorrido: se puede seguir corriendo.
    - **Menos de 200 m:** en lugar de la confirmación aparece el aviso «demasiado corto» (HU-06 R4).
      Se puede descartar o volver; **no se guarda**.
2. **Después de confirmar:** primero la **pantalla de resultado** (HU-06) y después **guardar**.
3. **El guardado tiene fotos** y los recorridos guardados **se ven en el Mapa**.
4. **Estados del recorrido:** se usan los de HU-04 (`IDLE`, `ACTIVE`, `PAUSED`, `FINISHED`,
   `RunSession` nunca nulo) **y se agregan otros** para separar frenada, pausa manual y pausa
   personal.
5. **Fecha y hora de inicio:** las guarda **HU-04** cuando arranca el recorrido.
6. **Señal GPS:** el recorrido **no arranca hasta que haya señal** («Esperando señal GPS»).
7. **Permisos de ubicación:** los pide el botón «+» (`StartRunFab`), en Inicio y en Mapa, **una sola
   vez** (si ya los diste, no se vuelven a pedir).
8. **Botón «+» deshabilitado:** HU-04 expone un booleano «hay recorrido en curso».
    - `true` → deshabilitado. `false` → habilitado.
    - Solo vuelve a `false` al **finalizar**. Pausas, frenadas o pérdida de señal **no** cuentan.
    - Persona 1 lo aplica en Inicio y Persona 2 en Mapa.
9. **`save-run` es de Persona 1.** Se adapta a los contratos de las demás specs cuando estén
   terminadas.
10. **Componentes compartidos** van en `ui/components/common/`.
11. **HU-04: se aprueban R9 (finalizar) y R10 (guardado).**

## Lo que sigue abierto 🟡

| Tema                                                                       | Quiénes           |
|----------------------------------------------------------------------------|-------------------|
| Nombres de los estados nuevos                                              | Personas 3 y 4    |
| Cómo pasa el resumen a la pantalla de resultado (la traza es grande)       | Personas 3 y 4    |
| Datos que le faltan al resumen: meta cumplida, demo, frenadas, regularidad | Personas 3, 4 y 1 |
| Dueños de: regularidad del piso (HU-05.2), modo demo y pin de frenada      | Equipo            |

## Quién espera a quién

```
HU-01 Inicio ✅ (Persona 1)
   │
   ▼
HU-04 contrato del recorrido (Persona 3)   ← todos lo necesitan
   │
   ├──► HU-02/03 iniciar recorrido (Persona 2)
   └──► HU-05.1 frenadas (Persona 4)
           │
           ▼
        HU-06 resultado (Persona 4)
           │
           ▼
        HU-07 guardar recorrido (Persona 1)
```

> **Contrato** = la forma de los datos que comparten las specs (clases, estados y funciones).

## Estado de cada spec

| Spec                  | Estado                                    | Qué falta                                              |
|-----------------------|-------------------------------------------|--------------------------------------------------------|
| `home-recent-runs`    | ✅ Implementada                           | Probar en un dispositivo; aplicar el «+» deshabilitado |
| `start-run`           | 🟡 Requisitos sin aprobar                 | Pasar las decisiones y aprobar                         |
| `live-route-tracking` | 🟡 R9 y R10 aprobados; diseño sin aprobar | Ajustar el diseño a las decisiones                     |
| `stop-detection`      | 🟡 Diseño hecho, sin aprobar              | Usar `ACTIVE` y los estados nuevos                     |
| `finish-run-result`   | 🟡 Requisitos sin aprobar                 | Reescribir el cierre según la decisión 1               |
| `save-run`            | 🟡 Requisitos sin aprobar (Persona 1)     | Esperar los contratos; sumar fotos y Mapa              |
| Steering              | 🟡 Generado                               | Revisarlo entre todos                                  |

## Archivos que tocan varias personas

Hacé PRs chicos y avisá antes de tocar estos:

- `ui/navigation/TrazoApp.kt`: rutas y navegación.
- `res/values/strings.xml`: usá el prefijo de tu pantalla (`home_`, `map_`, `result_`…).
- `ui/screens/map/MapScreen.kt`: es de Persona 3. Los demás suman sus piezas por parámetro.
- `ui/components/common/`: `StartRunFab`, el modal y `GoalChip` van acá.
- `gradle/libs.versions.toml`: avisá antes de agregar una librería.

<details>
<summary><b>Detalle: cómo quedó cada contradicción</b></summary>

### Contradicciones entre specs

| #   | Tema                              | Cómo quedó                                                                                                         |
|-----|-----------------------------------|--------------------------------------------------------------------------------------------------------------------|
| C1  | Meta alcanzada                    | ✅ Animación y se sigue corriendo. HU-06 R1 deja de detener el registro                                            |
| C2  | Confirmar «Finalizar»             | ✅ Siempre, con dos mensajes. HU-06 R2 (sin confirmación con meta) cambia. Se mantiene el aviso < 200 m (HU-06 R4) |
| C3  | Después de finalizar              | ✅ Pantalla de resultado y después guardar (HU-04 R9.4)                                                            |
| C4  | Estados                           | ✅ Se mantienen los de HU-04 y se agregan: frenada detectada, pausa manual y pausa personal. 🟡 Nombres            |
| C5  | Guardado dentro de HU-04          | ✅ HU-04 R10 aprobado; lo implementa `save-run`                                                                    |
| C6  | Fotos                             | ✅ Sí hay fotos. `save-run` las suma cuando estén los contratos                                                    |
| C7  | Traza guardada en Mapa            | ✅ Sí se ve. `save-run` lo suma cuando estén los contratos                                                         |
| C8  | Segundo plano                     | ✅ Sin servicio, pantalla encendida                                                                                |
| C9  | Nombres y forma del estado        | ✅ `ACTIVE` y `RunSession` sellada (no nula)                                                                       |
| C10 | Fecha y hora de inicio            | ✅ La guarda HU-04 al arrancar                                                                                     |
| C11 | Arranque con GPS malo             | ✅ No arranca sin señal. Se usa «Esperando señal GPS» de `start-run`                                               |
| C12 | Dónde viven el modal y el FAB     | ✅ `ui/components/common/`                                                                                         |
| C13 | «Comenzar» sin meta, «Seleccioná» | 🟡 Interno de Persona 2                                                                                            |

### Cosas sin dueño

| #  | Tema                                                 | Cómo quedó                                                   |
|----|------------------------------------------------------|--------------------------------------------------------------|
| H1 | «+» deshabilitado                                    | ✅ Booleano de HU-04; Persona 1 en Inicio, Persona 2 en Mapa |
| H2 | Regularidad del piso (HU-05.2)                       | 🟡 Sin dueño                                                 |
| H3 | Modo demo                                            | 🟡 Sin dueño                                                 |
| H4 | Pin de frenada en el mapa                            | 🟡 Sin dueño                                                 |
| H5 | Prioridad entre alerta de frenada y avisos de cierre | 🟡 Interno de Persona 4                                      |
| H6 | `save-run` (HU-07)                                   | ✅ Persona 1                                                 |

### Lo que ya coincidía

- Precisión GPS de 30 m (start-run y HU-04).
- Metas fijas de 3, 5 y 10 km.
- Texto «Todavía no hay recorridos» (HU-01 y save-run).
- «Quitar animaciones»: una sola utilidad (`rememberReduceMotion()`).

</details>
