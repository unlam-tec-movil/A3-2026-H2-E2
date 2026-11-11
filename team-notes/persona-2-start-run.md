# Persona 2 · Iniciar recorrido (`start-run`, HU-02 y HU-03)

## En 30 segundos

- La reunión **resolvió tus choques con Persona 3**. Ahora hay que pasarlo a tus requisitos.
- Vos pedís los permisos (una sola vez) y **no arrancás sin señal GPS**.
- Después de eso, aprobás los requisitos y seguís con el diseño.

## Lo que se decidió para vos

- **Permisos:** el «+» (`StartRunFab`) los pide, en **Inicio y en Mapa**, solo si el corredor
  **nunca los dio**. Se dan **una sola vez**.
- **Señal GPS:** se espera la señal antes de arrancar («Esperando señal GPS»). Tu R8 queda.
- **Fecha y hora de inicio:** las guarda Persona 3. Vos no.
- **«+» en Mapa:** lo deshabilitás vos con el booleano de Persona 3.
- **Componentes:** el modal, `GoalChip` y `StartRunFab` van en `ui/components/common/`.

## Tus próximos pasos

1. [ ] **Sumar un escenario** a tus requisitos: tocar «Iniciar un recorrido» desde **Mapa** sin
   permisos los pide igual que en Inicio. Si ya se dieron, no se piden.
2. [ ] Ajustar R6 para que diga «desde Inicio o desde Mapa» y «una sola vez».
3. [ ] Sacar de *Out of scope* el «+» deshabilitado: ahora lo aplicás vos en Mapa.
4. [ ] Cerrar tus dudas internas:
    - «Comenzar» deshabilitado hasta elegir meta + texto «Elegí una meta para continuar».
    - Texto con voseo: «Seleccioná una meta:».
5. [ ] Aprobar `requirements.md`.
6. [ ] `/kiro-validate-gap start-run` (mirá también la rama `origin/feat/modal-iniciar-recorrido`).
7. [ ] `/kiro-spec-design start-run`. Incluí:
    - Mover `StartRunFab` a `ui/components/common/` (avisale a Persona 1).
    - Conectar «Iniciar un recorrido» en `TrazoApp.kt` (hoy no hace nada).
    - Esperar señal GPS y **recién ahí** llamar a Persona 3 (ver abajo).
    - Al tocar «Comenzar» desde Inicio, ir a Mapa.
8. [ ] `/kiro-spec-tasks start-run` y `/kiro-impl start-run`: **primero el modal y la meta**,
   después permisos y GPS.

## Cómo arranca un recorrido

Cuando ya hay señal GPS, tu código llama a:

```kotlin
runSessionRepository.attachActiveSession(
    InitializedRunSession(id = "", goal = RunGoal.KM_5, startMonotonicTimestamp = "")
)
```

Persona 3 guarda la fecha y hora en ese momento y pone «hay recorrido en curso» en `true`.

## Tenés que hablar con…

| Persona | Tema | Qué hay que decidir |
|---|---|---|
| Persona 3 | Arranque | Que su temporizador arranque recién en `attachActiveSession` (después de la señal) |
| Persona 3 | Botón «+» | Nombre del booleano (propuesta: `isRunInProgress`) |
| Persona 3 | `MapScreen` | Cómo montás el «+» y la pedida de permisos por parámetro |
| Persona 1 | `StartRunFab` | Cuándo lo movés a `components/common/` |

## Cuidado con estos archivos

- `TrazoApp.kt`: conectás el botón de Inicio.
- `MapScreen.kt` es de Persona 3: el «+» se monta por parámetro.
- `AndroidManifest.xml`: los permisos de ubicación los declarás vos.

<details>
<summary><b>Detalle (si lo necesitás)</b></summary>

- **C10 resuelto:** la fecha y hora las guarda HU-04. HU-06 dice que «se especifican en HU-02/HU-03»;
  Persona 4 corrige esa frase.
- **C11 resuelto:** gana tu R8 («Esperando señal GPS», no arranca sin señal). El umbral de 30 m
  coincide con HU-04.
- **C12 resuelto:** todo componente compartido va en `ui/components/common/`. Tu Open Question 3
  queda cerrada así.
- **H1 resuelto:** booleano de HU-04. Pausas, frenadas y pérdida de señal **no** lo vuelven a
  `false`; solo finalizar.
- **C13:** Open Questions 1 y 2 de tu spec (siguen siendo tuyas).
- Duda menor: ¿quién genera el `id` de `InitializedRunSession`? Lo armás vos.

</details>
