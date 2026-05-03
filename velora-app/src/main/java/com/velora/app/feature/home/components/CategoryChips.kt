package com.velora.app.feature.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velora.app.data.model.Category

@Composable
fun CategoryChips(
    categories: List<Category>,
    selected: Category,
    onSelect: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories, key = { it.name }) { category ->
            val isSelected = category == selected
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "chip_bg_${category.name}",
            )
            val labelColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                              else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "chip_text_${category.name}",
            )
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(category) },
                label = {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = labelColor,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = containerColor,
                    selectedContainerColor = containerColor,
                ),
                border = null,
                shape = MaterialTheme.shapes.extraLarge,
            )
        }
    }
}
