package com.trazo.app.ui.components.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trazo.app.R
import com.trazo.app.domain.model.saved.RunBadge
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

/** Marcas de un recorrido guardado. Siempre se muestran con texto: el color solo refuerza. */
enum class StatusBadgeKind {
    GOAL_REACHED,
    INCOMPLETE,
    DEMO,
}

/** Mapea la marca de dominio a la que dibuja la UI. */
fun RunBadge.toStatusBadgeKind(): StatusBadgeKind =
    when (this) {
        RunBadge.GOAL_REACHED -> StatusBadgeKind.GOAL_REACHED
        RunBadge.INCOMPLETE -> StatusBadgeKind.INCOMPLETE
        RunBadge.DEMO -> StatusBadgeKind.DEMO
    }

private val BadgeMinHeight = 24.dp

/** Píldora de 24 dp (crece si la fuente del sistema es más grande) con «Meta cumplida», «Incompleto» o «Demo». */
@Composable
fun StatusBadge(
    kind: StatusBadgeKind,
    modifier: Modifier = Modifier,
) {
    val extended = TrazoTheme.extendedColors
    val (container, content) =
        when (kind) {
            StatusBadgeKind.GOAL_REACHED -> extended.successContainer to extended.onSuccessContainer
            StatusBadgeKind.INCOMPLETE -> extended.warningContainer to extended.onWarningContainer
            StatusBadgeKind.DEMO -> extended.infoContainer to extended.onInfoContainer
        }

    BadgeSurface(
        text = stringResource(kind.labelRes()),
        containerColor = container,
        contentColor = content,
        modifier = modifier,
    )
}

@Composable
private fun BadgeSurface(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = BadgeMinHeight),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Texto de la marca: lo usan la píldora y las descripciones accesibles que nombran las marcas. */
@StringRes
internal fun StatusBadgeKind.labelRes(): Int =
    when (this) {
        StatusBadgeKind.GOAL_REACHED -> R.string.profile_run_badge_goal_reached
        StatusBadgeKind.INCOMPLETE -> R.string.profile_run_badge_incomplete
        StatusBadgeKind.DEMO -> R.string.profile_run_badge_demo
    }

@Preview(showBackground = true, name = "Meta cumplida")
@Composable
private fun StatusBadgeGoalReachedPreview() {
    TrazoTheme { StatusBadge(kind = StatusBadgeKind.GOAL_REACHED) }
}

@Preview(showBackground = true, name = "Incompleto")
@Composable
private fun StatusBadgeIncompletePreview() {
    TrazoTheme { StatusBadge(kind = StatusBadgeKind.INCOMPLETE) }
}

@Preview(showBackground = true, name = "Demo")
@Composable
private fun StatusBadgeDemoPreview() {
    TrazoTheme { StatusBadge(kind = StatusBadgeKind.DEMO) }
}
