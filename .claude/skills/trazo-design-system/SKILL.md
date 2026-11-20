---
name: trazo-design-system
description: "Sistema de diseño de Trazo / Trazo design system (Kotlin + Jetpack Compose + Material 3). Usar al crear, modificar o revisar pantallas, componentes, colores, tipografía, espaciado, textos de interfaz o el tema de Compose. Use when creating, editing or reviewing screens, components, colors, typography, spacing, UI copy or the Compose theme of the Trazo app."
---

# trazo-design-system Skill

## Rol
Aplicás el sistema de diseño de Trazo a cualquier trabajo de UI, y revisás que la UI existente lo respete.

## Pasos
1. Leé el steering `.kiro/steering/design-system.md`, que resume las reglas.
2. Para medidas exactas, estados de componentes, patrones de pantalla y ejemplos de Compose, leé `references/trazo-design-system.md`, que está en el directorio de esta skill y es la fuente de verdad.
3. Usá el tema existente en `app/src/main/java/com/trazo/app/ui/theme/`. No dupliques tokens: si falta uno, agregalo ahí y en la referencia.
4. Ubicá los archivos según la sección "Ubicación de archivos de UI" del steering. Todo componente nuevo nace en `ui/screens/<pantalla>/components/` y pasa a `ui/components/layout/` o `ui/components/common/` recién cuando lo usa una segunda pantalla. Las pantallas son `map` (pestaña Actividad), `profile` (pestaña Perfil), `settings`, `result` y `saverun`, y la navegación (`TrazoApp`, `BottomNavBar`) va en `ui/navigation/`. Carpetas y archivos siempre en inglés.
5. Envolvé las pantallas en `TrazoTheme { … }`. Los colores van por rol (`MaterialTheme.colorScheme.*`, `TrazoTheme.extendedColors.*`), nunca con hex sueltos.
6. Antes de dar el trabajo por terminado, recorré el checklist de accesibilidad (sección 9 de la referencia) y corré `./gradlew :app:ktlintCheck :app:lint`.

## Reglas que no se rompen
- primary = Primary950 `#1B2E31`.
- Lima `#DCEB9E` / `#556810` es el único verde (Secondary = Success).
- La alerta de frenada va en inverseSurface (Primary900) con el ícono sobre un círculo blanco. Nunca en rojo.
- Un solo botón filled por pantalla. "Cancelar" e "Ir a configuración" son outlined.
- Las métricas usan `.tabular()`. Objetivo táctil mínimo de 48 dp; los controles del recorrido miden 56 dp.
- Textos en voseo rioplatense y con coma decimal.
