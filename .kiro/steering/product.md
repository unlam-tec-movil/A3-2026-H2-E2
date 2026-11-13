# Product Overview

_updated_at: 2026-11-05 · Fuente: specs de `.kiro/specs/` (definición de producto «Trazo v0.6»)._

**Trazo** es una app Android para **corredores recreativos urbanos** (público de referencia: Buenos
Aires) que quieren registrar sus salidas con una meta de distancia, ver el recorrido en vivo sobre
el mapa y entender qué tan fluido y parejo fue el camino (frenadas, pausas y regularidad del piso).

Es un proyecto académico de equipo (UNLaM, Taller de desarrollo móvil) que se construye por
historias de usuario (HU-01, HU-02…), una spec Kiro por historia o grupo de historias.

## Core Capabilities

1. **Iniciar un recorrido con meta fija** de 3, 5 o 10 km, desde Inicio o desde el Mapa.
2. **Seguir el recorrido en vivo**: posición, traza, tiempo, distancia y velocidad, con pausa,
   reanudación y control de calidad del GPS.
3. **Detectar cómo fue el camino** con los sensores del teléfono: frenadas (semáforos, esquinas),
   pausas personales y regularidad del piso.
4. **Cerrar y celebrar**: aviso de meta cumplida, pantalla de resultado y paso al guardado.
5. **Guardar y consultar**: título, destacados y métricas en un Historial local.

## Target Use Cases

- Primer uso: el corredor abre la app, ve Inicio vacío («Todavía no hay recorridos») y empieza un
  recorrido desde el FAB «+».
- Salida con meta: elige 5 km, corre con la pantalla encendida, para en semáforos, pausa si
  necesita, llega a la meta y guarda el recorrido.
- Revisión: entra al Historial para comparar recorridos (distancia, tiempo, frenadas, regularidad).

## Value Proposition

- **Simple y enfocada**: metas fijas, sin configuración de entrenamiento.
- **Sabe cómo fue la ciudad**: distingue frenadas del recorrido de pausas propias, algo que las apps
  de running genéricas no muestran.
- **Privada y offline**: todo queda en el teléfono.

## Product Boundaries (decisiones vigentes)

Estas reglas aplican a todas las specs; si una spec necesita romperlas, se discute en equipo.

- **100 % local**: persistencia en el dispositivo (Room). Sin cuentas, perfiles, comunidad,
  rankings, servidores ni APIs propias.
- **Metas solo de 3, 5 y 10 km**. No hay metas personalizadas.
- **Sin** calorías, planes de entrenamiento, pulsómetros ni wearables.
- **Pantalla encendida durante el recorrido**; no se garantiza seguimiento prolongado en segundo
  plano.
- **Solo español rioplatense con voseo** («Elegí una meta», «Escribí un título»). Sin
  internacionalización.
- **Accesibilidad como requisito**, no como extra: fuente al 200 %, «Quitar animaciones»,
  TalkBack, objetivos táctiles de 48 dp y estado nunca comunicado solo con color.

## Temas de producto abiertos

Ver `team-notes/README.md` para el detalle. Al cierre de esta versión siguen sin definir: fotos en
el guardado, modo demo, regularidad cuando el teléfono no tiene acelerómetro y el flujo exacto al
alcanzar la meta (HU-04 vs. HU-06).

---
_Focus on patterns and purpose, not exhaustive feature lists_
