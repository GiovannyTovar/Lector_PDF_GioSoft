package com.giosoft.pdf.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.giosoft.pdf.R

/**
 * El ojo que muestra u oculta una contrasena escrita.
 *
 * Escribir a ciegas una contrasena que no se puede comprobar es la forma mas
 * facil de aplicarle a un PDF una contrasena con una errata, y ahi no hay
 * vuelta atras: nadie la puede recuperar. Va en todos los campos de contrasena
 * de la app, y de ahi que este aqui y no repetido en cada dialogo.
 */
@Composable
fun PasswordVisibilityIcon(visible: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = stringResource(
                if (visible) R.string.password_hide else R.string.password_show,
            ),
        )
    }
}
