package com.giosoft.pdf.ui.viewer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.giosoft.pdf.R

/**
 * Contador de coincidencias y flechas para recorrerlas.
 *
 * El contador aparece solo cuando la busqueda ha terminado, para no mostrar
 * "0 de 0" mientras todavia se esta buscando y parecer que no hay resultados.
 */
@Composable
fun SearchNavigation(
    state: SearchState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when {
            state.searching -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
            }

            state.completed && !state.hasResults -> {
                Text(
                    text = stringResource(R.string.search_no_results),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            state.hasResults -> {
                Text(
                    text = stringResource(
                        R.string.search_match_of,
                        state.humanIndex,
                        state.total,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                IconButton(
                    onClick = onPrevious,
                    enabled = state.hasResults,
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        Icons.Outlined.KeyboardArrowUp,
                        stringResource(R.string.search_previous),
                    )
                }
                IconButton(
                    onClick = onNext,
                    enabled = state.hasResults,
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        Icons.Outlined.KeyboardArrowDown,
                        stringResource(R.string.search_next),
                    )
                }
            }
        }
        IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.Close, stringResource(R.string.search_close))
        }
    }
}
