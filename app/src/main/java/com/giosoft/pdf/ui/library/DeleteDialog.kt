package com.giosoft.pdf.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.giosoft.pdf.R
import com.giosoft.pdf.data.db.DocumentEntity
import com.giosoft.pdf.ui.components.AppDialog

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
    AppDialog(
        icon = Icons.Outlined.DeleteForever,
        iconTint = MaterialTheme.colorScheme.error,
        title = stringResource(R.string.delete_title),
        onDismiss = onDismiss,
        confirmText = stringResource(
            if (canDelete) R.string.delete_confirm else R.string.about_close,
        ),
        confirmIsDestructive = canDelete,
        onConfirm = { if (canDelete) onConfirm() else onDismiss() },
        dismissText = if (canDelete) stringResource(R.string.action_cancel) else null,
    ) {
        Text(
            text = if (canDelete) {
                stringResource(R.string.delete_message, document.name)
            } else {
                stringResource(R.string.delete_failed)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
