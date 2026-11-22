package com.trazo.app.ui.screens.rundetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.trazo.app.R
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme
import java.io.File

private const val MAX_PHOTOS = 2

/**
 * Sección «Fotos» del detalle: hasta 2 fotos, cargadas desde su ruta de archivo. Si una no carga (o mientras
 * carga) queda un recuadro neutro y la otra se ve igual. Con la lista vacía no compone nada: ni título ni espacio.
 */
@Composable
fun RunPhotos(
    photoPaths: List<String>,
    modifier: Modifier = Modifier,
) {
    val photos = photoPaths.take(MAX_PHOTOS)
    if (photos.isEmpty()) return

    val neutral = ColorPainter(MaterialTheme.colorScheme.surfaceContainer)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.run_detail_photos_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            photos.forEachIndexed { index, path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = stringResource(R.string.run_detail_photo_description, index + 1, photos.size),
                    contentScale = ContentScale.Crop,
                    placeholder = neutral,
                    error = neutral,
                    modifier =
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.medium),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RunPhotosPreview() {
    TrazoTheme {
        // Rutas que no existen: la preview muestra los recuadros neutros.
        RunPhotos(
            photoPaths = listOf("/foto-1.jpg", "/foto-2.jpg"),
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}
