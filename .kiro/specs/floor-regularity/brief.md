# Brief · floor-regularity (HU-05.2 · Regularidad del piso)

## Descripción

Mientras el corredor corre, la app mide qué tan parejo fue el camino con el acelerómetro y entrega un
porcentaje de regularidad para el resultado, el guardado y el historial (métrica «Regularidad»).
Rama: `feat/hu-05-frenadas`. Escenarios de origen: HU-05 (#35), Issues #43 a #47 (E8 a E12).

## Escenarios (texto literal de los Issues)

- **E8 · Pausa del recorrido**: con un recorrido en curso, al usar «Pausa» el recorrido entra en
  pausa, el tiempo y la captura de puntos se detienen y la pausa no se contabiliza como frenada.
- **E9 · Reanudar**: con el recorrido pausado con el botón, al elegir «Reanudar» se retoma el
  registro y el estado de movimiento vuelve a evaluarse desde ese momento.
- **E10 · Cálculo de la regularidad**: el recorrido se divide en tramos de 10 s; al finalizar la app
  calcula la regularidad con los tramos de vibración baja (8 de 10 tramos → 80 %).
- **E11 · Dispositivo sin acelerómetro**: al iniciar un recorrido se avisa que no se podrán medir
  frenadas ni regularidad; el recorrido se registra solo con distancia, tiempo y velocidad.
- **E12 · Dejar de escuchar el sensor**: al finalizar o descartar el recorrido, la app deja de leer el
  acelerómetro.

## Contexto verificado

- Reutiliza `MotionSource` y `AccelerometerMotionSource` (flujo frío con `callbackFlow`,
  `SENSOR_DELAY_UI`, flujo vacío sin acelerómetro). Hay un único sensor compartido con
  `stop-detection`; esta spec define quién gestiona su ciclo de vida. No se registra un segundo
  listener.
- `stop-detection` deja fuera la medición de regularidad, la gestión del sensor y la pausa manual.
  Esta spec respeta ese límite sin cambiar el dominio de `stop-detection`.
- `RunState` real (HU-04): `IDLE`, `ACTIVE`, `PAUSED`, `STOPPED`, `PERSONAL_PAUSED`. La pausa y la
  reanudación manual ya existen (`pauseSession` / `resumeSession`): no se reimplementan.
- `FrozenRunSummary` es de Persona 3, quien agrega los campos de frenadas y regularidad. Esta spec
  solo define qué valor entrega y cuándo está disponible; no modifica ese archivo.
- La presentación (card, detalle, resultado) es de otras personas (HU-06, `save-run`, Historial).

## Preguntas abiertas / supuestos a validar (los escenarios no los definen)

1. Umbral de «vibración baja» y cómo se mide (propuesta: desvío estándar de la magnitud por tramo de
   10 s; se calibra en un teléfono real).
2. Último tramo de menos de 10 s.
3. Tramos en pausa manual, `STOPPED` o `PERSONAL_PAUSED` (propuesta: no cuentan).
4. Sin ningún tramo completo: `null` («sin dato»).
5. Sin acelerómetro: regularidad `null`; frenadas «sin dato» o 0 (a acordar con `save-run` y Persona 3).
6. Dónde se muestra el aviso de E11 (esta spec define la señal y el texto; no cablea `MapScreen`).

## Reglas

Kotlin + Hilt, español rioplatense con voseo, cobertura mayor a 80 %, sin sobre-ingeniería, estado
nunca solo con color.
