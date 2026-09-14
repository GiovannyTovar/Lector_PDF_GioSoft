package com.giosoft.lectorpdf.ui.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.giosoft.lectorpdf.R
import com.giosoft.lectorpdf.data.db.CategoryEntity
import com.giosoft.lectorpdf.ui.theme.FavoriteGold

/** Que subconjunto del historial se esta mostrando. */
sealed interface LibraryFilter {
    data object All : LibraryFilter
    data object Favorites : LibraryFilter
    data object Uncategorized : LibraryFilter
    data class Category(val id: Long) : LibraryFilter
}

/**
 * Fila de filtros: Todos, Favoritos y una ficha por categoria.
 *
 * Se desplaza en horizontal para que quepan las que el usuario cree, sin
 * limitar cuantas puede tener.
 */
@Composable
fun CategoryBar(
    categories: List<CategoryEntity>,
    selected: LibraryFilter,
    hasUncategorized: Boolean,
    onSelect: (LibraryFilter) -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "todos") {
            Chip(
                label = stringResource(R.string.filter_all),
                selected = selected is LibraryFilter.All,
                onClick = { onSelect(LibraryFilter.All) },
            )
        }
        item(key = "favoritos") {
            Chip(
                label = stringResource(R.string.favorites),
                selected = selected is LibraryFilter.Favorites,
                dotColor = FavoriteGold,
                leadingIcon = Icons.Outlined.Star,
                onClick = { onSelect(LibraryFilter.Favorites) },
            )
        }
        items(categories, key = { it.id }) { category ->
            Chip(
                label = category.name,
                selected = selected == LibraryFilter.Category(category.id),
                dotColor = Color(category.colorArgb),
                onClick = { onSelect(LibraryFilter.Category(category.id)) },
            )
        }
        if (hasUncategorized) {
            item(key = "sin-categoria") {
                Chip(
                    label = stringResource(R.string.filter_uncategorized),
                    selected = selected is LibraryFilter.Uncategorized,
                    onClick = { onSelect(LibraryFilter.Uncategorized) },
                )
            }
        }
        item(key = "gestionar") {
            Chip(
                label = stringResource(R.string.categories_manage),
                selected = false,
                leadingIcon = Icons.Outlined.Add,
                onClick = onManage,
            )
        }
    }
}

@Composable
private fun Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    dotColor: Color? = null,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = CircleShape,
        leadingIcon = when {
            leadingIcon != null -> {
                { Icon(leadingIcon, null, Modifier.size(16.dp)) }
            }
            dotColor != null -> {
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(10.dp),
                            shape = CircleShape,
                            color = dotColor,
                            content = {},
                        )
                        Spacer(Modifier.width(2.dp))
                    }
                }
            }
            else -> null
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}
