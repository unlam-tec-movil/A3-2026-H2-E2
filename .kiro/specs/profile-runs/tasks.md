# Implementation Plan

> **Orden de entrega (Prioridad de los requisitos):** HU-09 (grupos 2 a 4), HU-16 (tareas 1.5 y 2.1 a 2.3), dibujo de trazas de HU-12 (grupo 5) y, al final y **recortable**, abrir el detalle tocando la traza (grupo 6). Nada de los grupos 1 a 5 depende del grupo 6; el grupo 7 valida lo que se entregue.
>
> **Fuera de esta spec:** el criterio 1.8 (conservar recorridos al cerrar la app) depende de que `save-run` reemplace el almacenamiento en memoria por Room. Esta spec solo lee, por lo que no tiene tareas de persistencia.

- [ ] 1. Base: textos y piezas compartidas
- [x] 1.1 Agregar los textos de la funcionalidad
  - Agregar a los recursos de texto, en español rioplatense con voseo, las marcas «Incompleto», «Demo» y «Meta cumplida», los rótulos de la card y del detalle (velocidad promedio, meta, destacados, fotos), «Cerrar», el texto alternativo cuando la traza no se puede mostrar y las plantillas de las descripciones accesibles de la card y del mapa del detalle, con las claves de pantalla que define el steering.
  - Incluir el texto de regularidad sin datos y su versión accesible («sin datos»).
  - Listo cuando `./gradlew :app:assembleDebug` compila y todos los textos existen como recursos, sin cadenas fijas en el código.
  - _Requirements: 6.1_

- [x] 1.2 (P) Marca de estado del recorrido
  - Crear la píldora con texto para «Incompleto», «Demo» y «Meta cumplida», con los colores del sistema de diseño y 24 dp de alto, sin depender del color para comunicar el estado.
  - Escribir una preview por variante y un test de UI que verifique el texto visible de cada una.
  - Listo cuando el test de UI de las tres marcas pasa y cada marca muestra su texto.
  - _Boundary: StatusBadge_
  - _Depends: 1.1_
  - _Requirements: 1.5, 1.6, 3.7, 6.4_

- [x] 1.3 (P) Formato de valores del recorrido
  - Crear el formateador puro en `es-AR` para kilómetros (`5,2 km`), duración (`HH:MM:SS`), velocidad promedio, regularidad (`80 %`, y «–» si no hay dato) y fecha corta (`Mar 28 oct`, con la primera letra en mayúscula y sin puntos de abreviatura).
  - Escribir primero los tests unitarios con los casos de valores enteros, con decimales, cero, regularidad nula y una fecha fija.
  - Listo cuando los tests del formateador pasan con `./gradlew :app:testDebugUnitTest`.
  - _Boundary: RunValueFormatter_
  - _Requirements: 1.3, 6.2_

- [x] 1.4 (P) Armado común del mapa
  - Extraer de la capa del mapa de Actividad la creación del estado del mapa (tiles, zoom inicial y límites, cierre al salir) a una pieza reutilizable que no incluya el pin, los botones ni el trazo en vivo.
  - Hacer que el mapa de Actividad use esa pieza sin cambiar su comportamiento ni su aspecto.
  - Listo cuando los tests existentes del mapa siguen pasando y Actividad se ve y se comporta igual (mismos tiles, atribución, zoom y centrado).
  - _Boundary: TrazoMapState, MapLayer_
  - _Requirements: 3.2, 5.1_

- [x] 1.5 (P) Totales de recorridos guardados
  - Crear el cálculo puro de totales (cantidad, kilómetros y tiempo) sobre la lista de recorridos guardados: suma los completos y los incompletos, deja afuera los demo y suma el tiempo registrado de cada recorrido, que ya excluye las pausas personales.
  - Escribir primero los tests unitarios: lista vacía (ceros), solo incompletos, mezcla con demo completos e incompletos, y orden de la lista sin efecto.
  - Listo cuando los tests de totales pasan y una lista vacía o solo con demo devuelve cantidad, distancia y tiempo en cero.
  - _Boundary: SavedRunTotals_
  - _Requirements: 2.3, 4.1, 4.2, 4.3, 4.4_

- [ ] 2. Mis recorridos y Mis estadísticas en Perfil (HU-09, HU-16)
- [x] 2.1 Estado y ViewModel de Perfil con los recorridos guardados
  - Escribir primero los tests unitarios con un repositorio falso: lista vacía, recorridos del más reciente al más antiguo, recorrido nuevo que queda primero, totales actualizados al agregar, y marcas Incompleto y Demo, incluidas juntas.
  - Hacer que el ViewModel observe los recorridos guardados (solo lectura), arme las cards con su descripción accesible y los totales, y que «hay recorridos» se derive de la lista.
  - Dejar anotado en el código y en los tests que la persistencia al cerrar la app depende de `save-run`: el ViewModel muestra lo que el repositorio emita.
  - Listo cuando los tests del ViewModel pasan y el estado refleja lista, marcas y totales para los casos descritos.
  - _Boundary: ProfileViewModel, ProfileUiState_
  - _Depends: 1.1, 1.3, 1.5_
  - _Requirements: 1.1, 1.2, 1.3, 1.5, 1.6, 1.7, 1.8, 2.5, 4.1, 4.5_

- [ ] 2.2 (P) Card de recorrido
  - Crear la card con título en una sola línea y puntos suspensivos si no entra, fecha, kilómetros, duración, frenadas, regularidad y las marcas que correspondan.
  - Unir su contenido en una sola descripción accesible con todos los datos y las marcas, y dar a toda la card un área táctil de al menos 48 dp que avise al tocarla.
  - Escribir tests de UI: título largo en una línea, marcas con texto, descripción accesible completa, toque que dispara el aviso y fuente al 200 % legible.
  - Listo cuando los tests de UI de la card pasan.
  - _Boundary: RunCard_
  - _Depends: 1.2_
  - _Requirements: 1.3, 1.4, 1.5, 1.6, 3.1, 5.8, 6.3, 6.4, 6.5, 6.7_

- [ ] 2.3 Pantalla de Perfil con la lista y los totales
  - Reemplazar la columna con scroll por una lista perezosa con «Mis estadísticas», el título «Mis recorridos» y las cards.
  - Mostrar el estado vacío (ilustración con «Todavía no hay recorridos», leído también por el lector de pantalla) solo mientras no hay recorridos, y los totales en cero en ese caso. Al llegar el primer recorrido, el estado vacío desaparece y aparece la card.
  - Recibir el aviso de toque sobre una card hacia afuera, sin abrir nada todavía.
  - Actualizar el test de contenido de Perfil existente al nuevo estado y cubrir: estado vacío, totales en cero, lista con cards y fuente al 200 %.
  - Listo cuando los tests de UI de Perfil pasan y la vista previa muestra los dos estados (vacío y con recorridos).
  - _Boundary: ProfileScreen, ProfileContent_
  - _Depends: 2.1, 2.2_
  - _Requirements: 1.1, 1.2, 2.1, 2.2, 2.3, 2.4, 2.5, 4.1, 4.6, 6.7_

- [ ] 3. Detalle del recorrido guardado (HU-09)
- [ ] 3.1 Estado y ViewModel del detalle
  - Escribir primero los tests unitarios: abrir con un id inexistente no abre nada, abrir muestra los datos del recorrido, cerrar lo deja en nada, las fotos se recortan a 2 y las listas vacías de destacados y fotos quedan vacías.
  - Crear el estado del detalle (título, velocidad promedio, meta, estado, marca Demo, destacados, fotos, traza y descripción accesible del mapa) y el ViewModel que lo arma a partir del recorrido elegido y lo cierra si el recorrido desaparece.
  - Listo cuando los tests del ViewModel del detalle pasan.
  - _Boundary: RunDetailViewModel_
  - _Depends: 1.1, 1.3_
  - _Requirements: 3.1, 3.2, 3.4, 3.5, 3.6, 3.7, 6.6_

- [ ] 3.2 (P) Mapa de solo lectura con la traza encuadrada
  - Crear el mapa del detalle sobre el armado común: dibuja la traza completa, la encuadra con su rectángulo (agregando a la utilidad de proyección el cálculo de encuadre), sin pin, sin botones y sin seguir al corredor, con atribución del proveedor de tiles.
  - Si la traza tiene menos de 2 puntos, mostrar el texto alternativo en lugar del mapa. Darle al mapa una descripción accesible con el resumen del recorrido.
  - Listo cuando una traza de prueba se ve completa dentro del recuadro, una de menos de 2 puntos muestra el texto alternativo y el test de UI de ambos casos pasa.
  - _Boundary: RunTraceMap, WebMercator_
  - _Depends: 1.4_
  - _Requirements: 3.2, 3.9, 6.6_

- [ ] 3.3 (P) Destacados y fotos del detalle
  - Crear la lista de destacados (chips de solo lectura) y la fila de hasta 2 fotos cargadas desde su ruta, con recuadro neutro si una foto no carga.
  - Verificar con tests de UI que muestran los datos recibidos y que no ocupan espacio si no tienen contenido.
  - Listo cuando los tests de UI de destacados y de fotos pasan, incluido el caso de una foto que no carga.
  - _Boundary: HighlightChips, RunPhotos_
  - _Requirements: 3.3, 3.5_

- [ ] 3.4 Modal de detalle
  - Armar el modal a pantalla completa con barra superior (título y botón «Cerrar» de 48 dp), el mapa de la traza, la velocidad promedio, la meta, el estado, la marca Demo cuando corresponde y, solo si no están vacíos, los destacados y las fotos. Con scroll vertical y sin animaciones propias.
  - Escribir tests de UI del contenido: sin destacados no hay sección, sin fotos no hay sección, marca Demo visible, traza no mostrable con el resto del detalle visible, cierre que avisa y fuente al 200 % operable.
  - Listo cuando los tests de UI del modal pasan y la vista previa muestra un detalle completo y uno sin destacados ni fotos.
  - _Boundary: RunDetailDialog_
  - _Depends: 1.2, 3.1, 3.2, 3.3_
  - _Requirements: 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 6.3, 6.7, 6.8_

- [ ] 4. Integración de HU-09 en la app
- [ ] 4.1 Abrir y cerrar el detalle desde Perfil
  - Crear el ViewModel del detalle en el nivel de la app, mostrar el modal cuando hay un recorrido abierto y conectar el toque sobre una card de Perfil con «abrir» y el cierre con «cerrar».
  - Verificar con un test que tocar una card abre el detalle de ese recorrido y cerrarlo deja a Perfil como estaba.
  - Listo cuando, con un recorrido de prueba guardado, tocar su card abre el detalle correcto y cerrarlo vuelve a Perfil en la misma posición.
  - _Boundary: TrazoApp_
  - _Depends: 2.3, 3.4_
  - _Requirements: 3.1, 3.8, 5.8_

- [ ] 5. Trazas guardadas en el mapa de Actividad (HU-12, dibujo)
- [ ] 5.1 Estado de las trazas guardadas en el ViewModel del mapa
  - Escribir primero los tests unitarios con repositorios falsos: sin trazas con recorrido en curso, sin trazas con ubicación aproximada, sin trazas si no hay recorridos, con trazas cuando se cumplen las tres condiciones, y que al guardarse un recorrido aparece su traza.
  - Hacer que el ViewModel combine los recorridos guardados, el estado «hay recorrido en curso» y la lectura de ubicación (precisa solo con permiso preciso y ubicación activada) y exponga las trazas a dibujar, releyendo la ubicación cuando se le avise.
  - Listo cuando los tests del ViewModel del mapa pasan, los existentes siguen pasando y las trazas a dibujar quedan vacías salvo cuando se cumplen las tres condiciones.
  - _Boundary: LiveRouteTrackingViewModel, LiveRouteTrackingUiState_
  - _Requirements: 5.1, 5.3, 5.4, 5.5, 5.6_

- [ ] 5.2 Dibujar las trazas guardadas en la capa del mapa
  - Hacer que la capa del mapa reciba las trazas a dibujar y las pinte con el color y el ancho de trazo guardado del tema, una por recorrido, agregando las nuevas y quitando las que ya no están, sin tocar el trazo en vivo.
  - Listo cuando, con trazas de prueba, el mapa muestra todas con el estilo de traza guardada y las quita al vaciarse la lista.
  - _Boundary: MapLayer_
  - _Depends: 1.4, 5.1_
  - _Requirements: 5.1, 5.3, 5.5, 5.6_

- [ ] 5.3 Conectar el mapa de Actividad con las trazas y la ubicación
  - Pasar las trazas del estado a la capa del mapa y avisar al ViewModel que relea la ubicación cada vez que Actividad vuelve a primer plano.
  - Verificar con un test de la pantalla del mapa que, con trazas en el estado, la capa las recibe, y sin ellas no.
  - Listo cuando en un dispositivo con recorridos guardados y ubicación precisa Actividad muestra las trazas, desaparecen al iniciar un recorrido y vuelven, junto con la nueva, al guardar el recorrido.
  - _Boundary: MapScreen_
  - _Depends: 5.1, 5.2_
  - _Requirements: 5.1, 5.3, 5.4, 5.6, 5.7_

- [ ] 6. Abrir el detalle tocando una traza (HU-12, recortable)
- [ ] 6.1 (P) Búsqueda de la traza tocada
  - Crear la regla pura que dice si la traza de un recorrido pasa a una distancia dada de un punto y que elige, entre los que coinciden, el recorrido más reciente. Una traza de menos de 2 puntos nunca coincide.
  - Escribir primero los tests unitarios: punto sobre la traza, fuera de la tolerancia, traza de 1 punto, dos trazas cruzadas (gana la más reciente) y lista vacía.
  - Listo cuando los tests de la regla pasan.
  - _Boundary: SavedRunTraceHit_
  - _Requirements: 5.2, 5.9_

- [ ] 6.2 (P) Toque sobre el mapa con tolerancia en metros
  - Agregar a la utilidad de proyección la conversión inversa a latitud y longitud y el cálculo de metros por pixel al zoom actual.
  - Hacer que la capa del mapa avise cada toque con latitud, longitud y una tolerancia equivalente a 24 dp.
  - Listo cuando un test de proyección confirma que convertir y volver da el mismo punto y que un toque en el mapa informa coordenadas y tolerancia.
  - _Boundary: WebMercator, MapLayer_
  - _Depends: 5.2_
  - _Requirements: 5.2_

- [ ] 6.3 Resolver el toque y abrir el detalle
  - Escribir primero los tests unitarios del ViewModel del mapa: toque sobre una traza emite el aviso de abrir ese recorrido, toque lejos de toda traza no emite nada, toque sin trazas dibujadas no emite nada y, con varias coincidencias, se abre el más reciente.
  - Hacer que el ViewModel resuelva el toque con la regla del recorrido más reciente, emita el aviso, y que Actividad y la app lo conecten con el modal de detalle ya integrado.
  - Verificar que al cerrar el detalle se vuelve al mapa con las trazas intactas.
  - Listo cuando los tests pasan y, en un dispositivo, tocar una traza (o un cruce de dos) abre el detalle del recorrido correcto y cerrarlo deja el mapa como estaba.
  - _Boundary: LiveRouteTrackingViewModel, MapScreen, TrazoApp_
  - _Depends: 4.1, 5.3, 6.1, 6.2_
  - _Requirements: 5.2, 5.7, 5.9_

- [ ] 7. Validación
- [ ] 7.1 Revisión de calidad del conjunto
  - Correr de a uno `./gradlew :app:ktlintCheck`, `:app:lint`, `:app:testDebugUnitTest` y `:app:assembleDebug`, y corregir lo que aparezca.
  - Revisar que no queden textos fijos en el código, colores ni medidas sueltas, y que esta spec no llame a la escritura del repositorio.
  - Listo cuando los cuatro comandos terminan sin errores.
  - _Depends: 6.3_
  - _Requirements: 6.1, 6.2_

- [ ] 7.2 Pruebas de accesibilidad y recorrido completo
  - Agregar tests instrumentados de Perfil y del detalle con fuente al 200 % y con descripciones accesibles, y verificar el recorrido completo con un recorrido de prueba: lista, detalle, cierre, totales y, con la ubicación precisa, trazas en Actividad.
  - Probar con «Quitar animaciones» activado que abrir y cerrar el detalle no agrega animaciones propias.
  - Listo cuando `./gradlew :app:connectedDebugAndroidTest` pasa para las pantallas de esta spec.
  - _Depends: 7.1_
  - _Requirements: 6.3, 6.5, 6.6, 6.7, 6.8_
