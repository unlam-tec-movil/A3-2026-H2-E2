package com.trazo.app.domain.repository

import com.trazo.app.domain.model.stops.StopLocation

interface StopLocationProvider {
    /** Última ubicación conocida del recorrido; null si no hay fix. */
    fun lastKnown(): StopLocation?
}
