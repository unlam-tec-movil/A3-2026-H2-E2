package com.trazo.app.ui.screens.home.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.trazo.app.R
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

/**
 * Estado vacío genérico: una ilustración decorativa y un mensaje que lee el lector de pantalla.
 */
@Composable
fun EmptyState(
    @DrawableRes illustrationRes: Int,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(illustrationRes),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(Spacing.xl))
        Text(
            text = message,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStateRunsPreview() {
    TrazoTheme {
        EmptyState(
            illustrationRes = R.drawable.img_empty_runs,
            message = stringResource(R.string.home_empty_runs),
        )
    }
}
