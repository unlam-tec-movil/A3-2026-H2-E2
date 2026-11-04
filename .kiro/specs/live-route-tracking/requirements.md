# Requirements Document

## Project Description (Input)
# HU-04 — Seguir el recorrido en vivo

### Descripción de la funcionalidad
**Característica:** Seguir un recorrido en curso desde la pestaña "Mapa".

**Historia de usuario:**
Como corredor que inició un recorrido, quiero seguir mi recorrido en vivo en el mapa, y poder pausarlo, reanudarlo o finalizarlo, para controlar y registrar mi salida.

### Escenarios de aceptación

#### Escenario 1: Mapa sin recorrido en curso
Dado que no hay un recorrido en curso,
Cuando el corredor abre la pestaña "Mapa",
Entonces:
- Se muestra el mapa centrado en su posición actual.
- El temporizador muestra 00:00:00.
- La distancia muestra 0 km.
- La velocidad muestra 0 km/h.
- Los botones de "Reanudar" y "Finalizar" están deshabilitados.

#### Escenario 2: Visualizar el recorrido en vivo
Dado que hay un recorrido en curso y el GPS proporciona ubicaciones válidas,
Cuando el corredor avanza,
Entonces:
- Se muestra su posición actual en el mapa.
- Se actualiza la traza del recorrido en vivo.
- Se actualiza el tiempo transcurrido.
- Se actualiza la distancia recorrida.
- Se actualiza la velocidad.

#### Escenario 3: Mostrar buena señal GPS
Dado que hay un recorrido en curso,
Cuando la señal GPS tiene calidad suficiente,
Entonces:
- El indicador "GPS" muestra la señal con barras completas.
- El registro del recorrido continúa normalmente.

*Aclaración:* El indicador de calidad de señal GPS debe manejarse independientemente del filtro de precisión de los puntos. La precisión de una ubicación no debe determinar automáticamente el estado visual de las barras de señal.

#### Escenario 4: Pérdida de señal GPS
Dado que hay un recorrido en curso,
Cuando transcurren 15 segundos consecutivos sin recibir ubicaciones GPS válidas,
Entonces:
- Se muestra el aviso "Sin señal GPS".
- No se agregan puntos inválidos a la traza.
- El registro continúa cuando se recibe nuevamente una ubicación GPS válida.

*Aclaración:* Los 15 segundos se cuentan desde la última ubicación GPS válida recibida, no simplemente desde la última lectura GPS. Una ubicación válida debe cumplir el criterio de precisión establecido en el escenario 5.

#### Escenario 5: Descartar un punto GPS impreciso
Dado que hay un recorrido en curso,
Cuando se recibe un punto GPS con una precisión superior a 30 metros,
Entonces:
- El punto se descarta.
- No se incorpora a la traza.
- No suma distancia a partir de ese punto.

#### Escenario 6: Pausar y reanudar el recorrido

**6.1 - Pausar**
Dado que hay un recorrido en curso,
Cuando el corredor toca el botón "Pausar",
Entonces:
- Se pausa el temporizador.
- Se detiene el registro de la traza.
- El botón "Pausar" cambia a "Reanudar".

**6.2 - Reanudar**
Dado que el recorrido está pausado,
Cuando el corredor toca el botón "Reanudar",
Entonces:
- Se reanuda el temporizador.
- Se reanuda el registro de la traza.
- El botón "Reanudar" cambia a "Pausar".

#### Escenario 7: Centrar el mapa en mi posición actual
Dado que hay un recorrido en curso y el corredor desplazó manualmente el mapa,
Cuando toca el botón de ubicación actual,
Entonces:
- El mapa vuelve a centrarse en su posición actual.
- La traza del recorrido permanece visible.

#### Escenario 8: Cambiar de pestaña sin perder el recorrido
Dado que hay un recorrido en curso,
Cuando el corredor cambia de la pestaña "Mapa" a "Historial",
Entonces:
- El recorrido continúa en curso.
- Al volver a la pestaña "Mapa" se muestra nuevamente la traza actualizada.
- El temporizador mantiene el tiempo transcurrido.

*Aclaración:* Si el recorrido está pausado al cambiar de pestaña, debe conservarse el estado pausado. El temporizador y el registro de la traza solo continúan cuando el recorrido está activo.

### Puntos pendientes de revisión con el equipo
- **Botón "Finalizar":** Se mantiene pendiente para verificar si ya está contemplado en otra historia del proyecto o si se definirán escenarios adicionales para la confirmación y conclusión del recorrido.

## Introduction
Esta funcionalidad permite a los corredores seguir su actividad en tiempo real desde la pestaña "Mapa" de Trazo. Mientras el corredor se desplaza, la aplicación muestra su ubicación sobre el mapa, traza la ruta seguida en vivo y actualiza las métricas clave: tiempo transcurrido, distancia acumulada y velocidad instantánea. La funcionalidad proporciona controles para pausar y reanudar la salida, recentrar la vista del mapa y asegurar la continuidad de la sesión al navegar entre pestañas de la app. Asimismo, incorpora mecanismos de robustez para descartar lecturas GPS imprecisas (mayores a 30 metros) y alertar al usuario ante la pérdida prolongada de señal válida (15 segundos).

## Boundary Context
- **In scope**:
  - Estado inicial de la pestaña "Mapa" cuando no hay un recorrido en curso.
  - Visualización de la ubicación actual del corredor y trazado en vivo sobre el mapa.
  - Cálculo y presentación continua de las métricas de recorrido: tiempo transcurrido, distancia recorrida y velocidad actual.
  - Filtrado de puntos GPS con precisión superior a 30 metros (descarte de traza y de incremento de distancia).
  - Indicador de estado de señal GPS y detección de pérdida de señal tras 15 segundos consecutivos sin puntos válidos, mostrando el aviso correspondiente.
  - Controles de pausa y reanudación del recorrido con alternancia de estado y congelamiento de métricas.
  - Recentrado del mapa sobre la ubicación actual tras desplazamientos manuales del usuario.
  - Conservación y persistencia de la sesión de recorrido (activo o pausado) al navegar entre las pestañas de la aplicación.
- **Out of scope**:
  - Modal y selección de meta de inicio del recorrido (cubierto por la funcionalidad `start-run` / HU-02 y HU-03).
  - Diálogos y pantallas de resultado posterior al fin del recorrido ("¡Meta cumplida!" / "Recorrido finalizado").
  - Pantalla y flujo de guardado de recorrido en el historial (cubierto por la funcionalidad `save-run`).
  - Detección y alertas de frenadas bruscas o irregularidades del suelo (cubiertas por historias especializadas de sensores).
- **Adjacent expectations**:
  - La funcionalidad de inicio entrega un recorrido iniciado con su meta seleccionada hacia la pestaña "Mapa".
  - El comportamiento del botón "Finalizar" queda pendiente de alineación con el equipo para determinar si se incorpora un diálogo de confirmación dentro de esta funcionalidad o si la acción delega directamente en el flujo de fin de recorrido.

## Requirements

### Requirement 1: Estado inicial del mapa sin recorrido
**Objective:** As a corredor, I want ver el mapa listo y centrado en mi ubicación al ingresar sin un recorrido activo, so that pueda orientarme antes de empezar.

#### Acceptance Criteria
1. When el corredor abre la pestaña "Mapa" sin un recorrido en curso, the Trazo app shall centrar el mapa en la posición actual del corredor.
2. While no hay un recorrido en curso, the Trazo app shall mostrar el temporizador en "00:00:00", la distancia en "0 km" y la velocidad en "0 km/h".
3. While no hay un recorrido en curso, the Trazo app shall mantener deshabilitados los controles de "Reanudar" y "Finalizar".

### Requirement 2: Visualización y métricas del recorrido en vivo
**Objective:** As a corredor, I want ver mi avance, la traza de mi camino y mis métricas en vivo, so that pueda monitorear mi rendimiento en tiempo real.

#### Acceptance Criteria
1. While un recorrido está en curso y activo, when el corredor avanza con ubicaciones GPS válidas, the Trazo app shall mostrar la posición actual del corredor sobre el mapa.
2. While un recorrido está en curso y activo, when el corredor avanza con ubicaciones GPS válidas, the Trazo app shall dibujar y extender la traza continua del recorrido en el mapa.
3. While un recorrido está en curso y activo, the Trazo app shall actualizar continuamente el tiempo transcurrido en el temporizador.
4. While un recorrido está en curso y activo, the Trazo app shall actualizar continuamente la distancia acumulada recorrida.
5. While un recorrido está en curso y activo, the Trazo app shall actualizar continuamente la velocidad actual del corredor.

### Requirement 3: Calidad y pérdida de señal GPS
**Objective:** As a corredor, I want saber si la señal GPS es buena o si se interrumpió, so that confíe en la precisión del registro de mi recorrido.

#### Acceptance Criteria
1. While un recorrido está en curso, when la señal GPS tiene calidad suficiente, the Trazo app shall mostrar el indicador "GPS" con barras completas de señal.
2. The Trazo app shall determinar el estado visual de las barras de señal GPS de forma independiente del filtro de precisión de los puntos.
3. While un recorrido está en curso, if transcurren 15 segundos consecutivos sin recibir ubicaciones GPS válidas que cumplan el criterio de precisión, then the Trazo app shall mostrar el aviso "Sin señal GPS".
4. While se muestra el aviso "Sin señal GPS", the Trazo app shall no agregar puntos a la traza ni sumar distancia.
5. When se recibe nuevamente una ubicación GPS válida tras haber mostrado "Sin señal GPS", the Trazo app shall ocultar el aviso y continuar el registro del recorrido.

### Requirement 4: Filtrado de puntos GPS imprecisos
**Objective:** As a corredor, I want que la app descarte lecturas GPS de baja precisión, so that el cálculo de distancia y el dibujo de la ruta no se distorsionen.

#### Acceptance Criteria
1. While un recorrido está en curso, if se recibe una lectura GPS con una precisión superior a 30 metros, then the Trazo app shall descartar dicha lectura.
2. When una lectura GPS es descartada por imprecisa, the Trazo app shall no incorporar el punto a la traza del recorrido.
3. When una lectura GPS es descartada por imprecisa, the Trazo app shall no sumar distancia recorrida a partir de dicho punto.

### Requirement 5: Pausa y reanudación del recorrido
**Objective:** As a corredor, I want pausar y reanudar mi salida cuando me detenga, so that el tiempo y la distancia reflejen únicamente mi actividad real.

#### Acceptance Criteria
1. While un recorrido está en curso y activo, when el corredor toca el botón "Pausar", the Trazo app shall pausar el temporizador.
2. While un recorrido está pausado, the Trazo app shall suspender el incremento de distancia y el registro de nuevos puntos en la traza.
3. When el recorrido pasa a estado pausado, the Trazo app shall cambiar el botón de control de "Pausar" a "Reanudar".
4. While un recorrido está pausado, when el corredor toca el botón "Reanudar", the Trazo app shall reanudar el conteo del temporizador y el registro de la traza y distancia.
5. When el recorrido se reanuda, the Trazo app shall cambiar el botón de control de "Reanudar" a "Pausar".

### Requirement 6: Recentrado de la vista del mapa
**Objective:** As a corredor, I want volver a enfocar mi posición actual tras haber explorado el mapa, so that no pierda de vista mi ubicación en carrera.

#### Acceptance Criteria
1. While un recorrido está en curso y el corredor ha desplazado manualmente el mapa, when el corredor toca el botón de ubicación actual, the Trazo app shall volver a centrar la vista del mapa en la posición actual del corredor.
2. When el mapa se recentra en la posición actual, the Trazo app shall mantener visible la traza completa acumulada del recorrido.

### Requirement 7: Persistencia del estado durante la navegación entre pestañas
**Objective:** As a corredor, I want cambiar de pestaña sin que se interrumpa mi recorrido, so that pueda consultar otras secciones de la app con tranquilidad.

#### Acceptance Criteria
1. While un recorrido está en curso, when el corredor cambia de la pestaña "Mapa" a otra pestaña (como "Historial" o "Inicio"), the Trazo app shall mantener el recorrido en curso.
2. While el recorrido está activo durante la navegación fuera de la pestaña "Mapa", the Trazo app shall continuar incrementando el temporizador y acumulando la distancia y la traza.
3. While el recorrido está pausado durante la navegación fuera de la pestaña "Mapa", the Trazo app shall conservar el estado pausado sin incrementar el temporizador ni la distancia.
4. When el corredor vuelve a la pestaña "Mapa", the Trazo app shall mostrar la traza actualizada y el temporizador con el tiempo transcurrido correspondiente.

### Requirement 8: Formato, idioma y accesibilidad
**Objective:** As a corredor, I want una interfaz clara, accesible y adaptada a mi idioma, so that pueda utilizar los controles con comodidad mientras corro.

#### Acceptance Criteria
1. The Trazo app shall mostrar todos los textos de esta funcionalidad en español rioplatense con voseo.
2. The Trazo app shall mostrar las métricas de distancia y velocidad utilizando la coma como separador decimal.
3. The Trazo app shall asegurar que todos los controles tocables del mapa y del panel de recorrido tengan un área táctil mínima de 48 × 48 dp.
4. The Trazo app shall proveer descripciones accesibles para lectores de pantalla en los botones de control de recorrido, el botón de recentrado y los avisos de estado de señal GPS.
