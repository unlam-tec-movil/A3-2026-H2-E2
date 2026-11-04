package com.trazo.app.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trazo.app.ui.theme.Spacing
import com.trazo.app.ui.theme.TrazoTheme

private val ChipHeight = 40.dp

@Composable
fun GoalChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier.height(ChipHeight),
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    )
}

@Preview(showBackground = true)
@Composable
private fun GoalChipPreview() {
    TrazoTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            GoalChip(label = "3 km", selected = false, onClick = {})
            GoalChip(label = "5 km", selected = true, onClick = {})
            GoalChip(label = "10 km", selected = false, onClick = {})
        }
    }
}
