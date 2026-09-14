package com.giosoft.pdf.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.giosoft.pdf.R
import com.giosoft.pdf.data.db.DocumentEntity
import com.giosoft.pdf.ui.components.AppDialog

/**
 * Explica QUE protege la huella antes de activarla.
 *
 * Es una proteccion de la app, no del archivo: quien reciba el PDF por
 * WhatsApp lo abrira sin que nadie le pida nada. Decirlo por adelantado evita
 * la confusion con "Poner contrasena al archivo", que si viaja con el
 * documento, y evita que alguien confie en una proteccion que no tiene.
 */
@Composable
fun LockInfoDialog(
    document: DocumentEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(
        icon = Icons.Outlined.Fingerprint,
        title = stringResource(R.string.lock_scope_title),
        supportingText = stringResource(R.string.lock_scope_intro, document.name),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.lock_scope_confirm),
        onConfirm = onConfirm,
        dismissText = stringResource(R.string.action_cancel),
    ) {
        Column {
            Punto(Icons.Outlined.Check, stringResource(R.string.lock_scope_yes_open))
            Punto(Icons.Outlined.Check, stringResource(R.string.lock_scope_yes_change))
            Punto(Icons.Outlined.Check, stringResource(R.string.lock_scope_yes_preview))
            Punto(Icons.Outlined.Info, stringResource(R.string.lock_scope_no_file))
            Punto(Icons.Outlined.Info, stringResource(R.string.lock_scope_no_other_apps))
        }
    }
}

/**
 * Una linea del alcance. El icono distingue lo que la proteccion SI hace de
 * lo que no llega a cubrir, para que no se lean como una lista uniforme.
 */
@Composable
private fun Punto(icon: ImageVector, text: String) {
    val cubre = icon == Icons.Outlined.Check
    Row {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (cubre) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.tertiary
            },
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.size(10.dp))
}
