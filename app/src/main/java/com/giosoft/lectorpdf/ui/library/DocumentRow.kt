package com.giosoft.lectorpdf.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.giosoft.lectorpdf.R
import com.giosoft.lectorpdf.data.db.DocumentEntity
import java.util.Locale

@Composable
fun DocumentRow(
    document: DocumentEntity,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit,
    onDeleteFromDevice: () -> Unit,
    onSaveToMisPdf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        // Borde sutil: en tema claro la tarjeta es blanca sobre un fondo casi
        // blanco, y sin el no se distinguiria donde acaba cada una.
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onOpen)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp),
            )

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    // En MEDIO y no al final: en los nombres de archivo lo que
                    // distingue a dos parecidos suele estar al final (fechas,
                    // numeros de factura) y la extension importa.
                    overflow = TextOverflow.MiddleEllipsis,
                )
                Spacer(Modifier.size(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // La ubicacion va primero: es lo que distingue dos
                    // documentos que se llaman igual pero estan en sitios
                    // distintos.
                    document.location?.let { location ->
                        Text(
                            text = location,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(
                            text = " · ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = document.sizeBytes.toReadableSize(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (document.pageCount > 0) {
                        Text(
                            text = " · ${document.pageCount} pág.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Aviso para los PDF que llegaron compartidos y cuyo acceso
                    // puede caducar: el usuario entiende por que a veces
                    // desaparecen en lugar de pensar que es un fallo.
                    if (!document.persistable) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = stringResource(R.string.temporary_access),
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            // Solo se marca la estrella cuando ya es favorito; anadirlo o
            // quitarlo vive en el menu, para dejar sitio al nombre.
            if (document.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = stringResource(R.string.action_unfavorite),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
            }

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_options),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (document.isFavorite) R.string.action_unfavorite
                                    else R.string.action_favorite,
                                ),
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (document.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                null,
                            )
                        },
                        onClick = { menuOpen = false; onToggleFavorite() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                        onClick = { menuOpen = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_share)) },
                        leadingIcon = { Icon(Icons.Default.Share, null) },
                        onClick = { menuOpen = false; onShare() },
                    )
                    if (!document.persistable) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_save_copy)) },
                            leadingIcon = { Icon(Icons.Default.SaveAlt, null) },
                            onClick = { menuOpen = false; onSaveToMisPdf() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_remove_from_history)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { menuOpen = false; onRemove() },
                    )
                    // El borrado real va separado por una linea y en rojo: es
                    // la unica accion de la app que destruye un archivo.
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.action_delete_device),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DeleteForever,
                                null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = { menuOpen = false; onDeleteFromDevice() },
                    )
                }
            }
        }
    }
}

private fun Long.toReadableSize(): String = when {
    this <= 0L -> "—"
    this < 1024 -> "$this B"
    this < 1024 * 1024 -> String.format(Locale.getDefault(), "%.0f KB", this / 1024.0)
    else -> String.format(Locale.getDefault(), "%.1f MB", this / (1024.0 * 1024.0))
}
