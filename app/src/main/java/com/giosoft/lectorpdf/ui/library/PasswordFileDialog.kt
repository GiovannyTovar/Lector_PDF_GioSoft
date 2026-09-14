package com.giosoft.lectorpdf.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.giosoft.lectorpdf.R
import com.giosoft.lectorpdf.data.db.DocumentEntity
import com.giosoft.lectorpdf.ui.components.AppDialog
import com.giosoft.lectorpdf.ui.components.AppTextField

private const val MIN_PASSWORD_LENGTH = 4

/**
 * Pone o quita la contrasena que va DENTRO del archivo PDF.
 *
 * El texto explica la diferencia con el bloqueo por huella, porque es facil
 * confundirlos y las consecuencias al compartir son opuestas.
 */
@Composable
fun PasswordFileDialog(
    document: DocumentEntity,
    onApply: (currentPassword: String?, newPassword: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val removing = document.hasPassword

    var current by remember { mutableStateOf(TextFieldValue("")) }
    var nueva by remember { mutableStateOf(TextFieldValue("")) }
    var repetida by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    val corta = nueva.text.isNotEmpty() && nueva.text.length < MIN_PASSWORD_LENGTH
    val distintas = repetida.text.isNotEmpty() && nueva.text != repetida.text
    val valid = when {
        removing -> current.text.isNotEmpty()
        else -> nueva.text.length >= MIN_PASSWORD_LENGTH && nueva.text == repetida.text
    }

    AppDialog(
        icon = if (removing) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
        title = stringResource(
            if (removing) R.string.password_remove_title else R.string.password_set_title,
        ),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_accept),
        confirmEnabled = valid,
        confirmIsDestructive = removing,
        onConfirm = {
            if (removing) {
                onApply(current.text, null)
            } else {
                onApply(null, nueva.text)
            }
        },
        dismissText = stringResource(R.string.action_cancel),
    ) {
        Column {
            Text(
                text = stringResource(
                    if (removing) R.string.password_remove_explain
                    else R.string.password_set_explain,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))

            if (removing) {
                AppTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = stringResource(R.string.password_current),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.focusRequester(focusRequester),
                )
            } else {
                AppTextField(
                    value = nueva,
                    onValueChange = { nueva = it },
                    label = stringResource(R.string.password_new),
                    isError = corta,
                    supportingText = if (corta) {
                        stringResource(R.string.password_too_short)
                    } else {
                        null
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.focusRequester(focusRequester),
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = repetida,
                    onValueChange = { repetida = it },
                    label = stringResource(R.string.password_repeat),
                    isError = distintas,
                    supportingText = if (distintas) {
                        stringResource(R.string.password_mismatch)
                    } else {
                        null
                    },
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        }
    }
}
