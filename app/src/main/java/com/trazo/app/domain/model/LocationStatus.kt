package com.trazo.app.domain.model

/** Estado de la ubicación del dispositivo antes de comenzar un recorrido. */
enum class LocationStatus {
    /** Permiso preciso concedido y ubicación del dispositivo activada. */
    READY,

    /** El corredor no dio el permiso de ubicación. */
    PERMISSION_DENIED,

    /** El corredor solo dio ubicación aproximada y Trazo necesita la precisa. */
    APPROXIMATE_ONLY,

    /** La ubicación del dispositivo está desactivada. */
    DEVICE_LOCATION_OFF,
}
