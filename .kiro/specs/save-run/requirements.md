# Requirements Document

## Project Description (Input)
Los corredores que terminan un recorrido hoy no pueden guardarlo: la pantalla de resultado solo muestra las métricas y se pierden al salir. Quiero que puedan guardarlo con un título (de 3 a 50 caracteres) y elegir destacados, para verlo después en el Historial.

## Introduction
Hoy, cuando un corredor termina un recorrido en Trazo, la pantalla de resultado ("¡Meta cumplida!" o "Recorrido finalizado") muestra sus métricas, pero se pierden al salir. Esta funcionalidad agrega la pantalla "Guardar recorrido". Ahí el corredor le pone un título, elige destacados (predefinidos o propios) y guarda el recorrido. Los recorridos guardados se ven en el Historial como una lista de cards, y al tocar una se abre su detalle.

## Boundary Context
- **In scope**:
  - La pantalla "Guardar recorrido": el resumen, el título, los destacados predefinidos y personalizados, el guardado y el diálogo de descarte.
  - La conservación de los recorridos guardados.
  - La lista del Historial con su estado vacío.
  - El modal de detalle de un recorrido guardado.
- **Out of scope**:
  - Las fotos del recorrido.
  - Editar o borrar un recorrido ya guardado.
  - La "Actividad reciente" de Inicio.
  - Las trazas guardadas en la pestaña Mapa.
  - La sincronización o el respaldo en la nube.
  - La medición del recorrido y la pantalla de resultado en sí.
- **Adjacent expectations**:
  - La funcionalidad de registro de recorridos entrega un recorrido finalizado con estos datos: fecha y hora de inicio, meta elegida (3, 5 o 10 km), si la meta se cumplió, si se hizo en modo demo, distancia, tiempo, frenadas, regularidad del piso y traza.
  - La pantalla de resultado ofrece el botón "Continuar", que lleva a "Guardar recorrido".
  - Esta funcionalidad no calcula ni modifica esas métricas.

## Requirements

### Requirement 1: Acceso a "Guardar recorrido" y resumen del recorrido
**Objective:** As a corredor, I want ver un resumen del recorrido que acabo de terminar antes de guardarlo, so that sepa qué estoy guardando.

#### Acceptance Criteria
1. When el corredor toca "Continuar" en la pantalla de resultado, the Trazo App shall mostrar la pantalla "Guardar recorrido" con una barra superior que tiene el título "Guardar recorrido" y un botón para volver.
2. The Trazo App shall mostrar en "Guardar recorrido" una card de resumen con la fecha y la hora del recorrido, su marca de estado y las métricas Distancia, Tiempo y Frenadas.
3. When el recorrido no alcanzó la meta elegida, the Trazo App shall mostrar la marca "Incompleto" en la card de resumen.
4. When el recorrido alcanzó la meta elegida, the Trazo App shall mostrar la marca "Meta cumplida" en la card de resumen.
5. When el recorrido se hizo en modo demo, the Trazo App shall mostrar además la marca "Demo" en la card de resumen.
6. The Trazo App shall permitir guardar cualquier recorrido finalizado, haya cumplido o no la meta y sea real o de modo demo.

### Requirement 2: Título del recorrido
**Objective:** As a corredor, I want ponerle un título a mi recorrido, so that lo reconozca después en el Historial.

#### Acceptance Criteria
1. The Trazo App shall mostrar un campo "Título del recorrido" vacío, con la ayuda "Obligatorio" y un contador de caracteres con el formato "N/50".
2. While el título tiene 50 caracteres, the Trazo App shall bloquear el ingreso de más caracteres.
3. If el corredor toca "Guardar recorrido" y el título tiene menos de 3 caracteres sin contar los espacios al inicio y al final, then the Trazo App shall no guardar el recorrido y shall mostrar el campo en estado de error con el mensaje "Escribí un título".
4. When el corredor corrige el título hasta que tenga entre 3 y 50 caracteres, the Trazo App shall quitar el estado de error del campo.
5. When el recorrido se guarda, the Trazo App shall guardar el título sin los espacios al inicio y al final.

### Requirement 3: Destacados predefinidos
**Objective:** As a corredor, I want marcar qué tuvo de bueno el recorrido, so that lo recuerde y compare con otros.

#### Acceptance Criteria
1. The Trazo App shall mostrar la sección opcional "¿Qué destacarías del recorrido?" con los destacados "Piso parejo", "Seguro", "Iluminado" y "Con sombra", todos sin seleccionar al abrir la pantalla.
2. When el corredor toca un destacado sin seleccionar, the Trazo App shall marcarlo como seleccionado con un check.
3. When el corredor toca un destacado seleccionado, the Trazo App shall quitarle la selección.
4. The Trazo App shall permitir seleccionar varios destacados a la vez o ninguno.
5. When el corredor guarda sin seleccionar destacados, the Trazo App shall guardar el recorrido sin destacados.

### Requirement 4: Destacados personalizados
**Objective:** As a corredor, I want agregar un destacado propio, so that describa algo que no está entre los predefinidos.

#### Acceptance Criteria
1. The Trazo App shall mostrar el chip "Agregar" al final de los destacados.
2. When el corredor toca "Agregar", the Trazo App shall pedirle el texto del destacado nuevo.
3. When el corredor confirma un texto de 1 a 20 caracteres, sin contar los espacios al inicio y al final, que no coincide con otro destacado de la pantalla, the Trazo App shall agregarlo como chip nuevo, ya seleccionado.
4. If el texto está vacío o tiene solo espacios, then the Trazo App shall no agregar el destacado y shall indicar que hay que escribir un texto.
5. If el texto coincide con un destacado existente sin distinguir mayúsculas de minúsculas, then the Trazo App shall no agregar el destacado y shall indicar que ese destacado ya existe.
6. While el texto del destacado nuevo tiene 20 caracteres, the Trazo App shall bloquear el ingreso de más caracteres.
7. When el corredor cancela el ingreso del destacado nuevo, the Trazo App shall cerrar el ingreso sin agregar ningún chip.
8. The Trazo App shall permitir seleccionar y deseleccionar los destacados personalizados igual que los predefinidos, sin límite de cantidad.
9. The Trazo App shall ofrecer cada destacado personalizado solo en el recorrido donde se creó.

### Requirement 5: Guardar el recorrido
**Objective:** As a corredor, I want guardar el recorrido con su título y destacados, so that no pierda sus métricas al salir.

#### Acceptance Criteria
1. The Trazo App shall mostrar el botón "Guardar recorrido" fijo en la parte inferior de la pantalla, como única acción principal.
2. When el corredor toca "Guardar recorrido" con un título válido, the Trazo App shall guardar el recorrido con su título, sus destacados seleccionados, sus métricas, su meta, su estado (meta cumplida o incompleto), la marca de modo demo y su traza.
3. When el recorrido se guarda, the Trazo App shall llevar al corredor al Historial y mostrar el aviso "Recorrido guardado.".
4. The Trazo App shall conservar los recorridos guardados al cerrar y volver a abrir la app.
5. The Trazo App shall guardar cada recorrido finalizado una sola vez, aunque el corredor toque "Guardar recorrido" varias veces seguidas.
6. If el guardado falla, then the Trazo App shall mantener al corredor en "Guardar recorrido" con los datos que ingresó y shall mostrar un aviso de error que le permita volver a intentarlo.

### Requirement 6: Salir sin guardar
**Objective:** As a corredor, I want que la app me avise antes de perder un recorrido sin guardar, so that no lo descarte por error.

#### Acceptance Criteria
1. When el corredor toca el botón de volver o usa el gesto de volver del sistema en "Guardar recorrido", the Trazo App shall mostrar el diálogo "¿Descartar el recorrido?" con las opciones "Descartar" y "Seguir editando".
2. When el corredor elige "Seguir editando", the Trazo App shall cerrar el diálogo y conservar el título y los destacados ingresados.
3. When el corredor elige "Descartar", the Trazo App shall descartar el recorrido, salir de "Guardar recorrido" y no agregarlo al Historial.

### Requirement 7: Lista del Historial
**Objective:** As a corredor, I want ver mis recorridos guardados en el Historial, so that revise mi actividad.

#### Acceptance Criteria
1. The Trazo App shall mostrar en el Historial los recorridos guardados como cards, ordenados del más reciente al más antiguo.
2. The Trazo App shall mostrar en cada card el título en una sola línea (con "…" si no entra), la fecha, la hora y la meta (por ejemplo, "Mar 28 oct · 07:45 · Meta 10 km") y las métricas Distancia, Tiempo, Frenadas y Regularidad.
3. If el recorrido es incompleto o de modo demo, then the Trazo App shall mostrar en la card la marca "Incompleto" o "Demo" según corresponda.
4. The Trazo App shall mostrar en la barra superior del Historial un resumen con la cantidad de recorridos y la distancia total (por ejemplo, "12 recorridos · 58,6 km").
5. While no hay recorridos guardados, the Trazo App shall mostrar en el Historial el estado vacío "Todavía no hay recorridos".
6. When se guarda un recorrido nuevo, the Trazo App shall mostrarlo primero en la lista del Historial y actualizar el resumen.

### Requirement 8: Detalle de un recorrido guardado
**Objective:** As a corredor, I want abrir un recorrido del Historial, so that vea todo lo que guardé de él.

#### Acceptance Criteria
1. When el corredor toca una card del Historial, the Trazo App shall abrir un modal de detalle de ese recorrido.
2. The Trazo App shall mostrar en el detalle la traza completa sobre un mapa, el título, las métricas Distancia, Tiempo, Frenadas y Regularidad, la meta y el estado (meta cumplida o incompleto).
3. If el recorrido tiene destacados, then the Trazo App shall mostrarlos en el detalle.
4. If el recorrido no tiene destacados, then the Trazo App shall ocultar la sección de destacados del detalle.
5. If el recorrido se hizo en modo demo, then the Trazo App shall mostrar la marca "Demo" en el detalle.
6. When el corredor cierra el modal de detalle, the Trazo App shall volver al Historial en la misma posición de la lista.

### Requirement 9: Idioma, formato y accesibilidad
**Objective:** As a corredor, I want que los textos y los controles sean claros y accesibles, so that pueda usar la funcionalidad cómodamente, también con lector de pantalla.

#### Acceptance Criteria
1. The Trazo App shall mostrar todos los textos de esta funcionalidad en español rioplatense con voseo.
2. The Trazo App shall mostrar los números decimales con coma (por ejemplo, "5,2 km").
3. The Trazo App shall dar a cada control tocable de esta funcionalidad un área táctil de al menos 48 × 48 dp.
4. The Trazo App shall anunciar con el lector de pantalla el nombre de cada destacado y si está seleccionado o no.
5. When el campo de título pasa a estado de error, the Trazo App shall anunciar el mensaje "Escribí un título" con el lector de pantalla.
6. When aparece el aviso "Recorrido guardado.", the Trazo App shall anunciarlo con el lector de pantalla sin interrumpir lo que se está leyendo.
7. While el tamaño de fuente del sistema está al 200 %, the Trazo App shall mantener legibles y operables todos los textos y controles de esta funcionalidad.
