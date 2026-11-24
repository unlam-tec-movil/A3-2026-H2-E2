# Requirements Document

> **Revisión 2026-11-24 · nueva UI de Perfil.** Esta nota manda sobre lo que se contradiga más
> abajo.
>
> - «Mis estadísticas» es una card con tres columnas: una barra que crece hacia arriba (con
>   animación y un tope chico), el valor y la etiqueta. Cada barra se llena contra un tope fijo:
>   20 recorridos, 50 km y 10 h; al superarlo queda llena. Los demo no suman.
> - «Recorridos guardados» muestra solo el número («0», «2»), sin la palabra.
> - El estado vacío es una card «Mis recorridos» con un ícono de corredor en círculos, «Todavía no
>   hay recorridos» y «Acá vas a ver la lista de tus recorridos finalizados cuando los guardes.
>   Andá a Actividad para empezar uno.», en lugar de la ilustración.

## Project Description (Input)
profile-runs: Mis recorridos y detalle (HU-09), Mis estadísticas (HU-16) y trazas guardadas en el mapa de Actividad (HU-12), según Trazo v0.7. Mis recorridos: tarjetas del más reciente al más antiguo, con título (con puntos suspensivos si es largo), fecha, km, duración, frenadas, regularidad y marca Incompleto o Demo. Detalle en modal: mapa con la traza, título, velocidad promedio, meta y estado, destacados y fotos (máx. 2), marca Demo; si no hay destacados o fotos, esas secciones se ocultan. Sin recorridos: ilustración con «Todavía no hay recorridos» y totales en cero. Mis estadísticas: recorridos, km y tiempo totales; suman los incompletos, no suman los demo, y el tiempo no incluye pausas personales. HU-12: con recorridos guardados, sin recorrido en curso y con ubicación precisa, el mapa de Actividad dibuja todas las trazas; al tocar una se abre el modal de detalle. No nombrar clases del contrato: hablar de «recorrido guardado».

## Introduction
Los corredores de Trazo guardan sus recorridos, pero hoy no tienen un lugar donde revisarlos ni entender cuánto corrieron en total. Esta funcionalidad completa la pestaña Perfil con «Mis estadísticas» (totales) y «Mis recorridos» (lista de recorridos guardados con su detalle en un modal), y hace que el mapa de Actividad muestre las trazas de esos recorridos para que el corredor las vea sobre la ciudad y abra su detalle desde ahí.

## Boundary Context
- **In scope**:
  - «Mis recorridos» en Perfil: lista de cards, estado vacío y orden.
  - El modal de detalle de un recorrido guardado, con su mapa, métricas, destacados y fotos.
  - «Mis estadísticas» en Perfil: recorridos, distancia y tiempo totales, y qué recorridos suman.
  - Las trazas de los recorridos guardados en el mapa de Actividad y la apertura del detalle al tocarlas.
- **Out of scope**:
  - Guardar un recorrido, elegir su título, sus destacados y sus fotos (lo define `save-run`).
  - Editar o borrar un recorrido guardado.
  - Medir el recorrido, detectar frenadas y calcular la regularidad.
  - La barra superior de Perfil y el botón de Configuración.
  - Compartir, exportar o sincronizar recorridos.
  - Guardar, escribir o conservar recorridos: esta funcionalidad solo los lee. Quien los escribe y los conserva es `save-run` (HU-07).
- **Adjacent expectations**:
  - Cada recorrido guardado trae: título, fecha y hora de inicio, meta (3, 5 o 10 km), estado (meta cumplida o incompleto), marca de modo demo, distancia, tiempo, frenadas, regularidad, traza, destacados y, si el corredor las agregó, hasta 2 fotos. Esta funcionalidad no calcula ni modifica esos datos.
  - El tiempo de cada recorrido guardado ya excluye las pausas personales; esta funcionalidad lo usa tal como viene.
  - Conservar los recorridos al cerrar y volver a abrir la app (criterio 1.8) es una dependencia de `save-run`: se cumple cuando el almacenamiento persistente de ese equipo reemplace al almacenamiento en memoria actual. Esta funcionalidad no implementa persistencia ni tiene tareas para ella.
  - Que el corredor pueda agregar fotos al guardar lo define `save-run`. Mientras un recorrido guardado no tenga fotos, el detalle las oculta.
  - Esta funcionalidad reemplaza la lista y el detalle que `save-run` ubicaba en el Historial: esa lista ahora vive en «Mis recorridos», dentro de Perfil.
  - Que haya un recorrido en curso y si la ubicación es precisa lo informa el seguimiento del recorrido y el permiso de ubicación del teléfono; esta funcionalidad solo reacciona a ese estado.
  - Los textos hablan de «recorrido guardado» y no nombran clases ni tipos del código.

## Prioridad
Orden de entrega, de mayor a menor prioridad:
1. HU-09: «Mis recorridos» y el detalle (Requisitos 1, 2 y 3).
2. HU-16: «Mis estadísticas» (Requisito 4).
3. HU-12, dibujo de las trazas guardadas en el mapa de Actividad (Requisito 5, criterios 1 y 3 a 7).
4. HU-12, abrir el detalle tocando la traza (Requisito 5, criterios 2 y 9). **Es lo último y se puede recortar** sin afectar al resto; el criterio 8 garantiza que el detalle siga accesible desde «Mis recorridos».

El Requisito 6 (idioma, formato y accesibilidad) aplica a todo lo que se entregue.

## Requirements

### Requirement 1: Lista de «Mis recorridos»
**Objective:** As a corredor, I want ver mis recorridos guardados en Perfil, so that revise y compare mi actividad.

#### Acceptance Criteria
1. The Trazo App shall mostrar en la sección «Mis recorridos» de Perfil cada recorrido guardado como una card.
2. The Trazo App shall ordenar las cards del recorrido más reciente al más antiguo.
3. The Trazo App shall mostrar en cada card el título, la fecha, la distancia en km, la duración, las frenadas y la regularidad del recorrido.
4. If el título de un recorrido no entra en una línea de la card, then the Trazo App shall mostrarlo en una sola línea y terminarlo con puntos suspensivos.
5. If un recorrido guardado es incompleto, then the Trazo App shall mostrar en su card la marca «Incompleto».
6. If un recorrido guardado se hizo en modo demo, then the Trazo App shall mostrar en su card la marca «Demo».
7. When se guarda un recorrido nuevo, the Trazo App shall mostrarlo primero en «Mis recorridos».
8. Where el almacenamiento de recorridos guardados es persistente (dependencia de `save-run`), the Trazo App shall mostrar los recorridos guardados después de cerrar y volver a abrir la app.

### Requirement 2: Estado vacío de «Mis recorridos»
**Objective:** As a corredor que todavía no guardó recorridos, I want un mensaje claro en lugar de una lista vacía, so that entienda que ahí van a aparecer mis recorridos.

#### Acceptance Criteria
1. While no hay ningún recorrido guardado, the Trazo App shall mostrar en «Mis recorridos» una ilustración con el mensaje «Todavía no hay recorridos».
2. While no hay ningún recorrido guardado, the Trazo App shall no mostrar cards de recorridos.
3. While no hay ningún recorrido guardado, the Trazo App shall mostrar en «Mis estadísticas» los totales de recorridos, distancia y tiempo en cero.
4. The Trazo App shall comunicar el estado vacío con texto que un lector de pantalla pueda leer, y no solo con la ilustración.
5. When se guarda el primer recorrido, the Trazo App shall reemplazar el estado vacío por la card del recorrido y actualizar los totales.

### Requirement 3: Detalle de un recorrido guardado
**Objective:** As a corredor, I want abrir un recorrido guardado y ver todo lo que guardé de él, so that lo recuerde y lo revise con calma.

#### Acceptance Criteria
1. When el corredor toca una card de «Mis recorridos», the Trazo App shall abrir un modal con el detalle de ese recorrido.
2. The Trazo App shall mostrar en el detalle un mapa con la traza completa del recorrido, su título, su velocidad promedio, su meta y su estado (meta cumplida o incompleto).
3. If el recorrido guardado tiene destacados, then the Trazo App shall mostrarlos en el detalle.
4. If el recorrido guardado no tiene destacados, then the Trazo App shall ocultar la sección de destacados del detalle.
5. If el recorrido guardado tiene fotos, then the Trazo App shall mostrar en el detalle hasta 2 fotos.
6. If el recorrido guardado no tiene fotos, then the Trazo App shall ocultar la sección de fotos del detalle.
7. If el recorrido guardado se hizo en modo demo, then the Trazo App shall mostrar la marca «Demo» en el detalle.
8. When el corredor cierra el modal de detalle, the Trazo App shall volver a la pantalla desde la que lo abrió, en el mismo lugar.
9. If la traza de un recorrido guardado no se puede mostrar en el mapa, then the Trazo App shall mostrar igualmente el resto del detalle.

### Requirement 4: «Mis estadísticas»
**Objective:** As a corredor, I want ver cuánto corrí en total, so that conozca mi progreso.

#### Acceptance Criteria
1. The Trazo App shall mostrar en «Mis estadísticas» el total de recorridos, el total de kilómetros y el tiempo total.
2. The Trazo App shall sumar a los totales los recorridos guardados incompletos.
3. The Trazo App shall no sumar a los totales los recorridos guardados en modo demo, sean completos o incompletos.
4. The Trazo App shall calcular el tiempo total sin las pausas personales de cada recorrido.
5. When se guarda un recorrido nuevo, the Trazo App shall actualizar los totales de «Mis estadísticas».
6. The Trazo App shall mostrar los kilómetros con coma decimal (por ejemplo, «58,6 km»).

### Requirement 5: Trazas guardadas en el mapa de Actividad
**Objective:** As a corredor, I want ver en el mapa de Actividad las trazas de mis recorridos guardados, so that vea por dónde corrí y abra su detalle desde ahí.

#### Acceptance Criteria
1. While hay al menos un recorrido guardado, no hay un recorrido en curso y la ubicación del teléfono es precisa, the Trazo App shall dibujar en el mapa de Actividad la traza de todos los recorridos guardados.
2. When el corredor toca una traza guardada en el mapa de Actividad, the Trazo App shall abrir el modal de detalle de ese recorrido. _(Recortable: ver Prioridad.)_
3. While hay un recorrido en curso, the Trazo App shall no dibujar en el mapa de Actividad las trazas guardadas.
4. While la ubicación del teléfono no es precisa, the Trazo App shall no dibujar en el mapa de Actividad las trazas guardadas.
5. While no hay recorridos guardados, the Trazo App shall no dibujar trazas guardadas en el mapa de Actividad.
6. When termina el recorrido en curso y se guarda, the Trazo App shall dibujar su traza junto con las demás en el mapa de Actividad, siempre que se cumplan las condiciones del criterio 1.
7. When el corredor cierra el modal de detalle abierto desde el mapa, the Trazo App shall volver al mapa de Actividad sin perder las trazas dibujadas.
8. The Trazo App shall dar acceso al detalle de cada recorrido guardado desde «Mis recorridos», para que no dependa de tocar la traza en el mapa.
9. When el corredor toca un punto del mapa de Actividad donde coinciden varias trazas guardadas, the Trazo App shall abrir el modal de detalle del recorrido más reciente entre ellas. _(Recortable: ver Prioridad.)_

### Requirement 6: Idioma, formato y accesibilidad
**Objective:** As a corredor, I want que los textos y los controles sean claros y accesibles, so that pueda usar la funcionalidad cómodamente, también con lector de pantalla.

#### Acceptance Criteria
1. The Trazo App shall mostrar todos los textos de esta funcionalidad en español rioplatense con voseo.
2. The Trazo App shall mostrar los números decimales con coma.
3. The Trazo App shall dar a cada control tocable de esta funcionalidad un área táctil de al menos 48 × 48 dp.
4. The Trazo App shall comunicar las marcas «Incompleto» y «Demo» con texto, y no solo con color.
5. The Trazo App shall dar a cada card de «Mis recorridos» una descripción accesible que incluya el título, la fecha, la distancia, la duración, las frenadas, la regularidad y sus marcas.
6. The Trazo App shall dar al mapa de la traza del detalle una descripción accesible que resuma el recorrido, porque el mapa no es legible con un lector de pantalla.
7. While el tamaño de fuente del sistema está al 200 %, the Trazo App shall mantener legibles y operables los textos y controles de esta funcionalidad.
8. While el corredor tiene activada la opción «Quitar animaciones», the Trazo App shall abrir y cerrar el detalle sin animaciones de movimiento.
