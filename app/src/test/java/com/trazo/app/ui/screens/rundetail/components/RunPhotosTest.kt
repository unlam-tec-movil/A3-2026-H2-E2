package com.trazo.app.ui.screens.rundetail.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.app.R
import com.trazo.app.ui.theme.TrazoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/** Fotos del detalle: hasta 2, cargadas desde su ruta (requisitos 3.5 y 3.6). */
@RunWith(AndroidJUnit4::class)
class RunPhotosTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun show(photoPaths: List<String>) {
        composeTestRule.setContent {
            TrazoTheme {
                Box(modifier = Modifier.testTag("host")) { RunPhotos(photoPaths = photoPaths) }
            }
        }
    }

    private fun photo(name: String): String {
        val file = File(folder.root, name)
        FileOutputStream(file).use { Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file.absolutePath
    }

    private fun description(
        index: Int,
        count: Int,
    ) = context.getString(R.string.run_detail_photo_description, index, count)

    @Test
    fun `muestra el titulo y las dos fotos`() {
        show(listOf(photo("1.png"), photo("2.png")))

        composeTestRule.onNodeWithText(context.getString(R.string.run_detail_photos_title)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(description(1, 2)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(description(2, 2)).assertIsDisplayed()
    }

    @Test
    fun `con una sola foto muestra solo esa`() {
        show(listOf(photo("1.png")))

        composeTestRule.onNodeWithContentDescription(description(1, 1)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(description(2, 1)).assertDoesNotExist()
    }

    @Test
    fun `si llegan mas de dos fotos muestra todas las que tiene el recorrido sin romper la fila`() {
        show(listOf(photo("1.png"), photo("2.png"), photo("3.png")))

        composeTestRule.onNodeWithContentDescription(description(1, 3)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(description(2, 3)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(description(3, 3)).assertIsDisplayed()
    }

    @Test
    fun `sin fotos no ocupa espacio ni muestra el titulo`() {
        show(emptyList())

        composeTestRule.onNodeWithTag("host").assertHeightIsEqualTo(0.dp)
        composeTestRule.onNodeWithText(context.getString(R.string.run_detail_photos_title)).assertDoesNotExist()
    }

    @Test
    fun `una foto que no carga deja un recuadro neutro y no rompe la fila`() {
        show(listOf("/ruta/que/no/existe.jpg", photo("2.png")))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(description(1, 2)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(description(2, 2)).assertIsDisplayed()
    }
}
