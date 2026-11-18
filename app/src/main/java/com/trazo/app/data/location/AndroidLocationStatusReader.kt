package com.trazo.app.data.location

import android.content.Context
import com.trazo.app.domain.model.LocationStatus
import com.trazo.app.domain.repository.LocationStatusReader
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Adaptador de [LocationStatusReader] que le pregunta a Android con [readLocationStatus]. */
class AndroidLocationStatusReader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : LocationStatusReader {
        override fun read(): LocationStatus = readLocationStatus(context)
    }
