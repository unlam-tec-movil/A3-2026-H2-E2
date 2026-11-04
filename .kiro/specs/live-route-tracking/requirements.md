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
- Se muestra el mapa centrado en su posición actual, si el permiso de ubicación y la ubicación están disponibles.
- Las métricas iniciales muestran el temporizador en 00:00:00, la distancia en 0 km y la velocidad en 0 km/h.
- El corredor puede iniciar una carrera desde el botón flotante (FAB "+"), siguiendo el flujo de inicio existente (`start-run`).
- Los controles de Pausar, Reanudar y Finalizar no permiten realizar acciones.
- La representación visual específica de habilitación o deshabilitación de los controles de Pausar, Reanudar y Finalizar queda explícitamente pendiente de definición con el equipo (no se asume su estado visual inicial).

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

*Aclaración:* El indicador visual de calidad de señal GPS debe manejarse independientemente del filtro de precisión de los puntos. La precisión de una ubicación no determina automáticamente el estado visual de las barras de señal (no se asume que una precisión ≤ 30 m determine directamente las barras de señal).

#### Escenario 4: Pérdida de señal GPS
Dado que hay un recorrido en curso,
Cuando transcurren 15 segundos consecutivos sin recibir ubicaciones GPS válidas,
Entonces:
- Se muestra el aviso "Sin señal GPS".
- No se incorporan puntos inválidos a la traza ni se suma distancia a partir de ellos.
- El registro continúa cuando se recibe nuevamente una ubicación GPS válida.

*Aclaración:* El temporizador de 15 segundos se contabiliza estrictamente desde la última ubicación GPS válida recibida, y no desde cualquier lectura GPS. Una ubicación válida debe cumplir el criterio de precisión establecido en el escenario 5.

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
- Se detiene el temporizador.
- Se detiene la acumulación de distancia recorrida.
- Se detiene el registro de nuevos puntos GPS en la traza.
- El botón "Pausar" cambia a "Reanudar".

**6.2 - Reanudar**
Dado que el recorrido está pausado,
Cuando el corredor toca el botón "Reanudar",
Entonces:
- Se continúa el temporizador.
- Se continúa la acumulación de distancia recorrida.
- Se continúa el registro de nuevos puntos GPS en la traza.
- El botón "Reanudar" cambia a "Pausar".

#### Escenario 7: Centrar el mapa en mi posición actual
Dado que hay un recorrido en curso y el corredor desplazó manualmente el mapa,
Cuando toca el botón de ubicación actual,
Entonces:
- El mapa vuelve a centrarse en su posición actual.
- La traza del recorrido permanece visible.

#### Escenario 8: Cambiar de pestaña sin perder el recorrido
Dado que hay un recorrido en curso,
Cuando el corredor cambia de la pestaña "Mapa" a "Historial" (u otra pestaña),
Entonces:
- El recorrido continúa en curso conservando su estado actual (activo o pausado).
- Si el recorrido está pausado al cambiar de pestaña, el temporizador, la distancia y el registro de la traza se mantienen detenidos sin avanzar.
- Al volver a la pestaña "Mapa" se restaura el estado y se muestran las métricas y la traza correspondientes.

#### Escenario 9: Meta alcanzada
Dado que hay un recorrido en curso con una meta establecida (3 km, 5 km o 10 km),
Cuando el corredor alcanza la distancia de la meta fijada,
Entonces:
- La aplicación celebra mostrando una animación de celebración de meta cumplida.
- No se asume finalización automática de la carrera.
- Queda pendiente de definición con el equipo si alcanzar la meta finaliza automáticamente el recorrido o si el corredor debe tocar el botón "Finalizar".

#### Escenario 10: Finalizar y guardar el recorrido [PROPUESTA PENDIENTE DE APROBACIÓN DEL EQUIPO]
*(Propuesto al no encontrarse especificación previa en otras funcionalidades del proyecto; sujeto a revisión y aprobación del equipo)*

**10.1 - Solicitar confirmación**
Dado que hay un recorrido activo o pausado,
Cuando el corredor toca el botón "Finalizar",
Entonces:
- La app muestra un diálogo de confirmación para evitar finalizaciones accidentales.

**10.2 - Confirmar finalización**
Dado que se muestra el diálogo de confirmación de finalización,
Cuando el corredor confirma la finalización,
Entonces:
- Se detienen definitivamente el temporizador y el registro GPS.
- Se congelan las métricas finales (tiempo, distancia, velocidad) y la traza.
- Se continúa al flujo de guardado del recorrido (`save-run`).

**10.3 - Cancelar finalización**
Dado que se muestra el diálogo de confirmación de finalización,
Cuando el corredor cancela la finalización,
Entonces:
- Se cierra el diálogo de confirmación.
- Se conserva el estado previo del recorrido (activo o pausado), sin modificar métricas ni traza.

**10.4 - Guardar recorrido (flujo `save-run`)**
Dado que el corredor continúa al flujo de guardado del recorrido finalizado,
Cuando completa el formulario de guardado,
Entonces:
- Debe ingresar obligatoriamente un título para el recorrido.
- Puede seleccionar destacados de forma opcional.
- Puede agregar hasta 2 fotos opcionales capturadas con la cámara (no se admiten videos).
- Al guardar, el recorrido queda almacenado localmente en Room y disponible en la pestaña "Historial", con su traza visible sobre el mapa.

### Puntos pendientes de revisión con el equipo
- **Representación visual de controles sin recorrido:** Se acordó que los controles de Pausar, Reanudar y Finalizar no permiten acciones mientras no haya un recorrido en curso. Queda pendiente definir con el equipo su representación visual concreta (deshabilitados, atenuados u otra solución visual).
- **Finalización al alcanzar la meta:** Se acordó mostrar la animación de celebración al alcanzar la meta fijada (3, 5 o 10 km). Queda pendiente definir si alcanzar la meta finaliza automáticamente el recorrido o si el corredor debe tocar "Finalizar" de manera explícita.
- **Navegación posterior a la finalización:** Queda pendiente definir si, tras confirmar la finalización, se navega directamente al flujo de guardado (`save-run`) o si se muestra primero una pantalla intermedia de resultados («¡Meta cumplida!» / «Recorrido finalizado»).
- **Aprobación del flujo de finalización propuesto:** La secuencia de diálogo de confirmación, detención definitiva, congelamiento de métricas y continuidad al flujo de guardado queda sujeta a la aprobación formal del equipo.

## Introduction
Esta funcionalidad permite a los corredores seguir su actividad en tiempo real desde la pestaña "Mapa" de Trazo (HU-04). Mientras el corredor se desplaza, la aplicación muestra su ubicación sobre el mapa, traza la ruta en vivo y actualiza las métricas clave: tiempo transcurrido, distancia acumulada y velocidad instantánea. La funcionalidad proporciona controles para pausar y reanudar la salida, recentrar la vista del mapa y asegurar la persistencia del estado de la sesión al navegar entre pestañas de la aplicación. 

Asimismo, contempla el estado inicial del mapa sin carrera activa (con métricas en cero, controles inhabilitados para acciones y acceso al inicio mediante el botón flotante FAB "+"), la celebración con animación al alcanzar una meta predefinida (3, 5 o 10 km), mecanismos de robustez para descartar lecturas GPS imprecisas (precisión superior a 30 metros), alertas de pérdida de señal tras 15 segundos sin lecturas válidas (contabilizados desde la última ubicación GPS válida) e integración con el flujo de guardado local (`save-run`).

En alineación con la definición de producto de Trazo v0.6:
- La aplicación funciona de manera 100% local, almacenando los recorridos en una base de datos Room, sin cuentas de usuario, perfiles, comunidad, rankings, seguidores ni servidores/APIs externas.
- Las metas disponibles son exclusivamente fijas (3 km, 5 km y 10 km); no existen metas personalizadas.
- No se registran calorías, planes de entrenamiento ni integración con pulsómetros o dispositivos wearables.
- En el guardado se admiten hasta 2 fotos tomadas con la cámara, sin soporte de video.
- Durante el seguimiento, la pantalla se mantiene activa para evitar la suspensión del dispositivo; no se garantiza seguimiento en segundo plano prolongado.
- Todos los textos de la interfaz están redactados en español rioplatense con voseo, sin soporte para múltiples idiomas ni internacionalización.

## Boundary Context
- **In scope**:
  - Estado inicial de la pestaña "Mapa" sin recorrido en curso: centrado en la ubicación actual (si permisos y ubicación están disponibles), métricas en cero ("00:00:00", "0 km", "0 km/h"), inicio de carrera vía botón flotante (FAB "+"), e inhabilitación funcional de acciones en controles de Pausar, Reanudar y Finalizar (representación visual pendiente de definición).
  - Visualización continua de la posición actual del corredor y trazado en vivo de la ruta sobre el mapa.
  - Cálculo y presentación continua de las métricas de recorrido: tiempo transcurrido, distancia recorrida y velocidad actual.
  - Filtrado de puntos GPS con precisión superior a 30 metros (descarte de traza y de incremento de distancia).
  - Indicador visual de calidad de señal GPS (manejado independientemente del filtro de precisión).
  - Detección de pérdida de señal tras 15 segundos consecutivos sin puntos válidos (contabilizados desde la última ubicación GPS válida), mostrando el aviso "Sin señal GPS" y excluyendo puntos inválidos.
  - Controles de pausa y reanudación del recorrido con alternancia de estado y detención/reanudación integral de temporizador, distancia y traza.
  - Recentrado del mapa sobre la ubicación actual tras desplazamientos manuales del corredor.
  - Conservación del estado de la sesión (activo o pausado) al navegar entre pestañas de la app, manteniendo detenidos temporizador, distancia y traza si está pausado, y restaurando estado y métricas al volver a Mapa.
  - Presentación de animación de celebración al alcanzar la meta de distancia (3, 5 o 10 km), dejando pendiente la definición de finalización automática.
  - Propuesta del flujo de finalización del recorrido: confirmación obligatoria mediante diálogo, detención definitiva de temporizador y GPS, congelamiento de métricas y traza, y transición hacia el guardado de recorrido (`save-run`).
  - Criterios de integración con el guardado local (`save-run`): requerimiento de título obligatorio, destacados opcionales, hasta 2 fotos opcionales capturadas con la cámara, persistencia en Room y visualización posterior en Historial y sobre el mapa.
- **Out of scope**:
  - Modal y selección de meta de inicio del recorrido (responsabilidad de la funcionalidad `start-run`).
  - Pantalla y componentes detallados del formulario de guardado (responsabilidad de la funcionalidad `save-run`).
  - Definición de pantalla de resultados intermedia («¡Meta cumplida!» / «Recorrido finalizado»), sujeta a la definición pendiente de navegación del equipo.
  - Cuentas de usuario, autenticación, perfiles, comunidad, rankings o seguidores.
  - Servidor propio, backend externo o sincronización en la nube (la persistencia es estrictamente local en Room).
  - Metas personalizadas (únicamente se contemplan metas fijas de 3 km, 5 km y 10 km).
  - Cálculo de calorías, pulsómetros, wearables y planes de entrenamiento.
  - Grabación o adjunto de videos (solo fotos, máximo 2).
  - Garantía de ejecución y seguimiento continuo en segundo plano durante períodos prolongados (la app mantiene la pantalla activa durante la carrera).
  - Soporte para múltiples idiomas o internacionalización (la aplicación se mantiene exclusivamente en español).
  - Detección y alertas de frenadas bruscas o irregularidades del suelo (cubiertas por historias especializadas de sensores).
- **Adjacent expectations**:
  - `start-run`: Entrega a la pestaña "Mapa" un recorrido iniciado con su meta seleccionada (3 km, 5 km o 10 km). El botón flotante (FAB "+") dispara este flujo de inicio cuando no hay un recorrido en curso.
  - `save-run`: Recibe las métricas congeladas y la traza tras la confirmación de finalización, solicitando título obligatorio, destacados opcionales y hasta 2 fotos para persistir el recorrido en Room y reflejarlo en Historial y el mapa.
  - Puntos pendientes de resolución por el equipo de diseño y producto:
    1. Representación visual de habilitación/deshabilitación de controles sin recorrido.
    2. Comportamiento ante la meta alcanzada (finalización automática vs. toque manual de Finalizar).
    3. Flujo de navegación post-finalización (directo a Guardar vs. pantalla de resultados intermedia).

## Requirements

### Requirement 1: Estado inicial del mapa y controles sin recorrido
**Objective:** As a corredor, I want ver el mapa listo y centrado en mi ubicación al ingresar sin un recorrido activo, junto con las métricas en cero y la posibilidad de iniciar una salida, so that pueda orientarme y comenzar cuando lo decida sin activar controles involuntarios.

#### Acceptance Criteria
1. When el corredor abre la pestaña "Mapa" sin un recorrido en curso, if los permisos de ubicación y la ubicación actual están disponibles, then the Trazo app shall centrar la vista del mapa en la posición actual del corredor.
2. While no hay un recorrido en curso, the Trazo app shall mostrar las métricas iniciales con el temporizador en "00:00:00", la distancia en "0 km" y la velocidad en "0 km/h".
3. While no hay un recorrido en curso, when el corredor interactúa con el botón flotante (FAB "+"), the Trazo app shall permitir iniciar una carrera siguiendo el flujo de inicio de `start-run`.
4. While no hay un recorrido en curso, the Trazo app shall no permitir acciones al interactuar con los controles de Pausar, Reanudar y Finalizar.
5. While no hay un recorrido en curso, the Trazo app shall [PENDIENTE DE DEFINICIÓN CON EL EQUIPO: definir la representación visual de habilitación o deshabilitación de los controles de Pausar, Reanudar y Finalizar sin asumir un estado visual previo].

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
2. The Trazo app shall determinar el estado visual de las barras de señal GPS de forma independiente del filtro de precisión de los puntos, sin asumir que una precisión menor o igual a 30 metros determina automáticamente las barras de señal.
3. While un recorrido está en curso, if transcurren 15 segundos consecutivos sin recibir ubicaciones GPS válidas (contabilizados estrictamente a partir de la última ubicación GPS válida recibida y no desde cualquier lectura GPS), then the Trazo app shall mostrar el aviso "Sin señal GPS".
4. While se muestra el aviso "Sin señal GPS", the Trazo app shall no incorporar puntos inválidos a la traza ni sumar distancia recorrida a partir de ellos.
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
1. While un recorrido está en curso y activo, when el corredor toca el control "Pausar", the Trazo app shall detener el conteo del temporizador, suspender la acumulación de distancia y pausar el registro de nuevos puntos GPS en la traza.
2. When el recorrido pasa a estado pausado, the Trazo app shall cambiar el control de "Pausar" a "Reanudar".
3. While un recorrido está pausado, when el corredor toca el control "Reanudar", the Trazo app shall continuar el conteo del temporizador, la acumulación de distancia y el registro de nuevos puntos GPS en la traza.
4. When el recorrido se reanuda, the Trazo app shall cambiar el control de "Reanudar" a "Pausar".

### Requirement 6: Recentrado de la vista del mapa
**Objective:** As a corredor, I want volver a enfocar mi posición actual tras haber explorado el mapa, so that no pierda de vista mi ubicación en carrera.

#### Acceptance Criteria
1. While un recorrido está en curso y el corredor ha desplazado manualmente el mapa, when el corredor toca el botón de ubicación actual, the Trazo app shall volver a centrar la vista del mapa en la posición actual del corredor.
2. When el mapa se recentra en la posición actual, the Trazo app shall mantener visible la traza completa acumulada del recorrido.

### Requirement 7: Persistencia del estado durante la navegación entre pestañas
**Objective:** As a corredor, I want cambiar de pestaña sin que se interrumpa mi recorrido, so that pueda consultar otras secciones de la app con tranquilidad.

#### Acceptance Criteria
1. While un recorrido está en curso, when el corredor cambia de la pestaña "Mapa" a otra pestaña (como "Historial" o "Inicio"), the Trazo app shall mantener el recorrido en curso conservando su estado actual (activo o pausado).
2. While el recorrido está activo durante la navegación fuera de la pestaña "Mapa", the Trazo app shall continuar incrementando el temporizador y acumulando la distancia y la traza.
3. While el recorrido está pausado durante la navegación fuera de la pestaña "Mapa", the Trazo app shall mantener detenidos el temporizador, la distancia y el registro de la traza, conservando el estado pausado sin avanzar métricas.
4. When el corredor vuelve a la pestaña "Mapa", the Trazo app shall restaurar el estado correspondiente (activo o pausado), mostrando las métricas actualizadas y la traza consolidada.

### Requirement 8: Meta alcanzada y animación de celebración
**Objective:** As a corredor, I want que la aplicación reconozca visualmente cuando alcancé la meta de mi salida, so that pueda celebrar mi logro sin interrumpir abruptamente mi experiencia.

#### Acceptance Criteria
1. While un recorrido con meta (3 km, 5 km o 10 km) está en curso y activo, when la distancia acumulada alcanza la distancia de la meta fijada, the Trazo app shall mostrar una animación de celebración de meta cumplida.
2. While se muestra la animación de celebración de meta alcanzada, the Trazo app shall [PENDIENTE DE DEFINICIÓN CON EL EQUIPO: no asumir finalización automática del recorrido, dejando pendiente de definición si alcanzar la meta finaliza automáticamente la sesión o si el corredor debe tocar el control Finalizar para concluir el recorrido].

### Requirement 9: Finalización del recorrido [PROPUESTA PENDIENTE DE APROBACIÓN DEL EQUIPO]
**Objective:** As a corredor, I want poder finalizar mi recorrido de forma segura mediante confirmación y congelar mis métricas, so that no pierda datos accidentalmente y pueda continuar hacia el guardado de mi salida.

#### Acceptance Criteria
1. While un recorrido está en curso (activo o pausado), when el corredor toca el control "Finalizar", the Trazo app shall [PROPUESTA PENDIENTE DE APROBACIÓN: mostrar un diálogo de confirmación para evitar finalizaciones accidentales].
2. While se muestra el diálogo de confirmación de finalización, when el corredor cancela la acción, the Trazo app shall [PROPUESTA PENDIENTE DE APROBACIÓN: cerrar el diálogo y conservar el estado previo del recorrido (activo o pausado) sin modificar las métricas ni la traza].
3. While se muestra el diálogo de confirmación de finalización, when el corredor confirma la finalización, the Trazo app shall [PROPUESTA PENDIENTE DE APROBACIÓN: detener de forma definitiva el temporizador y el registro GPS, congelar las métricas finales y la traza, y continuar hacia el flujo de guardado de recorrido de `save-run`].
4. When la finalización es confirmada, the Trazo app shall [PENDIENTE DE DEFINICIÓN CON EL EQUIPO: definir si se navega directamente al flujo de guardado de `save-run` o si se muestra previamente una pantalla intermedia de resultados («¡Meta cumplida!» / «Recorrido finalizado»), sin asumir ninguna alternativa como predeterminada].

### Requirement 10: Integración con el guardado y consulta local del recorrido [PROPUESTA PENDIENTE DE APROBACIÓN DEL EQUIPO]
**Objective:** As a corredor, I want que mi recorrido finalizado pueda ser guardado con un título y elementos opcionales para consultarlo en mi historial y en el mapa, so that mantenga un registro permanente y confiable de mis salidas sin requerir cuentas externas ni conexión a internet.

#### Acceptance Criteria
1. When el corredor avanza en el flujo de guardado del recorrido finalizado (`save-run`), the Trazo app shall requerir obligatoriamente el ingreso de un título para permitir guardar la actividad.
2. Where el corredor se encuentra en el flujo de guardado del recorrido (`save-run`), the Trazo app shall permitir seleccionar etiquetas de destacados de forma opcional.
3. Where el corredor se encuentra en el flujo de guardado del recorrido (`save-run`), the Trazo app shall permitir adjuntar hasta dos fotos opcionales tomadas con la cámara, sin admitir grabación ni adjunto de videos.
4. When el corredor confirma el guardado del recorrido (`save-run`), the Trazo app shall almacenar el recorrido con sus métricas finales, traza, título, destacados y fotos de forma estrictamente local en Room sin enviar datos a servidores externos ni requerir cuentas de usuario.
5. When un recorrido queda almacenado localmente en Room, the Trazo app shall dejarlo disponible en la pestaña "Historial" y mostrar su traza correspondiente en el mapa.

### Requirement 11: Alcance de plataforma, restricciones de hardware y accesibilidad
**Objective:** As a corredor, I want una interfaz en mi idioma, accesible, clara y adaptada a las capacidades del dispositivo, so that pueda utilizar la app cómodamente durante la actividad física respetando los límites de diseño de Trazo v0.6.

#### Acceptance Criteria
1. The Trazo app shall mostrar todos los textos de esta funcionalidad exclusivamente en idioma español rioplatense con voseo, sin incorporar soporte para múltiples idiomas ni internacionalización.
2. The Trazo app shall admitir únicamente metas predefinidas de 3 km, 5 km y 10 km, sin permitir configuración de metas personalizadas ni cálculo de calorías, planes de entrenamiento ni integración con pulsómetros o dispositivos wearables.
3. While un recorrido está en curso en la pestaña "Mapa", the Trazo app shall mantener la pantalla activa para evitar la suspensión del dispositivo durante la actividad física, sin garantizar el seguimiento continuo en segundo plano durante períodos prolongados.
4. The Trazo app shall mostrar las métricas de distancia y velocidad utilizando la coma como separador decimal.
5. The Trazo app shall asegurar que todos los controles táctiles del mapa y del panel de recorrido tengan un área táctil mínima de 48 × 48 dp (y 56 × 56 dp para los controles de Pausar, Reanudar y Finalizar).
6. The Trazo app shall proveer descripciones accesibles para lectores de pantalla en los botones de control de recorrido, el botón flotante de inicio, el botón de recentrado y los avisos de estado de señal GPS.
