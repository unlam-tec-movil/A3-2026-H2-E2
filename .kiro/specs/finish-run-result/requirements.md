
# Requirements Document

## Project Description (Input)
HU-06 · Finalizar y celebrar

**Quién:** corredor que está registrando un recorrido con una meta de distancia seleccionada.

**Situación actual:** el recorrido se registra, pero la app no avisa cuándo se alcanzó la meta, no muestra un cierre con los resultados antes de guardar y no evita guardar recorridos demasiado cortos.

**Qué debe cambiar:** al llegar a la meta o al finalizar manualmente, la app debe detener el registro, reconocer el logro (o avisar que todavía no se llegó a la meta) y mostrar un resumen del recorrido antes de pasar al guardado.

**Historia de usuario:** Como corredor que está registrando un recorrido, quiero recibir una confirmación cuando alcance la meta seleccionada y visualizar un resumen al finalizar, para reconocer mi logro y conocer los resultados de mi salida.

(Esta spec cubre el aviso de meta alcanzada, la confirmación al finalizar, la pantalla de resultado y el paso a «Guardar recorrido». El formulario de guardado es HU-07 y el cálculo de frenadas y regularidad es HU-05.)

**Escenarios a cubrir:**
1. Notificar que se alcanzó la meta: con meta de 5 km, al llegar a 5 km se muestra un aviso animado breve en el mapa indicando que se alcanzó la meta, con un botón «Finalizar», y el recorrido deja de registrarse.
2. Finalizar con la meta cumplida: al tocar «Finalizar» se muestra la pantalla «¡Meta cumplida!» con una animación, con distancia, tiempo y regularidad, y el botón «Continuar» para avanzar al guardado.
3. Finalizar antes de alcanzar la meta: con meta de 5 km y 3 km recorridos, al tocar finalizar se pide confirmación con el mensaje «Todavía no llegaste a tu meta» y las opciones «Finalizar recorrido» y «Cancelar». Al confirmar, se detiene el registro y se muestra la pantalla «Recorrido finalizado» sin animación, con los resultados y el botón «Continuar».
4. Intentar finalizar un recorrido demasiado corto: con menos de 200 metros, al tocar «Finalizar» se muestra un aviso de que el recorrido es demasiado corto para guardarlo, se ofrece descartarlo y se permite volver al recorrido sin perder los datos registrados. Al confirmar el descarte, el recorrido no se incorpora al historial.
5. Continuar al guardado: desde la pantalla de resultado, al tocar «Continuar» se abre «Guardar recorrido», conservando distancia, tiempo, velocidad promedio, frenadas, regularidad y meta seleccionada, y el corredor puede ingresar el título obligatorio y agregar destacados y fotografías opcionales.

## Introduction
Hoy, cuando un corredor registra un recorrido con una meta de distancia en Trazo, la app no le avisa cuándo la alcanzó, no le muestra un cierre con los resultados y no evita guardar recorridos demasiado cortos. Esta funcionalidad agrega el cierre del recorrido: el aviso de meta alcanzada, la confirmación al finalizar antes de la meta, el aviso de recorrido demasiado corto y la pantalla de resultado («¡Meta cumplida!» o «Recorrido finalizado»), que termina en el botón «Continuar» hacia «Guardar recorrido».

## Boundary Context
- **In scope**:
  - El aviso de meta alcanzada en el mapa y la detención del registro al alcanzarla.
  - La acción «Finalizar» y sus tres desenlaces: meta cumplida, meta no cumplida y recorrido demasiado corto.
  - La pantalla de resultado, con y sin animación.
  - El paso a «Guardar recorrido» con los datos del recorrido conservados.
- **Out of scope**:
  - El formulario de guardado: título, destacados y fotografías (HU-07 y la spec `save-run`).
  - El cálculo de frenadas y de regularidad (HU-05).
  - La elección de la meta y el inicio, la pausa y la medición del recorrido.
  - El Historial y la lista de recorridos guardados.
- **Adjacent expectations**:
  - El registro del recorrido entrega en todo momento la distancia, el tiempo, la velocidad promedio, las frenadas, la regularidad y la meta elegida (3, 5 o 10 km). Esta funcionalidad los muestra y los conserva, pero no los calcula ni los modifica.
  - La pantalla «Guardar recorrido» recibe el recorrido finalizado desde esta funcionalidad. Los datos que ya espera la spec `save-run` (fecha y hora de inicio, modo demo y traza) también se conservan.
  - La fecha (día del recorrido) y la hora de inicio las registra el sistema al comenzar el recorrido y se especifican en el escenario 1 de HU-02/HU-03, a cargo de otra persona. El modo demo sigue pendiente de definir por el equipo.

## Requirements

### Requirement 1: Aviso de meta alcanzada
**Objective:** As a corredor, I want que la app me avise cuando llegué a mi meta, so that sepa que la cumplí sin tener que mirar la distancia.

#### Acceptance Criteria
1. When la distancia recorrida llega a la meta elegida, the Trazo App shall dejar de registrar el recorrido.
2. When la distancia recorrida llega a la meta elegida, the Trazo App shall mostrar sobre el mapa un aviso que indica que se alcanzó la meta, con una animación breve y el botón «Finalizar».
3. While el aviso de meta alcanzada está visible, the Trazo App shall mantener el botón «Finalizar» disponible hasta que el corredor lo toque.
4. While el recorrido no alcanzó la meta, the Trazo App shall no mostrar el aviso de meta alcanzada.
5. Where el corredor tiene activada la opción del sistema para quitar animaciones, the Trazo App shall mostrar el aviso de meta alcanzada sin animación.

### Requirement 2: Finalizar con la meta cumplida
**Objective:** As a corredor, I want cerrar el recorrido tras alcanzar la meta, so that vea mi logro reconocido.

#### Acceptance Criteria
1. When el corredor toca «Finalizar» después de haber alcanzado la meta, the Trazo App shall mostrar la pantalla de resultado «¡Meta cumplida!» sin pedir confirmación.

### Requirement 3: Finalizar antes de alcanzar la meta
**Objective:** As a corredor, I want que la app me pida confirmación si todavía no llegué a mi meta, so that no termine el recorrido por error.

#### Acceptance Criteria
1. When el corredor toca «Finalizar» con la meta sin alcanzar y con 200 metros o más recorridos, the Trazo App shall mostrar un diálogo con el mensaje «Todavía no llegaste a tu meta» y las opciones «Finalizar recorrido» y «Cancelar».
2. While el diálogo de confirmación está visible, the Trazo App shall seguir registrando el recorrido.
3. When el corredor elige «Finalizar recorrido», the Trazo App shall detener el registro y mostrar la pantalla de resultado «Recorrido finalizado».
4. When el corredor elige «Cancelar» o cierra el diálogo sin elegir una opción, the Trazo App shall cerrar el diálogo y conservar el recorrido en curso con todos sus datos.

### Requirement 4: Recorrido demasiado corto
**Objective:** As a corredor, I want que la app me avise si el recorrido es demasiado corto para guardarlo, so that decida si lo descarto o sigo corriendo.

#### Acceptance Criteria
1. When el corredor toca «Finalizar» con menos de 200 metros recorridos, the Trazo App shall mostrar un aviso que indica que el recorrido es demasiado corto para guardarlo, con las opciones de descartarlo y de volver al recorrido.
2. The Trazo App shall mostrar el aviso de recorrido demasiado corto en lugar de la confirmación «Todavía no llegaste a tu meta».
3. While el aviso de recorrido demasiado corto está visible, the Trazo App shall seguir registrando el recorrido.
4. When el corredor elige volver al recorrido o cierra el aviso sin elegir una opción, the Trazo App shall cerrar el aviso y conservar el recorrido en curso con todos sus datos.
5. When el corredor confirma que desea descartar el recorrido, the Trazo App shall detener el registro, eliminar los datos registrados, no agregar el recorrido al Historial y volver al Mapa sin recorrido en curso.
6. The Trazo App shall no ofrecer el paso a «Guardar recorrido» para un recorrido de menos de 200 metros.

### Requirement 5: Pantalla de resultado
**Objective:** As a corredor, I want ver un resumen de mi salida al terminar, so that reconozca mi logro y conozca mis resultados.

#### Acceptance Criteria
1. The Trazo App shall mostrar en la pantalla de resultado la distancia, el tiempo y la regularidad del recorrido, y el botón «Continuar».
   - Where el celular no tiene acelerómetro, no hay regularidad: the Trazo App shall mostrar «No disponible» en lugar de la regularidad.
2. When el recorrido terminó con la meta cumplida, the Trazo App shall mostrar la pantalla de resultado con el título «¡Meta cumplida!» y una animación de celebración.
3. When el recorrido terminó antes de alcanzar la meta, the Trazo App shall mostrar la pantalla de resultado con el título «Recorrido finalizado» y sin animación.
4. Where el corredor tiene activada la opción del sistema para quitar animaciones, the Trazo App shall mostrar la pantalla «¡Meta cumplida!» sin animación y con el mismo contenido.
5. The Trazo App shall mostrar los valores de la pantalla de resultado tal como los entrega el registro del recorrido, sin recalcularlos.
6. While se muestra la pantalla de resultado, the Trazo App shall no seguir registrando el recorrido.

### Requirement 6: Continuar a «Guardar recorrido»
**Objective:** As a corredor, I want pasar del resultado al guardado sin perder datos, so that pueda guardar mi recorrido completo.

#### Acceptance Criteria
1. When el corredor toca «Continuar» en la pantalla de resultado, the Trazo App shall abrir la pantalla «Guardar recorrido».
2. When se abre «Guardar recorrido» desde la pantalla de resultado, the Trazo App shall conservar la distancia, el tiempo, la velocidad promedio, las frenadas, la regularidad y la meta elegida del recorrido finalizado.
   - Where el celular no tiene acelerómetro, no hay regularidad: the Trazo App shall enviar la regularidad a «Guardar recorrido» como «sin dato», nunca como 0 %.
3. When se abre «Guardar recorrido» desde la pantalla de resultado, the Trazo App shall conservar además la fecha y hora de inicio, la marca de modo demo y la traza del recorrido.
4. When se abre «Guardar recorrido» desde la pantalla de resultado, the Trazo App shall indicar si el recorrido alcanzó la meta o terminó antes de alcanzarla.
5. The Trazo App shall dejar el ingreso del título, de los destacados y de las fotografías a la pantalla «Guardar recorrido».

### Requirement 7: Prioridad del cierre
**Objective:** As a corredor, I want que el cierre del recorrido no se mezcle con otros avisos, so that vea un solo mensaje a la vez.

#### Acceptance Criteria
1. While hay visible un aviso o diálogo de cierre (el aviso de meta alcanzada, el diálogo «Todavía no llegaste a tu meta» o el aviso de recorrido demasiado corto), the Trazo App shall no mostrar la alerta de frenada ni la consulta de 20 segundos de HU-05.
2. While hay visible un aviso o diálogo de cierre y se produce una frenada, the Trazo App shall registrar igualmente la frenada.
3. If había una consulta de 20 segundos pendiente cuando apareció el aviso o diálogo de cierre, then the Trazo App shall resolver esa consulta como frenada.
