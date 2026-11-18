# Requirements Document

## Project Description (Input)

Historias: HU-02 (Iniciar un recorrido y elegir la meta desde Inicio) y HU-03 (desde el Mapa)

Como corredor que ingresa a Trazo quiero iniciar un recorrido y seleccionar una meta de distancia de
3 km, 5 km o 10 km desde Inicio o desde el Mapa, para tener un objetivo fijo al comenzar mi camino.

Escenarios de las historias (resumen):

- Elegir una meta e iniciar desde Inicio o desde el Mapa.
- Mostrar las metas disponibles en el modal "Iniciar nuevo recorrido".
- Intentar comenzar sin seleccionar una meta.
- Cancelar el inicio de un recorrido.
- Botón "+" deshabilitado durante un recorrido.
- Permiso de ubicación rechazado, solo ubicación aproximada y ubicación del dispositivo desactivada.
- Esperar una señal GPS precisa (solo HU-03).

## Introduction

Desde Inicio o desde el Mapa, el corredor toca el botón "+" y la opción "Iniciar un recorrido". Se
abre un modal donde elige una meta de 3, 5 o 10 km y toca "Comenzar". El mismo modal sirve para las
dos pantallas. Antes de empezar, Trazo necesita el permiso de ubicación precisa y la ubicación del
dispositivo activada.

El trabajo se hace en dos fases: primero el modal y la elección de la meta (Requirements 1 a 5),
después los permisos y los avisos de ubicación (Requirements 6 a 8). La fase 3 (Requirements 9 y 10)
suma el botón "+" único en Inicio y en Mapa y el arranque real del recorrido con señal GPS.

## Boundary Context

- **In scope**:
    - El modal "Iniciar nuevo recorrido" con título, descripción, las tres metas y los botones
      "Cancelar" y "Comenzar".
    - La elección de una sola meta entre 3, 5 y 10 km, sin distancia personalizada.
    - Qué pasa al comenzar sin meta, al cancelar y al comenzar con una meta.
    - Abrir el mismo modal desde Inicio y desde el Mapa.
    - (Fase 2) Pedir el permiso de ubicación, explicar por qué se necesita, botón "Ir a
      configuración", ubicación aproximada, ubicación del dispositivo desactivada y espera de señal
      GPS.
    - (Fase 3) El botón "+" y la opción "Iniciar un recorrido" como **un único componente** usado en
      Inicio y en Mapa, con toda su lógica (permisos, ubicación, modal, espera de señal y
      deshabilitado) en un único ViewModel compartido.
    - (Fase 3) El botón "+" deshabilitado mientras hay un recorrido en curso, en Inicio y en Mapa.
    - (Fase 3) Entregarle a HU-04 la sesión inicial (meta, fecha y hora de inicio e instante
      monotónico) recién cuando hay señal GPS precisa.
- **Out of scope**:
    - El registro real del recorrido: distancia, tiempo, traza y puntos GPS (HU-04).
    - El modo demo y la pantalla de Configuración.
- **Adjacent expectations**:
    - HU-01 dibuja Inicio y deja el lugar del botón "+"; el botón y su lógica son de esta spec.
    - HU-04 expone `isRunInProgress` y `attachActiveSession(InitializedRunSession)`, y empieza a
      registrar el recorrido en cuanto recibe la sesión.
    - La meta usa el enum `RunGoal` de HU-04 (`domain/model/map/RunGoal`); esta spec no define otro.

## Requirements

### Requirement 1: Modal "Iniciar nuevo recorrido"

**Objective:** As a corredor, I want ver las metas disponibles al iniciar un recorrido, so that
pueda elegir una distancia como objetivo.

#### Acceptance Criteria

1. When el corredor toca "Iniciar un recorrido", the Trazo app shall mostrar el modal con el título
   "Iniciar nuevo recorrido".
2. While el modal está abierto, the Trazo app shall oscurecer el fondo de la pantalla.
3. The modal shall mostrar la descripción "Seleccioná una meta:" y las opciones "3 km", "5 km" y "10
   km".
4. The modal shall ofrecer solo los botones "Cancelar" y "Comenzar", sin permitir una distancia
   personalizada.

### Requirement 2: Elegir una meta

**Objective:** As a corredor, I want elegir una única meta, so that mi recorrido tenga un objetivo
fijo.

#### Acceptance Criteria

1. When el corredor toca una meta, the Trazo app shall marcarla como seleccionada.
2. When el corredor toca otra meta, the Trazo app shall dejar seleccionada solo la nueva.
3. While no hay una meta seleccionada, the Trazo app shall no tener ninguna opción marcada.

### Requirement 3: Comenzar sin meta

**Objective:** As a corredor, I want que Trazo me avise si no elegí una meta, so that no empiece un
recorrido sin objetivo.

#### Acceptance Criteria

1. While no hay una meta seleccionada, the Trazo app shall no comenzar ningún recorrido.
2. While no hay una meta seleccionada, the Trazo app shall indicar que hay que elegir una meta para
   continuar.

### Requirement 4: Cancelar

**Objective:** As a corredor, I want cancelar el inicio, so that pueda volver sin empezar nada.

#### Acceptance Criteria

1. When el corredor toca "Cancelar", the Trazo app shall cerrar el modal.
2. When el corredor toca "Cancelar", the Trazo app shall no comenzar ningún recorrido.
3. When el corredor toca "Cancelar", the Trazo app shall dejar al corredor en la misma pantalla
   donde estaba (Inicio o Mapa).

### Requirement 5: Comenzar con una meta

**Objective:** As a corredor, I want comenzar con la meta que elegí, so that mi recorrido empiece
con ese objetivo.

#### Acceptance Criteria

1. When el corredor toca "Comenzar" con una meta seleccionada, the Trazo app shall cerrar el modal.
2. When el corredor toca "Comenzar" con una meta seleccionada desde Inicio, the Trazo app shall
   abrir la pantalla "Mapa".
3. When el corredor toca "Comenzar" con una meta seleccionada y hay señal GPS precisa, the Trazo
   app shall entregar la meta elegida al registro del recorrido (Requirement 10).
4. The Trazo app shall usar el mismo modal desde Inicio y desde el Mapa.

### Requirement 6 (Fase 2): Permiso de ubicación

**Objective:** As a corredor, I want entender por qué Trazo pide mi ubicación, so that pueda decidir
si la comparto.

#### Acceptance Criteria

1. When el corredor toca "Iniciar un recorrido" sin haber dado el permiso de ubicación, the Trazo
   app shall pedir el permiso.
2. If el corredor rechaza el permiso, then the Trazo app shall no abrir el modal y shall explicar
   por qué Trazo necesita la ubicación.
3. If el corredor rechaza el permiso, then the Trazo app shall mostrar el botón "Ir a configuración"
   y no comenzar el registro.

### Requirement 7 (Fase 2): Ubicación aproximada

**Objective:** As a corredor, I want saber que necesito la ubicación precisa, so that el recorrido
se registre bien.

#### Acceptance Criteria

1. When el corredor solo dio permiso de ubicación aproximada y toca "Iniciar un recorrido", the
   Trazo app shall informar que necesita la ubicación precisa.
2. When eso ocurre, the Trazo app shall mostrar el botón "Ir a configuración" y no comenzar el
   registro.

### Requirement 8 (Fase 2): Ubicación del dispositivo y señal GPS

**Objective:** As a corredor, I want que Trazo me avise si la ubicación está apagada o la señal es
débil, so that sepa por qué no empieza.

#### Acceptance Criteria

1. When la ubicación del dispositivo está desactivada y el corredor toca "Iniciar un recorrido", the
   Trazo app shall avisar que debe activarla y no comenzar el registro.
2. When la precisión de la ubicación es peor que 30 metros, the Trazo app shall mostrar "Esperando
   señal GPS" en la pantalla "Mapa", junto con la meta elegida, y no registrar el punto de inicio.
3. When la precisión mejora a 30 metros o menos, the Trazo app shall comenzar el registro.
4. While Trazo espera la señal GPS, the Trazo app shall ofrecer "Cancelar"; when el corredor lo
   toca, the Trazo app shall dejar de esperar y no comenzar ningún recorrido.
5. If la ubicación deja de estar disponible mientras Trazo espera la señal, then the Trazo app shall
   dejar de esperar, no comenzar el registro y mostrar el aviso de ubicación que corresponda.

### Requirement 9 (Fase 3): Botón "+" único en Inicio y en Mapa

**Objective:** As a corredor, I want el mismo botón "+" en Inicio y en Mapa, so that inicie un
recorrido de la misma forma desde las dos pantallas.

#### Acceptance Criteria

1. The Trazo app shall mostrar el botón "+" en Inicio y en Mapa con el mismo aspecto y
   comportamiento.
2. When el corredor toca el botón "+" habilitado, the Trazo app shall mostrar u ocultar la opción
   "Iniciar un recorrido".
3. When el corredor toca fuera de la opción, the Trazo app shall ocultarla sin iniciar nada.
4. When el corredor toca "Iniciar un recorrido", the Trazo app shall ocultar la opción y seguir el
   flujo de permisos y ubicación (Requirements 6 a 8) antes de abrir el modal.
5. While hay un recorrido en curso (activo, pausado, frenado o en pausa personal), the Trazo app
   shall mostrar el botón "+" deshabilitado en Inicio y en Mapa y no mostrar la opción.
6. When el recorrido se finaliza o se descarta, the Trazo app shall volver a habilitar el botón "+".
7. While Trazo espera la señal GPS para comenzar, the Trazo app shall mostrar el botón "+"
   deshabilitado.

### Requirement 10 (Fase 3): Comenzar el registro con señal GPS

**Objective:** As a corredor, I want que el recorrido empiece recién cuando Trazo sabe dónde estoy,
so that la traza y la distancia arranquen bien.

#### Acceptance Criteria

1. When llega una ubicación con precisión de 30 metros o menos durante la espera, the Trazo app
   shall entregarle a HU-04 una sesión inicial con un identificador nuevo, la meta elegida, la fecha
   y hora reales de inicio y el instante monotónico de inicio.
2. When la sesión se entrega, the Trazo app shall dejar de mostrar "Esperando señal GPS".
3. The Trazo app shall entregar la sesión una sola vez por cada "Comenzar".

## Decisiones tomadas (antes «Open Questions»)

1. **"Comenzar" sin meta:** el botón aparece deshabilitado y el modal muestra "Elegí una meta para
   continuar" (implementado).
2. **Texto de la descripción:** voseo, "Seleccioná una meta:".
3. **Dónde vive el modal:** las piezas visuales en `ui/components/common/` (`StartRunDialog`,
   `GoalChip`, `PrimaryButton`, `StartRunFab`) y la lógica en `ui/screens/startrun/StartRunViewModel`.
4. **Botón "+":** un único `StartRunFab` en Inicio y en Mapa; el componente no tiene lógica y todos
   sus eventos van a `StartRunViewModel`.
5. **Espera de señal GPS:** se muestra en Mapa, después de cerrar el modal, con la meta y "Cancelar".

## Open Questions (anteriores, ya resueltas arriba)

1. **"Comenzar" sin meta:** la historia dice que el botón se puede tocar y Trazo indica que hay que
   elegir una meta. El design system dice que "Comenzar" aparece deshabilitado hasta elegir una
   meta. Propuesta: seguir el design system (botón deshabilitado) y mostrar el texto "Elegí una meta
   para continuar" en el modal, así se cumplen las dos cosas.
2. **Texto de la descripción:** la historia escribe "Selecciona una meta:" y el design system usa
   voseo ("Seleccioná"). Propuesta: voseo.
3. **Dónde vive el modal:** el design system lo ubica en `ui/components/common`, porque lo usan
   Inicio y Mapa, y el plan lo asigna a la carpeta `startrun/`. Propuesta: `ui/screens/startrun/`
   para la lógica y `ui/components/common/` para las piezas (botón, chip de meta).
