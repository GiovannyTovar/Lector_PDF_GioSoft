package com.giosoft.pdf.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Primero lo que la proteccion SI hace. Sus dos limites -no cifra el archivo y
 * no viaja con el- quedan tras "Ver mas", y tambien en Acerca de > Privacidad:
 * quien active la proteccion de verdad puede leerlos cuando quiera, pero no se
 * le regalan en pantalla a quien tome el celular ajeno y vea de un vistazo
 * hasta donde llega (y hasta donde no) lo que acaba de encontrarse.
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
        var detalle by remember { mutableStateOf(false) }

        Column {
            Punto(Icons.Outlined.Check, stringResource(R.string.lock_scope_yes_open))
            Punto(Icons.Outlined.Check, stringResource(R.string.lock_scope_yes_change))
            Punto(Icons.Outlined.Check, stringResource(R.string.lock_scope_yes_preview))

            if (detalle) {
                Punto(Icons.Outlined.Info, stringResource(R.string.lock_scope_no_file))
                Punto(Icons.Outlined.Info, stringResource(R.string.lock_scope_no_other_apps))
            } else {
                TextButton(
                    onClick = { detalle = true },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.lock_scope_more),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
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
