package com.giosoft.pdf.ui.viewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.giosoft.pdf.R
import com.giosoft.pdf.ui.components.AppDialog
import com.giosoft.pdf.ui.components.AppTextField
import com.giosoft.pdf.ui.components.PasswordVisibilityIcon

/**
 * Dialogo de contrasena propio de la app.
 *
 * En la version 4.x el dialogo lo imponia MuPDF y no se podia tocar. Aqui el
 * documento lo abre la app, asi que este dialogo es enteramente nuestro:
 * mensaje en espanol con el nombre del archivo, boton para revelar la
 * contrasena, foco y teclado automaticos, y error en linea al fallar.
 */
@Composable
fun PasswordDialog(
    state: ViewerUiState.PasswordRequired,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var visible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val canSubmit = password.text.isNotEmpty() && !state.checking
    val submit = {
        if (canSubmit) {
            keyboard?.hide()
            onSubmit(password.text)
        }
    }

    AppDialog(
        icon = Icons.Outlined.Lock,
        title = stringResource(R.string.password_title),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_accept),
        confirmEnabled = canSubmit,
        onConfirm = submit,
        dismissText = stringResource(R.string.action_cancel),
    ) {
        Column {
            Text(
                text = stringResource(R.string.password_body, state.documentName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            AppTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.password_label),
                isError = state.previousAttemptFailed,
                supportingText = if (state.previousAttemptFailed) {
                    stringResource(R.string.password_wrong)
                } else {
                    null
                },
                visualTransformation = if (visible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardActions = KeyboardActions(onDone = { submit() }),
                trailingIcon = { PasswordVisibilityIcon(visible) { visible = !visible } },
                modifier = Modifier.focusRequester(focusRequester),
            )
        }
    }
}
