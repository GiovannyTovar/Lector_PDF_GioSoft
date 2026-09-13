package com.giosoft.lectorpdf.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.giosoft.lectorpdf.R
import com.giosoft.lectorpdf.data.db.DocumentEntity
import com.giosoft.lectorpdf.ui.theme.LocalIsDarkTheme
import com.giosoft.lectorpdf.ui.theme.PdfRed
import com.giosoft.lectorpdf.ui.theme.PdfRedContainerLight
import java.util.Locale

/** Acciones disponibles sobre un documento, agrupadas para no repetirlas. */
data class DocumentActions(
    val onOpen: (DocumentEntity) -> Unit,
    val onRename: (DocumentEntity) -> Unit,
    val onShare: (DocumentEntity) -> Unit,
    val onToggleFavorite: (DocumentEntity) -> Unit,
    val onRemove: (DocumentEntity) -> Unit,
    val onDeleteFromDevice: (DocumentEntity) -> Unit,
    val onSaveToMisPdf: (DocumentEntity) -> Unit,
)

/** Cada documento en su propia tarjeta blanca, con sombra muy suave. */
@Composable
fun DocumentCard(
    document: DocumentEntity,
    actions: DocumentActions,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 1.dp,
    ) {
        DocumentRow(document, actions)
    }
}

@Composable
private fun DocumentRow(
    document: DocumentEntity,
    actions: DocumentActions,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isDark = LocalIsDarkTheme.current

    // El rojo identifica el formato PDF en el tema claro. En oscuro se usa el
    // azul claro del tema, el mismo de la ruta.
    val iconColor = if (isDark) MaterialTheme.colorScheme.primary else PdfRed
    val iconBackground = if (isDark) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        PdfRedContainerLight
    }

    Row(
        modifier = Modifier
            .clickable { actions.onOpen(document) }
            .padding(start = 12.dp, end = 0.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color = iconBackground, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PictureAsPdf,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(21.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (document.isFavorite) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = stringResource(R.string.action_unfavorite),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    // En MEDIO y no al final: en los nombres de archivo lo que
                    // distingue a dos parecidos suele estar al final (fechas,
                    // numeros de factura) y la extension importa.
                    overflow = TextOverflow.MiddleEllipsis,
                )
            }

            Spacer(Modifier.size(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // La ubicacion resaltada: es lo que distingue dos documentos
                // que se llaman igual en sitios distintos.
                document.location?.let { location ->
                    Text(
                        text = location,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Dot()
                }
                Text(
                    text = document.sizeBytes.toReadableSize(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (document.pageCount > 0) {
                    Dot()
                    Text(
                        text = "${document.pageCount} pág.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!document.persistable) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = stringResource(R.string.temporary_access),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }

        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.cd_more_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            if (document.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                            null,
                        )
                    },
                    onClick = { menuOpen = false; actions.onToggleFavorite(document) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_rename)) },
                    leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, null) },
                    onClick = { menuOpen = false; actions.onRename(document) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_share)) },
                    leadingIcon = { Icon(Icons.Outlined.Share, null) },
                    onClick = { menuOpen = false; actions.onShare(document) },
                )
                if (!document.persistable) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_save_copy)) },
                        leadingIcon = { Icon(Icons.Outlined.SaveAlt, null) },
                        onClick = { menuOpen = false; actions.onSaveToMisPdf(document) },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_remove_from_history)) },
                    leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                    onClick = { menuOpen = false; actions.onRemove(document) },
                )
                // El borrado real va separado y en rojo: es la unica accion de
                // la app que destruye un archivo.
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
                            Icons.Outlined.DeleteForever,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = { menuOpen = false; actions.onDeleteFromDevice(document) },
                )
            }
        }
    }
}

/** Separador " · " de los metadatos, con el mismo estilo en todos. */
@Composable
private fun Dot() {
    Text(
        text = " · ",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
    )
}

private fun Long.toReadableSize(): String = when {
    this <= 0L -> "—"
    this < 1024 -> "$this B"
    this < 1024 * 1024 -> String.format(Locale.getDefault(), "%.0f KB", this / 1024.0)
    else -> String.format(Locale.getDefault(), "%.1f MB", this / (1024.0 * 1024.0))
}
