# Persona 1 · Inicio y Guardar recorrido (`home-recent-runs` HU-01 y `save-run` HU-07)

## En 30 segundos

- **Inicio:** terminada. Falta probarla en un dispositivo y aplicar el «+» deshabilitado.
- **Nuevo:** quedaste a cargo de **`save-run`** (guardar recorrido).
- `save-run` **espera** a que las otras specs terminen y después se adapta a sus contratos.

## Tus próximos pasos

**Inicio (`home-recent-runs`)**

1. [ ] **Probar en un emulador o celular:** `./gradlew connectedDebugAndroidTest`.
2. [ ] Abrir la app: tiene que entrar a Inicio, y «Atrás» tiene que cerrarla.
3. [ ] Revisar que el ícono no se corte y regenerar los `.webp` con *Image Asset Studio*.
4. [ ] Revisar el PR de Persona 2 que mueve `StartRunFab` a `ui/components/common/` (lo usan
   Inicio y Mapa).
5. [ ] **Cuando Persona 3 publique el contrato:** conectar el «+» al booleano «hay recorrido en
   curso».
   - `true` → deshabilitado. `false` → habilitado.
   - Sumá un test en `HomeViewModelTest`: pausado sigue deshabilitado; finalizado se habilita.

**Guardar recorrido (`save-run`)**

6. [ ] Dejar escrito en los requisitos que **sí hay fotos** y que **los recorridos guardados se ven
   en el Mapa**. Hoy figuran como «fuera de alcance»: marcalo como «se suma cuando estén los
   contratos de HU-02 a HU-06».
7. [ ] Esperar los contratos de Persona 3 (resumen del recorrido) y Persona 4 (resultado).
8. [ ] Con los contratos listos: actualizar requisitos, aprobar y seguir con
   `/kiro-spec-design save-run`, `/kiro-spec-tasks` y `/kiro-impl`.

**Equipo**

9. [ ] Revisar con todos `product.md`, `tech.md` y `structure.md` (en `.kiro/steering/`).
10. [ ] Proponer cambiar CI a `koverXmlReportDebug` (hoy la cobertura sale en 0 %).

## Tenés que hablar con…

| Persona | Tema | Qué hay que decidir |
|---|---|---|
| Persona 3 | Botón «+» | Nombre del booleano (propuesta: `isRunInProgress`) |
| Persona 3 | Datos del recorrido para guardar | Qué trae el resumen: fecha y hora, traza, distancia, tiempo, meta |
| Persona 4 | Datos del resultado | Frenadas, regularidad («sin dato» si no hay acelerómetro), meta cumplida |
| Persona 2 | `StartRunFab` | Cuándo lo mueve a `components/common/` y cómo pide los permisos |

## Cuidado con estos archivos

- `TrazoApp.kt`: todos van a sumar rutas. Revisá esos PRs. Vos sumás la ruta de guardar.
- `StartRunFab`, `EmptyState` y `TrazoTopBar` se van a mover a `ui/components/common/`.
- `MapScreen.kt` es de Persona 3: para mostrar recorridos guardados, coordiná con Persona 3.

<details>
<summary><b>Detalle (si lo necesitás)</b></summary>

- **H1 resuelto:** HU-04 expone «hay recorrido en curso». Solo vuelve a `false` al finalizar;
  pausas, frenadas y pérdida de señal no cuentan. Tu `HomeUiState.isStartRunEnabled` ya existe:
  conectarlo es un *Revalidation Trigger* de tu diseño (revalidar 3.1–3.2).
- **C6 y C7 (fotos y traza en Mapa):** decidido que sí. `save-run` hoy las tiene en *Out of scope*
  (líneas 16 y 19 de `requirements.md`).
- **C12:** los componentes compartidos van en `ui/components/common/`. Corregir también el catálogo
  de `ui-components.md` (dice `DropdownMenu`; vos usaste `Popup`).
- **H6:** cuando Historial use `EmptyState`, se mueve a `components/common/`.
- **Menos de 200 m:** esos recorridos no llegan a `save-run` (se descartan en HU-06 R4).
- **Pendiente en el equipo:** regularidad (HU-05.2) y modo demo siguen sin dueño; afectan lo que
  guardás.
- Notas pendientes de `tasks.md`: 3.2 y 4.1 (pruebas en dispositivo) y 4.3 (Kover en release).

</details>
