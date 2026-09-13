package com.giosoft.lectorpdf.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.giosoft.lectorpdf.R
import com.giosoft.lectorpdf.data.db.DocumentEntity

/**
 * Confirmacion del unico borrado irreversible de la app.
 *
 * A diferencia de "quitar de la lista", esto no se puede deshacer, asi que el
 * mensaje dice exactamente que se pierde y el boton destructivo va en rojo.
 * Si el proveedor no permite borrar ([canDelete] a false), se explica en vez
 * de dejar al usuario intentarlo y fallar.
 */
@Composable
fun DeleteDialog(
    document: DocumentEntity,
    canDelete: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(R.string.delete_title)) },
        text = {
            Text(
                text = if (canDelete) {
                    stringResource(R.string.delete_message, document.name)
                } else {
                    stringResource(R.string.delete_failed)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            if (canDelete) {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = stringResource(R.string.delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(if (canDelete) R.string.action_cancel else R.string.about_close))
            }
        },
    )
}
