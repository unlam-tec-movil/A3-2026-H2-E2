package com.trazo.app.domain.repository

import com.trazo.app.domain.model.regularity.RegularityState
import kotlinx.coroutines.flow.StateFlow

/** Vigila el sensor durante el recorrido y expone la regularidad del piso. No guarda nada: es un monitor. */
interface RegularityMonitor {
    val state: StateFlow<RegularityState>
}
