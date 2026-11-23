package com.trazo.app.ui.screens.rundetail.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.R
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Mapa de solo lectura del detalle (requisitos 3.2, 3.9 y 6.6). */
@RunWith(AndroidJUnit4::class)
class RunTraceMapTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mapDescription = "Mapa con la traza del recorrido Vuelta al lago. Distancia 5,2 km, duración 00:55:00."

    private fun point(
        latitude: Double,
        longitude: Double,
    ) = RoutePoint(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = 5f,
        speedMetersPerSecond = 2f,
        monotonicTimestampMillis = 0L,
    )

    private fun show(routePoints: List<RoutePoint>) {
        composeTestRule.setContent {
            TrazoTheme {
                Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                    RunTraceMap(
                        routePoints = routePoints,
                        contentDescription = mapDescription,
                    )
                }
            }
        }
    }

    @Test
    fun `con una traza de dos puntos o mas muestra el mapa con su atribucion y su descripcion`() {
        show(listOf(point(-34.60, -58.40), point(-34.62, -58.38), point(-34.64, -58.36)))

        composeTestRule.onNodeWithContentDescription(mapDescription).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.map_attribution)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.run_detail_map_unavailable)).assertDoesNotExist()
    }

    @Test
    fun `el mapa de solo lectura no trae botones ni el pin del corredor`() {
        show(listOf(point(-34.60, -58.40), point(-34.64, -58.36)))

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.map_recenter)).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.map_your_location)).assertDoesNotExist()
    }

    @Test
    fun `con menos de 2 puntos muestra el texto alternativo en lugar del mapa`() {
        show(listOf(point(-34.60, -58.40)))

        composeTestRule.onNodeWithText(context.getString(R.string.run_detail_map_unavailable)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.map_attribution)).assertDoesNotExist()
    }

    @Test
    fun `sin puntos muestra el texto alternativo`() {
        show(emptyList())

        composeTestRule.onNodeWithText(context.getString(R.string.run_detail_map_unavailable)).assertIsDisplayed()
    }
}
