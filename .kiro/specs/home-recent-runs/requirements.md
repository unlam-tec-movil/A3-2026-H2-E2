# Requirements Document

## Project Description (Input)
Historia: Inicio con sección de recorridos recientes y botón para iniciar recorrido

Como corredor que ingresa por primera vez a Trazo quiero ver en Inicio una sección vacía de recorridos recientes, y poder iniciar un recorrido desde un botón flotante, para comenzar a guardar mis recorridos.

Escenario 1/4: Pantalla de carga
Dado que el corredor abre Trazo por primera vez
Cuando Trazo termina de cargar
Entonces se muestra el logo de Trazo
Y luego dirige al corredor a la pantalla de Inicio.

Escenario 2/4: Todavía no hay recorridos
Dado que el corredor no guardó ningún recorrido
Cuando el corredor está en la pantalla de Inicio
Entonces se muestra una ilustración con el mensaje "Todavía no hay recorridos".

Escenario 3/4: Botón flotante
Dado que no hay un recorrido en curso
Cuando el corredor está en la pantalla de Inicio
Entonces se ve el botón flotante "+" en la esquina inferior derecha
Y está habilitado
Y al seleccionarlo muestra la opción "Iniciar un recorrido".

Escenario 4/4: Barra de navegación
Dado que la barra de navegación muestra las opciones "Inicio", "Mapa" e "Historial" con sus iconos correspondientes
Cuando el corredor está en la pantalla de Inicio
Entonces la opción de "Inicio" se ve acentuada
Y las opciones "Mapa" e "Historial" se ven desactivadas.

## Introduction
Cuando un corredor abre Trazo por primera vez, todavía no guardó ningún recorrido. Esta funcionalidad arma su primer contacto con la app: el logo de Trazo como ícono de la app, que el sistema muestra al abrirla, y después la pantalla de Inicio. Inicio tiene la sección de actividad reciente vacía, un botón flotante "+" para empezar un recorrido y la barra de navegación inferior con Inicio activo.

## Boundary Context
- **In scope**:
  - El logo de Trazo como ícono de la app y el paso automático a Inicio al abrirla.
  - El estado vacío de la sección de actividad reciente en Inicio (ilustración y mensaje "Todavía no hay recorridos").
  - El botón flotante "+" de Inicio y la opción "Iniciar un recorrido" que muestra al tocarlo.
  - La barra superior de Inicio con "Trazo" y el botón de Configuración.
  - La barra de navegación inferior (Inicio · Mapa · Historial) con Inicio activo.
- **Out of scope**:
  - Lo que pasa al tocar "Iniciar un recorrido" (el modal "Iniciar nuevo recorrido", la elección de meta y el registro del recorrido).
  - La lista de recorridos guardados en Inicio (cards compactas y "Ver todo").
  - El estado deshabilitado del botón "+" mientras hay un recorrido en curso.
  - El contenido de las pantallas Mapa e Historial.
  - La pantalla de Configuración: el botón de la barra superior todavía no navega.
  - Una pantalla de carga (splash) propia o configurada: no se agrega pantalla, librería ni tema de arranque.
- **Adjacent expectations**:
  - Las pantallas Mapa e Historial existen como destinos de la barra de navegación, aunque su contenido lo definen otras historias.
  - Otra historia define qué pasa al tocar "Iniciar un recorrido". Esta funcionalidad solo muestra esa opción.
  - Desde Android 12, el sistema muestra el ícono de la app al abrirla. En Android 7 a 11 el sistema no lo hace, así que ahí el logo se ve solo en el launcher.

## Requirements

### Requirement 1: Logo de Trazo al abrir la app
**Objective:** As a corredor, I want reconocer el logo de Trazo en mi teléfono y verlo al abrir la app, so that sepa que abrí la app correcta.

#### Acceptance Criteria
1. The Trazo app shall usar el logo de Trazo como ícono de la app en el launcher.
2. When el corredor abre Trazo en un dispositivo con Android 12 o superior, the Trazo app shall mostrar por un momento el logo de Trazo (el ícono de la app), tal como el sistema lo muestra al abrir cualquier app.
3. When Trazo termina de abrir, the Trazo app shall llevar al corredor a la pantalla de Inicio sin que tenga que hacer nada y sin agregar una espera extra.
4. If el corredor usa el botón "Atrás" del sistema en la pantalla de Inicio, the Trazo app shall no volver a mostrar el logo de apertura.

### Requirement 2: Estado vacío de la actividad reciente en Inicio
**Objective:** As a corredor que todavía no guardó recorridos, I want ver en Inicio una sección de actividad reciente vacía con un mensaje claro, so that entienda que ahí van a aparecer mis recorridos.

#### Acceptance Criteria
1. While el corredor no tiene ningún recorrido guardado, the pantalla de Inicio shall mostrar una ilustración junto con el mensaje "Todavía no hay recorridos".
2. While el corredor no tiene ningún recorrido guardado, the pantalla de Inicio shall no mostrar cards de recorridos.
3. The pantalla de Inicio shall comunicar el estado vacío con texto que un lector de pantalla pueda leer, y no solo con la ilustración.

### Requirement 3: Botón flotante para iniciar un recorrido
**Objective:** As a corredor, I want un botón flotante "+" en Inicio, so that pueda empezar un recorrido desde ahí.

#### Acceptance Criteria
1. While no hay un recorrido en curso, the pantalla de Inicio shall mostrar el botón flotante "+" en la esquina inferior derecha, por encima de la barra de navegación.
2. While no hay un recorrido en curso, the botón flotante "+" shall estar habilitado.
3. When el corredor toca el botón flotante "+", the pantalla de Inicio shall mostrar la opción "Iniciar un recorrido".
4. While la opción "Iniciar un recorrido" está visible, when el corredor toca fuera de ella, the pantalla de Inicio shall ocultarla sin iniciar un recorrido.
5. The botón flotante "+" shall tener una descripción accesible que diga para qué sirve, porque solo muestra un ícono.

### Requirement 4: Barra de navegación inferior
**Objective:** As a corredor, I want ver en qué sección estoy y poder pasar a Mapa o Historial, so that me ubique dentro de la app.

#### Acceptance Criteria
1. The Trazo app shall mostrar en la barra de navegación inferior las opciones "Inicio", "Mapa" e "Historial", cada una con su ícono y su etiqueta.
2. While el corredor está en la pantalla de Inicio, the barra de navegación shall mostrar la opción "Inicio" acentuada (con el indicador de selección y el ícono relleno).
3. While el corredor está en la pantalla de Inicio, the barra de navegación shall mostrar "Mapa" e "Historial" como opciones no seleccionadas (sin acento), y el corredor las va a poder seguir tocando.
4. When el corredor toca "Mapa" o "Historial", the Trazo app shall llevarlo a esa pantalla y acentuar esa opción en la barra de navegación.
5. The barra de navegación shall mostrar cuál es la opción seleccionada con algo más que el color: con el ícono relleno y la etiqueta en negrita.

### Requirement 5: Barra superior de Inicio
**Objective:** As a corredor, I want ver el nombre de la app arriba en Inicio y un acceso a Configuración, so that reconozca la app y sepa dónde ajustarla.

#### Acceptance Criteria
1. While el corredor está en la pantalla de Inicio, the pantalla de Inicio shall mostrar arriba una barra en el color principal de la marca (Primary950) con el texto "Trazo" a la izquierda.
2. While el corredor está en la pantalla de Inicio, the barra superior shall mostrar a la derecha un botón de Configuración con ícono de tuerca y la descripción accesible "Configuración".
3. When el corredor toca el botón de Configuración, the pantalla de Inicio shall no navegar a ninguna pantalla en esta versión (Configuración la define otra historia).
4. While el corredor está en la pantalla de Inicio, the Trazo app shall mostrar los íconos de la barra de estado del sistema en un color legible sobre la barra superior.
