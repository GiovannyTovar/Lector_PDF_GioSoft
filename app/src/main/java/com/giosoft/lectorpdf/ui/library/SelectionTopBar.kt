package com.giosoft.lectorpdf.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.giosoft.lectorpdf.R
import com.giosoft.lectorpdf.ui.theme.FavoriteGold

/**
 * Barra que sustituye a la normal mientras hay documentos seleccionados.
 *
 * Reemplazarla entera, en vez de anadir botones a la de siempre, deja claro
 * que la app esta en otro modo y que las acciones afectan a varios documentos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    count: Int,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    onMoveToCategory: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onRemove: () -> Unit,
    isDark: Boolean,
) {
    val container = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.primary
    }
    val content = if (isDark) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.selection_count, count),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onClear, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Outlined.Close, stringResource(R.string.selection_clear))
            }
        },
        actions = {
            IconButton(onClick = onMoveToCategory, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Folder, stringResource(R.string.move_to_category))
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = stringResource(R.string.action_favorite),
                    tint = FavoriteGold,
                )
            }
            IconButton(onClick = onShare, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Share, stringResource(R.string.action_share))
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Outlined.Delete,
                    stringResource(R.string.action_remove_from_history),
                )
            }
            IconButton(onClick = onSelectAll, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.DoneAll, stringResource(R.string.selection_all))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = container,
            titleContentColor = content,
            navigationIconContentColor = content,
            actionIconContentColor = content,
        ),
    )
}
