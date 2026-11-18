package com.trazo.app.domain.repository

import com.trazo.app.domain.model.regularity.RegularityState
import kotlinx.coroutines.flow.StateFlow

interface RegularityRepository {
    val state: StateFlow<RegularityState>
}
