package com.trazo.app.domain.repository

import com.trazo.app.domain.model.LocationStatus

/** Informa en qué estado están el permiso y la ubicación del dispositivo antes de comenzar. */
fun interface LocationStatusReader {
    fun read(): LocationStatus
}
