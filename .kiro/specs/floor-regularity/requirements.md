# Requirements Document

## Project Description (Input)

HU-05.2 · Regularidad del piso

**Quién:** corredor recreativo urbano de Buenos Aires que registra un recorrido con Trazo.

**Situación actual:** la app detecta frenadas y pausas personales (HU-05.1), pero no dice qué tan
parejo fue el camino. Además, la pausa manual, la reanudación y el ciclo de vida del acelerómetro
quedaron fuera de HU-05.1.

**Qué debe cambiar:** mientras el corredor corre, la app mide con el acelerómetro qué tan parejo fue
el camino y entrega un porcentaje de regularidad para el resultado, el guardado y el historial
(métrica «Regularidad»). Si el teléfono no tiene acelerómetro, la app avisa y la regularidad queda
«sin dato».

**Escenarios a cubrir (HU-05, historia #35, Issues #43 a #47):**

8. **Pausa del recorrido:** Dado que hay un recorrido en curso, Cuando el corredor utiliza el botón
   «Pausa», Entonces el recorrido entra en estado de pausa, el tiempo y la captura de nuevos puntos
   de la traza se detienen y la pausa del recorrido no se contabiliza como frenada.
9. **Reanudar después de una pausa del recorrido:** Dado que el recorrido está pausado con el
   botón, Cuando el corredor selecciona «Reanudar», Entonces se retoma el registro del recorrido y
   el estado de movimiento vuelve a evaluarse desde ese momento.
10. **Cálculo de la regularidad:** Dado que hay un recorrido en curso dividido en tramos de 10
    segundos, Cuando el recorrido finaliza, Entonces la app calcula la regularidad a partir de los
    tramos con vibración baja; si 8 de cada 10 tramos tuvieron vibración baja, la regularidad es de
    80 %.
11. **Dispositivo sin acelerómetro:** Dado que el dispositivo no cuenta con acelerómetro, Cuando el
    corredor inicia un recorrido, Entonces se muestra un aviso indicando que no se podrán medir
    frenadas ni regularidad y el recorrido se registra únicamente con distancia, tiempo y velocidad.
12. **Dejar de escuchar el sensor al terminar:** Dado que el recorrido finalizó o se descartó,
    Cuando se cierra el recorrido, Entonces la app deja de leer el acelerómetro.

## Introducción

Esta spec entrega la métrica «Regularidad»: el porcentaje de tramos del recorrido en los que el
corredor sintió el piso parejo (vibración baja). También define cuándo se mide (solo mientras el
corredor corre), qué pasa cuando el teléfono no tiene acelerómetro y quién deja de leer el sensor
cuando el recorrido termina. El acelerómetro es un único sensor que comparte con la detección de
frenadas (HU-05.1): la lectura es una sola para ambas funciones.

## Contexto de límites

- **Dentro del alcance**: la medición de regularidad por tramos de 10 segundos; el valor final de
  regularidad y el momento en que está disponible; el aviso por falta de acelerómetro (la señal de
  disponibilidad del sensor y el texto del aviso); que la pausa manual no cuente como frenada ni
  como tramo de regularidad; la reevaluación del movimiento al reanudar; dejar de leer el
  acelerómetro cuando el recorrido termina o se descarta.
- **Fuera del alcance**:
  - Detectar y clasificar frenadas y pausas personales (HU-05.1 / `stop-detection`); esta spec no
    cambia sus reglas.
  - Implementar la pausa y la reanudación manual: ya existen (HU-04). Esta spec solo define cómo
    afectan a la regularidad y a las frenadas.
  - Armar el resumen del recorrido y los campos de frenadas y regularidad del resumen (Persona 3).
  - Mostrar la regularidad en la tarjeta, el detalle, el resultado o el historial (HU-06,
    `save-run`, Historial) y ubicar el aviso en las pantallas del Mapa (otras personas): esta spec
    solo produce el valor, la señal y el texto.
  - Guardar la regularidad (`save-run`).
- **Expectativas adyacentes**: existe un recorrido en curso con los estados reales de HU-04
  (activo, pausado, detenido por frenada, en pausa personal). La detección de frenadas ya queda
  inactiva durante la pausa manual. La app mantiene la pantalla encendida durante el recorrido, así
  que no se mide con la pantalla apagada.

## Requirements

### Requirement 1: La pausa manual no cuenta como frenada

**Objective:** Como corredor, quiero que cuando pauso el recorrido a propósito la app no me anote una
frenada, para que mis frenadas reflejen solo los semáforos y esquinas reales.

#### Acceptance Criteria

1. While el recorrido está pausado con el botón «Pausa», the Detector de Frenadas shall no registrar
   ninguna frenada.
2. When el corredor pausa el recorrido con el botón «Pausa», the Detector de Frenadas shall no
   contabilizar la pausa como frenada ni como pausa personal.
3. When el corredor pausa el recorrido con el botón «Pausa», the Detector de Frenadas shall
   conservar las frenadas ya registradas antes de la pausa.
4. While el recorrido está pausado con el botón «Pausa», the Medidor de Regularidad shall no
   incorporar ese tiempo a ningún tramo de regularidad.

### Requirement 2: Reanudar después de una pausa manual

**Objective:** Como corredor, quiero que al reanudar el recorrido la app evalúe mi movimiento desde
cero, para que la pausa no me genere frenadas ni ensucie la regularidad.

#### Acceptance Criteria

1. When el corredor selecciona «Reanudar» después de una pausa con el botón, the Detector de
   Frenadas shall volver a evaluar el estado de movimiento a partir de ese momento, sin arrastrar
   quietud de antes o durante la pausa.
2. When el corredor selecciona «Reanudar» después de una pausa con el botón, the Medidor de
   Regularidad shall empezar un tramo nuevo a partir de ese momento.
3. When el corredor selecciona «Reanudar», the Medidor de Regularidad shall conservar los tramos
   completos medidos antes de la pausa.

### Requirement 3: Medición de la regularidad por tramos

**Objective:** Como corredor, quiero que la app mida qué tan parejo fue el piso a lo largo de todo el
recorrido, para saber en qué proporción del camino corrí sin vibración fuerte.

#### Acceptance Criteria

1. While hay un recorrido en curso y el corredor está corriendo, the Medidor de Regularidad shall
   dividir el recorrido en tramos consecutivos de 10 segundos.
2. When un tramo de 10 segundos se completa, the Medidor de Regularidad shall clasificarlo como
   tramo de vibración baja o de vibración alta.
3. The Medidor de Regularidad shall calcular la regularidad como la proporción de tramos de
   vibración baja sobre el total de tramos completos medidos, expresada como porcentaje.
4. When el recorrido tuvo 10 tramos completos y 8 fueron de vibración baja, the Medidor de
   Regularidad shall informar una regularidad de 80 %.
5. While el recorrido está pausado con el botón «Pausa», detenido por una frenada o en pausa
   personal, the Medidor de Regularidad shall no incorporar ese tiempo a ningún tramo, para que
   estar quieto no infle el porcentaje. (Supuesto a validar, ver pregunta abierta 3.)
6. If el estado del recorrido deja de ser «corriendo» antes de completar un tramo, then the Medidor
   de Regularidad shall descartar ese tramo incompleto y empezar uno nuevo cuando el corredor
   vuelva a correr.
7. If el recorrido finaliza con un último tramo de menos de 10 segundos, then the Medidor de
   Regularidad shall no contar ese tramo. (Supuesto a validar, ver pregunta abierta 2.)
8. If el sensor deja de entregar lecturas durante un tramo, then the Medidor de Regularidad shall
   descartar ese tramo en lugar de clasificarlo.
9. The Medidor de Regularidad shall definir «vibración baja» mediante un valor de referencia
   ajustable, calibrado en un teléfono real, y no con un valor fijo sin respaldo. (Ver pregunta
   abierta 1.)

### Requirement 4: Valor de regularidad para el resultado y el guardado

**Objective:** Como corredor, quiero ver un porcentaje de regularidad claro al terminar, para
comparar recorridos en el historial.

#### Acceptance Criteria

1. When el recorrido finaliza, the Medidor de Regularidad shall tener disponible el valor final de
   regularidad, para que quede incluido cuando se arma el resumen del recorrido.
2. The Medidor de Regularidad shall informar la regularidad como un porcentaje entero entre 0 y
   100. (El redondeo se valida en el diseño.)
3. If el recorrido finaliza sin ningún tramo completo medido, then the Medidor de Regularidad shall
   informar la regularidad como «sin dato» y no como 0 %.
4. The Medidor de Regularidad shall mantener el valor de regularidad del recorrido terminado
   disponible hasta que empiece el siguiente recorrido.
5. When empieza un recorrido nuevo, the Medidor de Regularidad shall empezar la medición desde cero,
   sin arrastrar tramos del recorrido anterior.
6. Where se muestra una regularidad «sin dato», the app shall comunicarlo con el texto «sin dato» y
   no solo con color.

### Requirement 5: Dispositivo sin acelerómetro

**Objective:** Como corredor con un teléfono sin acelerómetro, quiero saber desde el inicio qué no se
va a medir, para no esperar frenadas ni regularidad que no van a aparecer.

#### Acceptance Criteria

1. When el corredor inicia un recorrido en un dispositivo sin acelerómetro, the app shall mostrar un
   aviso indicando que no se podrán medir frenadas ni regularidad.
2. The aviso shall comunicarse con texto y un ícono, no solo con color, y ser legible con la fuente
   al 200 % y anunciado por TalkBack.
3. While el dispositivo no tiene acelerómetro, the app shall registrar el recorrido únicamente con
   distancia, tiempo y velocidad.
4. While el dispositivo no tiene acelerómetro, the Medidor de Regularidad shall informar la
   regularidad como «sin dato» y nunca como 0 %.
5. While el dispositivo no tiene acelerómetro, the app shall tratar las frenadas del recorrido como
   «sin dato». (Supuesto a validar con `save-run` y Persona 3, ver pregunta abierta 5.)
6. The Medidor de Regularidad shall exponer si el dispositivo tiene acelerómetro y el texto del
   aviso, para que la pantalla que corresponda los muestre sin tener que consultar el sensor. (La
   ubicación del aviso en pantalla es de otras personas, ver pregunta abierta 6.)
7. While el dispositivo no tiene acelerómetro, the app shall no fallar ni interrumpir el recorrido.

### Requirement 6: Una sola lectura del acelerómetro, y solo mientras hace falta

**Objective:** Como corredor, quiero que la app lea el acelerómetro solo mientras corro, para no
gastar batería de más ni medir después de terminar.

#### Acceptance Criteria

1. While la detección de frenadas y la medición de regularidad necesitan el acelerómetro al mismo
   tiempo, the app shall mantener una sola lectura compartida, sin duplicar el sensor.
2. When el recorrido finaliza, the app shall dejar de leer el acelerómetro.
3. When el recorrido se descarta, the app shall dejar de leer el acelerómetro.
4. While el recorrido está pausado con el botón «Pausa», the app shall no leer el acelerómetro.
5. When el corredor reanuda el recorrido, the app shall volver a leer el acelerómetro.
6. While no hay un recorrido en curso, the app shall no leer el acelerómetro.
7. When ninguna función necesita el acelerómetro, the app shall dar de baja la lectura del sensor.

### Requirement 7: Calidad y verificación

**Objective:** Como equipo, queremos que las reglas de tiempo y de cálculo estén probadas, para que
los umbrales se puedan ajustar sin romper el comportamiento.

#### Acceptance Criteria

1. The Medidor de Regularidad shall tener una cobertura de pruebas mayor al 80 % sobre sus reglas
   de cálculo y de ciclo de vida.
2. The Medidor de Regularidad shall poder verificarse con tiempo simulado, sin esperar 10 segundos
   reales por tramo.
3. The Medidor de Regularidad shall tener una prueba por cada escenario E8 a E12 y por cada caso
   límite de los requisitos 3 y 4 (último tramo corto, recorrido sin tramos completos, tramos
   descartados).
4. The Medidor de Regularidad shall no alterar las reglas de detección de frenadas de
   `stop-detection`: sus pruebas existentes deben seguir pasando.

## Preguntas abiertas y supuestos a validar

Los escenarios no definen estos puntos. Se registran como supuestos y se resuelven en el diseño o
con el equipo; no son valores acordados.

1. **Umbral de «vibración baja» y cómo se mide.** Propuesta: desvío estándar de la magnitud de la
   aceleración en cada tramo de 10 s, con un umbral calibrado en un teléfono real (igual que el
   umbral de quietud de `stop-detection`).
2. **Último tramo de menos de 10 s.** Propuesta: no se cuenta.
3. **Qué tramos cuentan.** Propuesta: solo los tramos en que el corredor corre; los tramos en pausa
   manual, frenada o pausa personal no cuentan, porque quieto da vibración baja e inflaría el
   porcentaje.
4. **Recorrido sin ningún tramo completo.** Propuesta: «sin dato».
5. **Sin acelerómetro.** Regularidad «sin dato». Falta acordar si las frenadas también son «sin
   dato» o 0 (con `save-run` y Persona 3).
6. **Dónde se muestra el aviso de E11.** Las pantallas del Mapa son de otras personas: esta spec
   define la señal de disponibilidad del sensor y el texto del aviso, pero no lo cablea.

## Dependencias y acuerdos abiertos

| Con quién | Qué falta acordar | Propuesta de esta spec |
|-----------|-------------------|------------------------|
| Persona 3 (`FinishRunUseCase`, `FrozenRunSummary`) | **Cómo llega el dato al resumen en `finishSession()`.** Ella agrega los campos de frenadas y regularidad; esta spec no toca ese archivo. | La regularidad ya está calculada y disponible al finalizar (requisito 4.1), así que `finishSession()` puede leerla en ese momento sin esperar nada. Falta acordar si la lee ella directamente o si se le inyecta. |
| Persona 3 | Frenadas «sin dato» o 0 cuando no hay acelerómetro (requisito 5.5). | «Sin dato» en ambas, con la misma señal de disponibilidad. |
| `save-run` (Persona 4) | **Cuándo lee la regularidad.** | El valor sigue disponible después de finalizar o descartar, hasta que empiece el siguiente recorrido (requisito 4.4). Debe leerse antes de iniciar el siguiente. |
| `stop-detection` (Persona 4) | Que el acelerómetro compartido pase a tener un único gestor de ciclo de vida (requisito 6.1). | No cambia el dominio ni las reglas de `stop-detection`. Solo cambia de dónde sale el flujo de muestras que se le inyecta. Avisar para que su módulo de inyección no cree otro. |
| Pantallas del Mapa, resultado, guardado e historial | Dónde se muestra el aviso (E11) y cómo se presenta «sin dato». | Esta spec entrega el valor, la señal y el texto; la presentación es de cada pantalla (HU-06, `save-run`, Historial). |
