package com.trazo.app.ui.screens.map

import com.trazo.app.data.repository.FakeLocationRepository
import com.trazo.app.data.repository.FakeSavedRunRepository
import com.trazo.app.data.repository.LocationRepository
import com.trazo.app.data.repository.RunSessionRepository
import com.trazo.app.data.repository.SavedRunRepository
import com.trazo.app.domain.usecases.testFinishRunUseCase
import com.trazo.app.domain.usecases.testRegularityMonitor

/**
 * Arma el ViewModel del Mapa para los tests, con fakes por defecto. Si el constructor suma un
 * parámetro, se agrega acá y no en cada archivo de test.
 */
fun testLiveRouteTrackingViewModel(
    runs: RunSessionRepository,
    location: LocationRepository = FakeLocationRepository(),
    savedRuns: SavedRunRepository = FakeSavedRunRepository(),
    sensorAvailable: Boolean = true,
): LiveRouteTrackingViewModel {
    val regularityMonitor = testRegularityMonitor(runs, sensorAvailable)
    return LiveRouteTrackingViewModel(
        runSessionRepository = runs,
        locationRepository = location,
        finishRun = testFinishRunUseCase(runs, regularityMonitor),
        savedRunRepository = savedRuns,
        regularityMonitor = regularityMonitor,
    )
}
