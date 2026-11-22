# Requirements Document

## Project Description (Input)
Como corredor quiero una pantalla de Configuración donde ver el estado de los permisos y activar el modo demo, para resolver problemas de permisos y probar la app sin salir a correr.

## Introduction
En Trazo, los corredores necesitan conocer de forma clara el estado de los permisos necesarios para utilizar la aplicación (ubicación y cámara) y disponer de un medio para solucionar cualquier inconveniente de permisos directamente en los ajustes de Android. Asimismo, requieren la posibilidad de activar o desactivar un "Modo demo" para probar la aplicación de manera simulada sin necesidad de desplazarse físicamente. Esta funcionalidad agrega la pantalla "Configuración", accesible desde la barra superior de Perfil, donde se detallan el estado real de los permisos y la gestión del interruptor del modo demo.

## Boundary Context
- **In scope**:
  - La navegación hacia la pantalla "Configuración" al tocar el ícono de engranaje en la barra superior de Perfil.
  - La visualización del estado real de los permisos de ubicación ("Precisa", "Aproximada", "Denegada") y cámara ("Permitida", "Denegada").
  - La lectura del estado de los permisos a través de repositorios o lectores en la capa `data` (sin acceso directo desde la UI / ViewModel).
  - La acción del botón "Ir a configuración" para abrir la pantalla de ajustes de la aplicación en el sistema Android cuando un permiso figura como "Denegada" o "Aproximada".
  - La actualización automática del estado de los permisos al regresar a Trazo desde los ajustes del sistema.
  - El interruptor "Modo demo" habilitado cuando no hay un recorrido en curso, mostrando el aviso exacto "Modo demo activado: los recorridos son simulados" al activarlo, y su persistencia local.
  - El bloqueo del interruptor "Modo demo" cuando hay un recorrido en curso, mostrando exactamente el mensaje "Finalizá el recorrido para cambiar el modo demo".
- **Out of scope**:
  - La implementación del recorrido simulado o la generación de ubicaciones GPS falsas.
  - Cambios en el seguimiento GPS del recorrido.
  - Cambios en la detección de frenadas y pausas del recorrido.
  - Cambios en la finalización y visualización de resultados del recorrido.
  - Cambios en el guardado y formulario del recorrido.
  - La creación de cuentas de usuario o la sincronización con servidores en la nube (todos los datos se almacenan localmente).

## Requirements

### Requirement 1: Abrir Configuración
**Objective:** As a corredor, I want acceder a la pantalla de Configuración desde el ícono de engranaje en Perfil, so that pueda consultar los ajustes de la aplicación.

#### Acceptance Criteria
1. When el corredor toca el ícono de engranaje en la barra superior de Perfil, the Trazo App shall abrir la pantalla "Configuración".
2. The Trazo App shall mostrar en la pantalla "Configuración" la sección "Permisos".
3. The Trazo App shall mostrar en la pantalla "Configuración" la sección "Modo demo".
4. The Trazo App shall incluir en la barra superior de "Configuración" un botón para volver que permita regresar a la pantalla anterior.

### Requirement 2: Estado de los permisos
**Objective:** As a corredor, I want conocer el estado real de los permisos de ubicación y cámara, so that sepa si la aplicación puede medir mi recorrido o tomar fotografías.

#### Acceptance Criteria
1. The Trazo App shall consultar el estado del permiso de ubicación mediante una abstracción en la capa `data` (por ejemplo, `LocationRepository` / `LocationStatusReader`) que evalúe únicamente si los permisos de Android están concedidos.
2. When el permiso de ubicación precisa (`Manifest.permission.ACCESS_FINE_LOCATION`) está concedido, the Trazo App shall mostrar el estado de ubicación "Precisa".
3. When únicamente el permiso de ubicación aproximada (`Manifest.permission.ACCESS_COARSE_LOCATION`) está concedido, the Trazo App shall mostrar el estado de ubicación "Aproximada".
4. When no se ha concedido ningún permiso de ubicación, the Trazo App shall mostrar el estado de ubicación "Denegada".
5. The Trazo App shall consultar el estado del permiso de cámara mediante una abstracción en la capa `data` (por ejemplo, `CameraPermissionReader`).
6. When el permiso de cámara (`Manifest.permission.CAMERA`) está concedido, the Trazo App shall mostrar el estado de cámara "Permitida".
7. When el permiso de cámara no está concedido, the Trazo App shall mostrar el estado de cámara "Denegada".

### Requirement 3: Ir a la configuración del sistema
**Objective:** As a corredor, I want un botón que me lleve a los ajustes de Android cuando un permiso falta o es aproximado, so that pueda resolver el problema rápidamente.

#### Acceptance Criteria
1. When el estado del permiso de ubicación figura como "Denegada" o "Aproximada", the Trazo App shall mostrar el botón "Ir a configuración" junto al estado de ubicación.
2. When el estado del permiso de cámara figura como "Denegada", the Trazo App shall mostrar el botón "Ir a configuración" junto al estado de cámara.
3. When el corredor toca "Ir a configuración", the Trazo App shall abrir la pantalla de ajustes de la aplicación en el sistema Android.

### Requirement 4: Actualizar al volver de los ajustes
**Objective:** As a corredor, I want que la pantalla de Configuración refleje inmediatamente los cambios de permisos cuando vuelvo de los ajustes de Android, so that confirme que el problema se solucionó.

#### Acceptance Criteria
1. When el corredor vuelve a Trazo desde los ajustes del sistema Android, the Trazo App shall reevaluar inmediatamente el estado real de los permisos de ubicación y cámara mediante las fuentes de datos.
2. When el estado de los permisos cambia en los ajustes del sistema, the Trazo App shall actualizar las etiquetas de estado y ocultar o mostrar los botones "Ir a configuración" según corresponda.

### Requirement 5: Activar y desactivar el modo demo
**Objective:** As a corredor, I want activar el interruptor de Modo demo cuando no estoy corriendo, so that pueda probar la aplicación sin salir a la calle.

#### Acceptance Criteria
1. While no existe un recorrido en curso, the Trazo App shall mantener habilitado el interruptor "Modo demo".
2. When el corredor activa el interruptor "Modo demo", the Trazo App shall mostrar exactamente el aviso: "Modo demo activado: los recorridos son simulados".
3. When el corredor activa el interruptor "Modo demo", the Trazo App shall establecer el interruptor en estado activado.
4. When el corredor desactiva el interruptor "Modo demo", the Trazo App shall establecer el interruptor en estado desactivado y volver a utilizar el GPS real como modo seleccionado.
5. The Trazo App shall persistir localmente el valor del modo demo en la capa `data` para que pueda ser consultado desde otras pantallas de la aplicación.

### Requirement 6: Modo demo con recorrido en curso
**Objective:** As a corredor, I want que el modo demo no se pueda cambiar durante un recorrido, so that no se alteren las mediciones de mi carrera activa.

#### Acceptance Criteria
1. When existe un recorrido en curso (según el estado real de `RunSessionRepository.isRunInProgress`), the Trazo App shall deshabilitar el interruptor "Modo demo".
2. When existe un recorrido en curso, the Trazo App shall mostrar exactamente el mensaje: "Finalizá el recorrido para cambiar el modo demo".
3. While el recorrido permanece en curso, the Trazo App shall impedir cualquier cambio en el estado del modo demo.
