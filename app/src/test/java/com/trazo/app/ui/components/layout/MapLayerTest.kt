package com.trazo.app.ui.components.layout

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trazo.app.R
import com.trazo.app.domain.model.map.RoutePoint
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapLayerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var recenterClicks = 0

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

    private fun showMap(
        routePoints: List<RoutePoint> = emptyList(),
        currentLocation: RoutePoint? = null,
        centered: Boolean = true,
    ) {
        composeTestRule.setContent {
            TrazoTheme {
                MapLayer(
                    routePoints = routePoints,
                    currentLocation = currentLocation,
                    isMapCenteredOnRunner = centered,
                    onCenterMapClicked = { recenterClicks++ },
                    onMapDragged = {},
                    belowCenterButton = { Text("below-center") },
                    bottomContent = { Text("bottom-content") },
                )
            }
        }
    }

    @Test
    fun `given the map layer, when it is shown, then displays the attribution and the slots`() {
        // Given / When
        showMap()

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.map_attribution)).assertIsDisplayed()
        composeTestRule.onNodeWithText("below-center").assertIsDisplayed()
        composeTestRule.onNodeWithText("bottom-content").assertIsDisplayed()
    }

    @Test
    fun `given the map layer, when tapping recenter without location, then notifies the click`() {
        // Given
        showMap()

        // When
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.map_recenter)).performClick()

        // Then
        assertEquals(1, recenterClicks)
    }

    @Test
    fun `given a runner location, when tapping recenter, then notifies the click and keeps the map usable`() {
        // Given
        showMap(currentLocation = point(-34.6037, -58.3816))

        // When
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.map_recenter)).performClick()
        composeTestRule.waitForIdle()

        // Then
        assertEquals(1, recenterClicks)
    }

    @Test
    fun `given a route with several points, when the map is shown, then it draws the trace without failing`() {
        // Given
        val route = listOf(point(-34.6037, -58.3816), point(-34.6040, -58.3820), point(-34.6050, -58.3780))

        // When
        showMap(routePoints = route, currentLocation = route.last())
        composeTestRule.waitForIdle()

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.map_attribution)).assertIsDisplayed()
    }

    @Test
    fun `given a tap on the map, when resolved, then reports the coordinates and a 24 dp tolerance in meters`() {
        // Given
        val tapped = WebMercator.normalize(-34.6037, -58.3816)
        val scale = WebMercator.scaleForZoom(15)

        // When
        val tap = resolveMapTap(tapped.x, tapped.y, scale, density = 2f)

        // Then
        assertEquals(-34.6037, tap.latitude, 1e-7)
        assertEquals(-58.3816, tap.longitude, 1e-7)
        assertEquals(48.0 * WebMercator.metersPerPixel(-34.6037, scale), tap.toleranceMeters, 1e-9)
    }

    @Test
    fun `given a closer zoom, when a tap is resolved, then the tolerance in meters is smaller`() {
        // Given
        val point = WebMercator.normalize(-34.6037, -58.3816)

        // When
        val far = resolveMapTap(point.x, point.y, WebMercator.scaleForZoom(12), density = 2f)
        val near = resolveMapTap(point.x, point.y, WebMercator.scaleForZoom(16), density = 2f)

        // Then
        assertEquals(far.toleranceMeters / 16, near.toleranceMeters, 1e-9)
    }

    @Test
    fun `given a runner that is not followed, when the location changes, then the map is still displayed`() {
        // Given / When
        showMap(currentLocation = point(-34.6037, -58.3816), centered = false)
        composeTestRule.waitForIdle()

        // Then
        composeTestRule.onNodeWithText(context.getString(R.string.map_attribution)).assertIsDisplayed()
    }
}
