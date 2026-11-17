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
- Se muestra el mapa centrado en su posición actual, si los permisos de ubicación y la ubicación están disponibles.
- Las métricas iniciales muestran el temporizador en 00:00:00, la distancia en 0 km y la velocidad en 0 km/h.
- El corredor puede iniciar una carrera desde el botón flotante (FAB "+"), siguiendo el flujo de inicio existente (`start-run`).
- Los controles de Pausar, Reanudar y Finalizar no permiten realizar acciones.
- La representación visual específica de habilitación o deshabilitación de los controles de Pausar, Reanudar y Finalizar queda pendiente de definición con el equipo.
- La pestaña "Mapa" no solicita permisos de ubicación por su cuenta. La solicitud de permisos corresponde al flujo de inicio del recorrido (`start-run`).

#### Escenario 2: Visualizar el recorrido en vivo

Dado que hay un recorrido iniciado y el GPS proporciona ubicaciones válidas,

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

**Aclaración:** El indicador visual de calidad de señal GPS debe manejarse independientemente del filtro de precisión de los puntos. La precisión de una ubicación no determina automáticamente el estado visual de las barras de señal.

#### Escenario 4: Pérdida de señal GPS

Dado que hay un recorrido en curso,

Cuando transcurren 15 segundos consecutivos sin recibir ubicaciones GPS válidas,

Entonces:
- Se muestra el aviso "Sin señal GPS".
- No se incorporan puntos inválidos a la traza ni se suma distancia a partir de ellos.
- El registro continúa cuando se recibe nuevamente una ubicación GPS válida.

**Aclaración:** El temporizador de 15 segundos se contabiliza desde la última ubicación GPS válida recibida y no desde cualquier lectura GPS. Una ubicación válida debe cumplir el criterio de precisión establecido en el escenario 5. El recorrido se inicia desde el flujo `start-run` una vez que hay señal GPS disponible; por lo tanto, no se debe impedir el inicio por un período de espera de señal anterior a la creación de la sesión.

#### Escenario 5: Descartar un punto GPS impreciso

Dado que hay un recorrido en curso,

Cuando se recibe un punto GPS con una precisión superior a 30 metros,

Entonces:
- El punto se descarta.
- No se incorpora a la traza.
- No suma distancia a partir de ese punto.

#### Escenario 6: Pausar y reanudar el recorrido

**6.1 - Pausar**

Dado que hay un recorrido en curso y activo,

Cuando el corredor toca el botón "Pausar",

Entonces:
- Se detiene el temporizador.
- Se detiene la acumulación de distancia recorrida.
- Se detiene el registro de nuevos puntos GPS en la traza.
- El control cambia de "Pausar" a "Reanudar".
- Se conserva el estado pausado mientras el corredor navega por otras pestañas.

**6.2 - Reanudar**

Dado que el recorrido está pausado,

Cuando el corredor toca el botón "Reanudar",

Entonces:
- Se continúa el temporizador.
- Se continúa la acumulación de distancia recorrida.
- Se continúa el registro de nuevos puntos GPS en la traza.
- El control cambia de "Reanudar" a "Pausar".

#### Escenario 7: Centrar el mapa en mi posición actual

Dado que hay un recorrido en curso y el corredor desplazó manualmente el mapa,

Cuando toca el botón de ubicación actual,

Entonces:
- El mapa vuelve a centrarse en su posición actual.
- La traza del recorrido permanece visible.

#### Escenario 8: Cambiar de pestaña sin perder el recorrido

Dado que hay un recorrido en curso,

Cuando el corredor cambia de la pestaña "Mapa" a "Historial", "Inicio" u otra pestaña,

Entonces:
- El recorrido continúa en curso conservando su estado actual (activo o pausado).
- Si el recorrido está activo, el temporizador, la distancia y el registro de la traza continúan avanzando.
- Si el recorrido está pausado, el temporizador, la distancia y el registro de la traza se mantienen detenidos.
- La pantalla puede apagarse mientras el corredor está fuera de la pestaña "Mapa".
- Al volver a la pestaña "Mapa" se restaura el estado y se muestran las métricas y la traza correspondientes.

#### Escenario 9: Meta alcanzada

Dado que hay un recorrido en curso con una meta establecida (3 km, 5 km o 10 km),

Cuando el corredor alcanza la distancia de la meta fijada,

Entonces:
- La aplicación celebra mostrando una animación de celebración de meta cumplida.
- El recorrido continúa activo y el corredor puede seguir corriendo.
- La aplicación registra que la meta fue alcanzada para incluir esa información en el resumen final.
- Alcanzar la meta no finaliza automáticamente el recorrido. El corredor debe tocar el botón "Finalizar" cuando decida concluirlo.

#### Escenario 10: Finalizar y continuar al flujo de resultados y guardado

Dado que hay un recorrido activo o pausado,

Cuando el corredor toca el botón "Finalizar",

Entonces:

**10.1 - Solicitar confirmación o informar recorrido demasiado corto**

- Si la distancia recorrida es inferior a 200 metros, la aplicación muestra un aviso indicando que el recorrido es demasiado corto para guardarse.
- El aviso permite descartar el recorrido o volver a él.
- Si la distancia recorrida es igual o superior a 200 metros y la meta fue alcanzada, la aplicación muestra un diálogo de confirmación con el mensaje: "¿Estás seguro de que deseás finalizar?".
- Si la distancia recorrida es igual o superior a 200 metros y la meta no fue alcanzada, la aplicación muestra un diálogo de confirmación con el mensaje: "Todavía no llegaste a tu meta. ¿Estás seguro de que deseás finalizar?".

**10.2 - Confirmar finalización**

Dado que se muestra el diálogo de confirmación de finalización,

Cuando el corredor confirma la finalización,

Entonces:
- Se detienen definitivamente el temporizador y el registro GPS.
- Se congelan las métricas finales y la traza.
- Se genera el resumen final del recorrido, incluyendo la fecha y hora real de inicio y si la meta fue alcanzada.
- Se continúa a la pantalla de resultados antes de iniciar el flujo de guardado.

**10.3 - Cancelar finalización**

Dado que se muestra el diálogo de confirmación de finalización,

Cuando el corredor cancela la acción,

Entonces:
- Se cierra el diálogo de confirmación.
- Se conserva el estado previo del recorrido (activo o pausado), sin modificar las métricas ni la traza.

**10.4 - Mostrar resultados**

Dado que el corredor confirmó la finalización de un recorrido de al menos 200 metros,

Cuando la aplicación termina de congelar los datos del recorrido,

Entonces:
- Se muestra primero la pantalla de resultados del recorrido.
- La pantalla presenta el resultado correspondiente: "¡Meta cumplida!" si se alcanzó la meta, o "Recorrido finalizado" si no se alcanzó.
- La pantalla de resultados recibe el resumen final generado por HU-04.
- Después de los resultados, el corredor puede continuar al flujo de guardado `save-run`.

**10.5 - Descartar un recorrido demasiado corto**

Dado que se muestra el aviso de recorrido demasiado corto,

Cuando el corredor elige descartarlo,

Entonces:
- Se detienen el temporizador y el registro GPS.
- Se descarta la sesión sin guardarla.
- Se limpia el recorrido en curso.
- Se vuelve a la pestaña "Mapa" sin un recorrido en curso.

**10.6 - Volver a un recorrido demasiado corto**

Dado que se muestra el aviso de recorrido demasiado corto,

Cuando el corredor elige volver,

Entonces:
- Se cierra el aviso.
- Se conserva el estado previo del recorrido (activo o pausado).
- No se modifican las métricas ni la traza.

#### Escenario 11: Guardar el recorrido finalizado

Dado que el corredor terminó de consultar los resultados y continúa al flujo de guardado `save-run`,

Cuando completa el formulario de guardado,

Entonces:
- Debe ingresar obligatoriamente un título para el recorrido.
- Puede seleccionar destacados de forma opcional.
- Puede agregar hasta 2 fotos opcionales capturadas con la cámara.
- No se admiten videos.
- Al guardar, el recorrido queda almacenado localmente en Room y disponible en la pestaña "Historial", con su traza visible sobre el mapa.

### Puntos pendientes de revisión con el equipo

- **Representación visual de controles sin recorrido:** Se acordó que los controles de Pausar, Reanudar y Finalizar no permiten acciones mientras no haya un recorrido en curso. Queda pendiente definir su representación visual concreta.
- **Nombres de estados adicionales:** Coordinar con Persona 4 los nombres definitivos para diferenciar la pausa manual, la pausa personal y la frenada detectada automáticamente. No se fijan nombres de estados nuevos hasta alcanzar ese acuerdo.
- **Responsabilidad del aviso de recorrido demasiado corto:** Coordinar con Persona 4 qué componente o pantalla será responsable de presentar el aviso de recorrido inferior a 200 metros.
- **Datos adicionales del resumen:** Coordinar con Persona 4 qué información adicional, como modo demo, frenadas detectadas o regularidad del suelo, debe acompañar al resumen. HU-04 debe entregar los datos que le corresponden y no asumir información que pertenece a otras historias.

Las siguientes decisiones ya están aprobadas y no deben permanecer como pendientes:
- Alcanzar la meta no finaliza automáticamente el recorrido.
- La finalización requiere confirmación para recorridos de al menos 200 metros.
- Los recorridos inferiores a 200 metros no se guardan.
- Después de confirmar la finalización se muestra primero la pantalla de resultados y luego se continúa a `save-run`.
- R9 y R10 están aprobados por el equipo.

---

## Introduction

Esta funcionalidad permite a los corredores seguir su actividad en tiempo real desde la pestaña "Mapa" de Trazo (HU-04). Mientras el corredor se desplaza, la aplicación muestra su ubicación sobre el mapa, traza la ruta en vivo y actualiza las métricas clave: tiempo transcurrido, distancia acumulada y velocidad actual.

La funcionalidad proporciona controles para pausar y reanudar la salida, recentrar la vista del mapa y conservar el estado de la sesión al navegar entre pestañas. El recorrido continúa activo aunque el corredor salga de la pestaña "Mapa"; si está pausado, el temporizador, la distancia y la traza permanecen detenidos.

El recorrido se recibe desde el flujo de inicio `start-run` una vez que hay señal GPS disponible. La solicitud de permisos de ubicación corresponde a dicho flujo y no a `MapScreen`. HU-04 registra la fecha y hora real de inicio al asociar la sesión activa.

La funcionalidad contempla el estado inicial del mapa sin recorrido activo, las métricas en cero, el acceso al inicio mediante el botón flotante FAB "+", la celebración con animación al alcanzar una meta predefinida (3, 5 o 10 km), el filtrado de lecturas GPS imprecisas y la alerta de pérdida de señal tras 15 segundos sin recibir ubicaciones válidas.

Al alcanzar la meta, la aplicación muestra la animación de celebración y registra que la meta fue alcanzada, pero el recorrido continúa activo hasta que el corredor decida finalizarlo.

La finalización de un recorrido de al menos 200 metros requiere confirmación. El mensaje de confirmación depende de si se alcanzó la meta. Los recorridos inferiores a 200 metros muestran un aviso especial que permite descartarlos o volver al recorrido; no se guardan. Al confirmar la finalización de un recorrido válido, HU-04 detiene el seguimiento, congela las métricas y la traza y genera un resumen final. A continuación, se muestra la pantalla de resultados, responsabilidad de Persona 4, y luego se continúa al flujo de guardado `save-run`, responsabilidad de Persona 1.

En alineación con la definición de producto de Trazo v0.6:

- La aplicación funciona de manera 100% local, almacenando los recorridos en una base de datos Room, sin cuentas de usuario, perfiles, comunidad, rankings, seguidores ni servidores o APIs externas.
- Las metas disponibles son exclusivamente fijas (3 km, 5 km y 10 km); no existen metas personalizadas.
- No se registran calorías, planes de entrenamiento ni integración con pulsómetros o dispositivos wearables.
- En el guardado se admiten hasta 2 fotos tomadas con la cámara, sin soporte de video.
- La pantalla se mantiene activa mientras el corredor está en la pestaña "Mapa" durante el seguimiento. No se garantiza seguimiento prolongado en segundo plano y la pantalla puede apagarse al navegar a otras pestañas.
- Todos los textos de la interfaz están redactados en español rioplatense con voseo, sin soporte para múltiples idiomas ni internacionalización.

---

## Boundary Context

### In scope

- Estado inicial de la pestaña "Mapa" sin recorrido en curso: centrado en la ubicación actual si está disponible, métricas en cero, inicio de carrera mediante FAB "+" e inhabilitación funcional de acciones en los controles de Pausar, Reanudar y Finalizar.
- Visualización continua de la posición actual del corredor y trazado en vivo de la ruta sobre el mapa.
- Cálculo y presentación continua de las métricas del recorrido: tiempo transcurrido, distancia recorrida y velocidad actual.
- Registro de la fecha y hora real de inicio de la sesión al asociar el recorrido activo.
- Filtrado de puntos GPS con precisión superior a 30 metros, descartándolos de la traza y del cálculo de distancia.
- Indicador visual de calidad de señal GPS, manejado independientemente del filtro de precisión.
- Detección de pérdida de señal tras 15 segundos consecutivos sin ubicaciones GPS válidas, contabilizados desde la última ubicación válida.
- Controles de pausa y reanudación del recorrido, deteniendo o reanudando el temporizador, la distancia y el registro de nuevos puntos GPS.
- Conservación del estado de la sesión al navegar entre pestañas, manteniendo el recorrido activo o pausado según corresponda.
- Recentrado del mapa sobre la ubicación actual tras desplazamientos manuales del corredor.
- Presentación de la animación de celebración al alcanzar la meta de 3, 5 o 10 km, sin finalizar automáticamente el recorrido.
- Registro de si la meta fue alcanzada para incluirlo en el resumen final.
- Finalización mediante confirmación para recorridos de al menos 200 metros, con mensajes diferentes según se haya alcanzado la meta.
- Aviso especial para recorridos inferiores a 200 metros, con opciones para descartarlos o volver al recorrido.
- Descarte de recorridos inferiores a 200 metros sin guardarlos y limpieza de la sesión activa.
- Detención definitiva del temporizador y del registro GPS al confirmar la finalización.
- Congelamiento de métricas y traza y generación de un resumen final que incluya la fecha y hora real de inicio y el indicador de meta alcanzada.
- Entrega del resumen final a la pantalla de resultados y continuidad posterior hacia `save-run`.
- Pantalla activa mientras el corredor permanece en la pestaña "Mapa", sin exigir que permanezca encendida al navegar a otras pestañas.
- Respeto de la configuración de accesibilidad del dispositivo para reducir o quitar animaciones.

### Out of scope

- Modal y selección de meta de inicio del recorrido (responsabilidad de `start-run`).
- Solicitud inicial de permisos de ubicación desde `MapScreen` (responsabilidad del flujo de inicio).
- Implementación detallada de la pantalla de resultados (responsabilidad de Persona 4).
- Implementación detallada del formulario de guardado (responsabilidad de `save-run`, Persona 1).
- Persistencia definitiva del recorrido en Room desde HU-04; HU-04 entrega el resumen congelado al flujo responsable del guardado.
- Cuentas de usuario, autenticación, perfiles, comunidad, rankings o seguidores.
- Servidor propio, backend externo o sincronización en la nube.
- Metas personalizadas; únicamente se contemplan metas fijas de 3 km, 5 km y 10 km.
- Cálculo de calorías, pulsómetros, wearables y planes de entrenamiento.
- Grabación o adjunto de videos; solo se admiten fotos, con un máximo de 2 en el flujo de guardado.
- Garantía de ejecución y seguimiento continuo en segundo plano durante períodos prolongados.
- Soporte para múltiples idiomas o internacionalización; la aplicación se mantiene exclusivamente en español.
- Implementación de la detección de frenadas bruscas o irregularidades del suelo, que corresponde a historias especializadas de sensores. HU-04 podrá recibir e integrar los datos que se acuerden para el resumen, pero no implementa esa detección.

### Adjacent expectations

- **`start-run` (Persona 2):** solicita los permisos de ubicación necesarios, espera a que haya señal GPS disponible y entrega a HU-04 una sesión iniciada con la meta seleccionada (3 km, 5 km o 10 km). El FAB "+" dispara este flujo cuando no hay un recorrido en curso.
- **HU-04 (Persona 3):** asocia la sesión recibida, registra la fecha y hora real de inicio, mantiene el seguimiento, administra los estados del recorrido, expone el indicador de recorrido en curso y genera el resumen congelado al finalizar.
- **Pantalla de resultados (Persona 4):** recibe el resumen final generado por HU-04 y muestra el resultado correspondiente antes del guardado.
- **`save-run` (Persona 1):** recibe el resumen final, solicita un título obligatorio, permite seleccionar destacados opcionales y hasta 2 fotos capturadas con la cámara, y persiste el recorrido localmente en Room para su consulta desde Historial y el mapa.
- **HU-05:** puede aportar información relacionada con frenadas detectadas y regularidad del suelo. La integración de esos datos en el resumen debe coordinarse con Persona 4.
- **Contrato entre HU-04 y las demás funcionalidades:** HU-04 debe exponer un booleano `isRunInProgress`, que sea verdadero desde que se asocia una sesión activa hasta que se finaliza. Debe permanecer verdadero durante pausas, frenadas y pérdida de señal GPS. Debe volver a falso al finalizar o al descartar una sesión demasiado corta.
- **Estados del recorrido:** se deben diferenciar los estados necesarios para recorrido activo, pausa manual, pausa personal y frenada detectada automáticamente. Los nombres definitivos de los estados adicionales deben coordinarse con Persona 4 antes de cerrar el diseño.

---

## Requirements

### Requirement 1: Estado inicial del mapa y controles sin recorrido

**Objective:** As a corredor, I want ver el mapa listo y centrado en mi ubicación al ingresar sin un recorrido activo, junto con las métricas en cero y la posibilidad de iniciar una salida, so that pueda orientarme y comenzar cuando lo decida sin activar controles involuntarios.

#### Acceptance Criteria

1. When el corredor abre la pestaña "Mapa" sin un recorrido en curso, if los permisos de ubicación y la ubicación actual están disponibles, then the Trazo app shall centrar la vista del mapa en la posición actual del corredor.
2. While no hay un recorrido en curso, the Trazo app shall mostrar las métricas iniciales con el temporizador en "00:00:00", la distancia en "0 km" y la velocidad en "0 km/h".
3. While no hay un recorrido en curso, when el corredor interactúa con el botón flotante (FAB "+"), the Trazo app shall permitir iniciar una carrera siguiendo el flujo de inicio de `start-run`.
4. While no hay un recorrido en curso, the Trazo app shall no permitir acciones al interactuar con los controles de Pausar, Reanudar y Finalizar.
5. While no hay un recorrido en curso, the Trazo app shall mantener pendiente de definición con el equipo la representación visual de habilitación o deshabilitación de los controles de Pausar, Reanudar y Finalizar.
6. The Trazo app shall delegar al flujo `start-run` la solicitud de permisos de ubicación y no solicitar dichos permisos desde `MapScreen`.
7. The Trazo app shall recibir la sesión de recorrido desde `start-run` una vez que haya señal GPS disponible, sin iniciar el seguimiento de HU-04 antes de que se cumpla esa condición.

### Requirement 2: Visualización y métricas del recorrido en vivo

**Objective:** As a corredor, I want ver mi avance, la traza de mi camino y mis métricas en vivo, so that pueda monitorear mi rendimiento en tiempo real.

#### Acceptance Criteria

1. While un recorrido está en curso y activo, when el corredor avanza con ubicaciones GPS válidas, the Trazo app shall mostrar la posición actual del corredor sobre el mapa.
2. While un recorrido está en curso y activo, when el corredor avanza con ubicaciones GPS válidas, the Trazo app shall dibujar y extender la traza continua del recorrido en el mapa.
3. While un recorrido está en curso y activo, the Trazo app shall actualizar continuamente el tiempo transcurrido.
4. While un recorrido está en curso y activo, the Trazo app shall actualizar continuamente la distancia acumulada recorrida.
5. While un recorrido está en curso y activo, the Trazo app shall actualizar continuamente la velocidad actual del corredor.
6. When HU-04 asocia la sesión activa recibida desde `start-run`, the Trazo app shall registrar la fecha y hora real de inicio del recorrido.
7. The Trazo app shall conservar la fecha y hora real de inicio durante toda la sesión y entregarla en el resumen final.

### Requirement 3: Calidad y pérdida de señal GPS

**Objective:** As a corredor, I want saber si la señal GPS es buena o si se interrumpió, so that confíe en la precisión del registro de mi recorrido.

#### Acceptance Criteria

1. While un recorrido está en curso, when la señal GPS tiene calidad suficiente, the Trazo app shall mostrar el indicador "GPS" con barras completas de señal.
2. The Trazo app shall determinar el estado visual de las barras de señal GPS de forma independiente del filtro de precisión de los puntos, sin asumir que una precisión menor o igual a 30 metros determina automáticamente las barras de señal.
3. While un recorrido está en curso, if transcurren 15 segundos consecutivos sin recibir ubicaciones GPS válidas, contabilizados desde la última ubicación GPS válida recibida, then the Trazo app shall mostrar el aviso "Sin señal GPS".
4. While se muestra el aviso "Sin señal GPS", the Trazo app shall no incorporar puntos inválidos a la traza ni sumar distancia recorrida a partir de ellos.
5. When se recibe nuevamente una ubicación GPS válida tras haber mostrado "Sin señal GPS", the Trazo app shall ocultar el aviso y continuar el registro del recorrido.
6. The Trazo app shall iniciar el seguimiento de HU-04 únicamente después de que `start-run` haya entregado una sesión con señal GPS disponible. El período previo a la asociación de la sesión no debe contabilizarse como pérdida de señal de un recorrido en curso.

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
2. When el recorrido pasa a estado de pausa manual, the Trazo app shall cambiar el control de "Pausar" a "Reanudar".
3. While el recorrido está pausado manualmente, when el corredor toca el control "Reanudar", the Trazo app shall continuar el conteo del temporizador, la acumulación de distancia y el registro de nuevos puntos GPS en la traza.
4. When el recorrido se reanuda, the Trazo app shall cambiar el control de "Reanudar" a "Pausar".
5. The Trazo app shall permitir diferenciar la pausa manual solicitada por el corredor, la pausa personal y la frenada detectada automáticamente, sin confundirlas como un único estado. Los nombres definitivos de los estados adicionales deben acordarse con Persona 4 antes de cerrar el diseño.

### Requirement 6: Recentrado de la vista del mapa

**Objective:** As a corredor, I want volver a enfocar mi posición actual tras haber explorado el mapa, so that no pierda de vista mi ubicación en carrera.

#### Acceptance Criteria

1. While hay un recorrido en curso y el corredor ha desplazado manualmente el mapa, when el corredor toca el botón de ubicación actual, the Trazo app shall volver a centrar la vista del mapa en la posición actual del corredor.
2. When el mapa se recentra en la posición actual, the Trazo app shall mantener visible la traza completa acumulada del recorrido.

### Requirement 7: Persistencia del estado durante la navegación entre pestañas

**Objective:** As a corredor, I want cambiar de pestaña sin que se interrumpa mi recorrido, so that pueda consultar otras secciones de la app con tranquilidad.

#### Acceptance Criteria

1. While un recorrido está en curso, when el corredor cambia de la pestaña "Mapa" a otra pestaña, the Trazo app shall mantener el recorrido en curso conservando su estado actual (activo o pausado).
2. While el recorrido está activo durante la navegación fuera de la pestaña "Mapa", the Trazo app shall continuar incrementando el temporizador y acumulando la distancia y la traza.
3. While el recorrido está pausado durante la navegación fuera de la pestaña "Mapa", the Trazo app shall mantener detenidos el temporizador, la distancia y el registro de la traza, conservando el estado pausado sin avanzar métricas.
4. When el corredor vuelve a la pestaña "Mapa", the Trazo app shall restaurar el estado correspondiente (activo o pausado), mostrando las métricas actualizadas y la traza consolidada.
5. While el corredor permanece en la pestaña "Mapa" con un recorrido en curso, the Trazo app shall mantener la pantalla activa para evitar la suspensión del dispositivo.
6. While el corredor permanece en otra pestaña, the Trazo app shall permitir que la pantalla se apague. No se garantiza el seguimiento continuo en segundo plano durante períodos prolongados.

### Requirement 8: Meta alcanzada y animación de celebración

**Objective:** As a corredor, I want que la aplicación reconozca visualmente cuando alcancé la meta de mi salida, so that pueda celebrar mi logro sin interrumpir abruptamente mi experiencia.

#### Acceptance Criteria

1. While un recorrido con meta (3 km, 5 km o 10 km) está en curso y activo, when la distancia acumulada alcanza la distancia de la meta fijada, the Trazo app shall mostrar una animación de celebración de meta cumplida.
2. While se muestra la animación de celebración de meta alcanzada, the Trazo app shall mantener el recorrido en curso y permitir que el corredor continúe corriendo hasta que decida finalizarlo explícitamente.
3. When se alcanza la meta fijada, the Trazo app shall registrar que la meta fue alcanzada para incluir esa información en el resumen final del recorrido.
4. The Trazo app shall respetar la configuración de accesibilidad del dispositivo para reducir o quitar animaciones, sin impedir que se registre el cumplimiento de la meta.

### Requirement 9: Finalización del recorrido

**Objective:** As a corredor, I want poder finalizar mi recorrido de forma segura mediante confirmación y congelar mis métricas, so that no pierda datos accidentalmente y pueda consultar los resultados antes de guardar mi salida.

#### Acceptance Criteria

1. While un recorrido está en curso (activo o pausado), when el corredor toca el control "Finalizar", the Trazo app shall:
    - Si la distancia recorrida es inferior a 200 metros, mostrar un aviso indicando que el recorrido es demasiado corto para guardarse, ofreciendo las opciones de descartarlo o volver.
    - Si la distancia recorrida es igual o superior a 200 metros y la meta fue alcanzada, mostrar un diálogo de confirmación con el mensaje "¿Estás seguro de que deseás finalizar?".
    - Si la distancia recorrida es igual o superior a 200 metros y la meta no fue alcanzada, mostrar un diálogo de confirmación con el mensaje "Todavía no llegaste a tu meta. ¿Estás seguro de que deseás finalizar?".

2. While se muestra el diálogo de confirmación de finalización, when el corredor cancela la acción, the Trazo app shall cerrar el diálogo y conservar el estado previo del recorrido (activo o pausado), sin modificar las métricas ni la traza.

3. While se muestra el diálogo de confirmación de finalización, when el corredor confirma la finalización, the Trazo app shall detener definitivamente el temporizador y el registro GPS, congelar las métricas finales y la traza, y generar el resumen final del recorrido.

4. When la finalización es confirmada para un recorrido de al menos 200 metros, the Trazo app shall mostrar primero una pantalla de resultados antes de continuar al flujo de guardado `save-run`. La pantalla de resultados corresponde a Persona 4.

5. The Trazo app shall incluir en el resumen final la fecha y hora real de inicio del recorrido y un indicador de si la meta fue alcanzada.

6. While se muestra el aviso de recorrido demasiado corto, when el corredor elige descartarlo, the Trazo app shall detener el seguimiento, descartar la sesión sin guardarla, limpiar el recorrido en curso y volver a la pestaña "Mapa" sin una sesión activa.

7. While se muestra el aviso de recorrido demasiado corto, when el corredor elige volver, the Trazo app shall cerrar el aviso y conservar el recorrido en su estado previo (activo o pausado), sin modificar las métricas ni la traza.

### Requirement 10: Integración con el guardado y consulta local del recorrido

**Objective:** As a corredor, I want que mi recorrido finalizado pueda ser guardado con un título y elementos opcionales para consultarlo en mi historial y en el mapa, so that mantenga un registro permanente y confiable de mis salidas sin requerir cuentas externas ni conexión a internet.

#### Acceptance Criteria

1. When el corredor continúa desde la pantalla de resultados al flujo de guardado del recorrido finalizado (`save-run`), the Trazo app shall requerir obligatoriamente el ingreso de un título para permitir guardar la actividad.
2. Where el corredor se encuentra en el flujo de guardado del recorrido (`save-run`), the Trazo app shall permitir seleccionar destacados de forma opcional.
3. Where el corredor se encuentra en el flujo de guardado del recorrido (`save-run`), the Trazo app shall permitir adjuntar hasta dos fotos opcionales tomadas con la cámara, sin admitir grabación ni adjunto de videos.
4. When el corredor confirma el guardado del recorrido (`save-run`), the Trazo app shall almacenar el recorrido con sus métricas finales, traza, título, destacados y fotos de forma estrictamente local en Room, sin enviar datos a servidores externos ni requerir cuentas de usuario.
5. When un recorrido queda almacenado localmente en Room, the Trazo app shall dejarlo disponible en la pestaña "Historial" y mostrar su traza correspondiente en el mapa.
6. HU-04 shall entregar al flujo `save-run` el resumen congelado del recorrido, sin implementar el formulario de guardado ni asumir la responsabilidad de persistirlo en Room.

### Requirement 11: Alcance de plataforma, restricciones de hardware y accesibilidad

**Objective:** As a corredor, I want una interfaz en mi idioma, accesible, clara y adaptada a las capacidades del dispositivo, so that pueda utilizar la app cómodamente durante la actividad física respetando los límites de diseño de Trazo v0.6.

#### Acceptance Criteria

1. The Trazo app shall mostrar todos los textos de esta funcionalidad exclusivamente en idioma español rioplatense con voseo, sin incorporar soporte para múltiples idiomas ni internacionalización.
2. The Trazo app shall admitir únicamente metas predefinidas de 3 km, 5 km y 10 km, sin permitir configuración de metas personalizadas ni cálculo de calorías, planes de entrenamiento ni integración con pulsómetros o dispositivos wearables.
3. While un recorrido está en curso y el corredor permanece en la pestaña "Mapa", the Trazo app shall mantener la pantalla activa para evitar la suspensión del dispositivo. Al navegar a otras pestañas, no se garantiza que la pantalla permanezca encendida ni el seguimiento continuo en segundo plano durante períodos prolongados.
4. The Trazo app shall mostrar las métricas de distancia y velocidad utilizando la coma como separador decimal.
5. The Trazo app shall asegurar que todos los controles táctiles del mapa y del panel de recorrido tengan un área táctil mínima de 48 × 48 dp (y 56 × 56 dp para los controles de Pausar, Reanudar y Finalizar).
6. The Trazo app shall proveer descripciones accesibles para lectores de pantalla en los botones de control de recorrido, el botón flotante de inicio, el botón de recentrado y los avisos de estado de señal GPS.
7. The Trazo app shall respetar la configuración de accesibilidad del dispositivo para reducir o quitar animaciones de celebración, utilizando el mecanismo de accesibilidad acordado para la aplicación.