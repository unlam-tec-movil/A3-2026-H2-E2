
# Requirements Document

## Project Description (Input)
HU-05.1 · Frenadas y pausas personales

**Quién:** corredor recreativo urbano de Buenos Aires que registra un recorrido con Trazo.

**Situación actual:** durante un recorrido en curso la app no detecta cuándo el corredor deja de moverse, por lo que no distingue una frenada (semáforo, esquina) de una pausa personal (un descanso propio).

**Qué debe cambiar:** la app detecta con el acelerómetro cuándo el corredor deja de moverse, registra la frenada con su ubicación, muestra una alerta animada y, si la detención se prolonga, permite al corredor clasificarla como frenada o pausa personal.

**Historia de usuario:** Como corredor quiero que la app detecte automáticamente los momentos en los que dejo de moverme y mida la regularidad del piso para conocer qué tan fluido y parejo fue mi recorrido, diferenciando las frenadas propias del recorrido de las pausas personales. (Esta spec cubre la detección de frenadas y pausas personales; la pausa del recorrido, la regularidad y el sensor se cubren en HU-05.2.)

**Escenarios a cubrir:**
1. **Movimiento continuo:** Dado que hay un recorrido en curso y el corredor se mueve, Cuando el movimiento continúa sin superar el umbral de detención, Entonces el estado se mantiene como Movimiento continuo y no se registra ninguna frenada.
2. **Frenada por semáforo o intersección:** Dado que hay un recorrido en curso y el corredor se mueve, Cuando permanece sin movimiento durante 8 segundos, Entonces el estado cambia a Frenada, se registra la ubicación de la detención y se muestra una alerta animada con el mensaje «Se detectó una frenada por semáforo o intersección».
3. **Detención inferior a 8 segundos:** Dado que hay un recorrido en curso, Cuando el corredor permanece sin movimiento menos de 8 segundos y retoma el movimiento, Entonces la detención no se registra como frenada y el estado vuelve a Movimiento continuo.
4. **Frenada que se prolonga más de 20 segundos:** Dado que hay un recorrido en curso y el corredor permanece sin movimiento más de 20 segundos, Entonces se muestra un mensaje para identificar el tipo de detención con las opciones «Frenada» y «Pausa personal».
5. **Confirmar una frenada prolongada:** Dado que la app detectó una detención de más de 20 segundos, Cuando el corredor selecciona «Frenada», Entonces la detención se mantiene registrada como frenada y se conserva su ubicación.
6. **Identificar una pausa personal:** Dado que la app detectó una detención de más de 20 segundos, Cuando el corredor selecciona «Pausa personal», Entonces la detención no se contabiliza como frenada y su ubicación no se incluye entre las frenadas registradas.

## Introducción
Durante un recorrido en curso, Trazo debe distinguir cuándo el corredor deja de moverse y qué tipo de detención es: una frenada propia del recorrido (semáforo, esquina) o una pausa personal (un descanso propio). Esta spec cubre la detección de la detención, el registro de la frenada con su ubicación, la alerta animada y la clasificación que el corredor hace de una detención prolongada.

## Contexto de límites
- **Dentro del alcance**: estados «Movimiento continuo» y «Frenada» durante un recorrido en curso; registro de la ubicación de cada frenada; alerta animada de frenada; consulta de clasificación («Frenada» / «Pausa personal») ante una detención de más de 20 segundos; exclusión de las pausas personales de las frenadas registradas.
- **Fuera del alcance**: pausar o reanudar el recorrido, la medición de regularidad del piso y la gestión del sensor (HU-05.2); el conteo y la presentación de frenadas en resultados e historial.
- **Expectativas adyacentes**: existe un recorrido en curso con ubicación disponible; esta spec solo actúa mientras ese recorrido está en curso. El umbral de detención (qué nivel de movimiento se considera «sin movimiento») es un parámetro de la solución y se define en el diseño.

## Requirements

### Requirement 1: Estado de movimiento del recorrido
**Objective:** Como corredor, quiero que la app sepa si me estoy moviendo o estoy detenido, para que solo las detenciones reales se consideren frenadas.

#### Acceptance Criteria
1. While hay un recorrido en curso y el corredor se mueve sin superar el umbral de detención, the Detector de Frenadas shall mantener el estado «Movimiento continuo».
2. While el estado es «Movimiento continuo», the Detector de Frenadas shall no registrar ninguna frenada.
3. When el corredor permanece sin movimiento menos de 8 segundos y retoma el movimiento, the Detector de Frenadas shall no registrar la detención como frenada y volver al estado «Movimiento continuo».
4. While no hay un recorrido en curso, the Detector de Frenadas shall no detectar ni registrar frenadas.

### Requirement 2: Detección y registro de la frenada
**Objective:** Como corredor, quiero que la app registre dónde frené, para conocer las frenadas propias de mi recorrido.

#### Acceptance Criteria
1. When hay un recorrido en curso y el corredor permanece sin movimiento durante 8 segundos, the Detector de Frenadas shall cambiar el estado a «Frenada».
2. When el estado cambia a «Frenada», the Detector de Frenadas shall registrar la ubicación de la detención.
3. When el estado cambia a «Frenada», the Detector de Frenadas shall registrar una única frenada por cada detención continua.
4. When el corredor retoma el movimiento después de una frenada, the Detector de Frenadas shall volver al estado «Movimiento continuo» y conservar la frenada ya registrada.

### Requirement 3: Alerta animada de frenada
**Objective:** Como corredor, quiero ver un aviso claro cuando la app detecta una frenada, para saber que quedó registrada.

#### Acceptance Criteria
1. When el estado cambia a «Frenada», the Trazo App shall mostrar una alerta animada con el mensaje «Se detectó una frenada por semáforo o intersección».
2. Where el corredor tiene activada la opción del sistema para quitar animaciones, the Trazo App shall mostrar la alerta sin animación.
3. When se muestra la alerta de frenada, the Trazo App shall anunciarla a los servicios de accesibilidad.
4. The Trazo App shall comunicar la alerta de frenada con texto e ícono, sin depender solo del color.

### Requirement 4: Consulta ante una detención prolongada
**Objective:** Como corredor, quiero poder indicar si mi detención fue una frenada o una pausa personal, para que mi recorrido refleje lo que realmente pasó.

#### Acceptance Criteria
1. When hay un recorrido en curso y el corredor permanece sin movimiento más de 20 segundos, the Trazo App shall mostrar un mensaje para identificar el tipo de detención con las opciones «Frenada» y «Pausa personal».
2. When el corredor selecciona «Frenada», the Detector de Frenadas shall mantener la detención registrada como frenada y conservar su ubicación.
3. When el corredor selecciona «Pausa personal», the Detector de Frenadas shall no contabilizar la detención como frenada.
4. When el corredor selecciona «Pausa personal», the Detector de Frenadas shall excluir la ubicación de la detención de las frenadas registradas.
5. When el corredor selecciona una de las opciones, the Trazo App shall cerrar el mensaje de identificación.
6. The Trazo App shall presentar el mensaje de identificación con texto accionable, legible con la fuente del sistema al 200 % y con objetivos táctiles de al menos 48 dp.
