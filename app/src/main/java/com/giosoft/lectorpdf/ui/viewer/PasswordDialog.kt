package com.giosoft.lectorpdf.ui.viewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.giosoft.lectorpdf.R

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
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val submit = {
        if (password.isNotEmpty() && !state.checking) {
            keyboard?.hide()
            onSubmit(password)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        title = { Text(stringResource(R.string.password_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.password_body, state.documentName),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password_label)) },
                    singleLine = true,
                    isError = state.previousAttemptFailed,
                    supportingText = if (state.previousAttemptFailed) {
                        { Text(stringResource(R.string.password_wrong)) }
                    } else {
                        null
                    },
                    visualTransformation = if (visible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                imageVector = if (visible) hideIcon else showIcon,
                                contentDescription = stringResource(
                                    if (visible) R.string.password_hide else R.string.password_show,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = password.isNotEmpty() && !state.checking) {
                if (state.checking) {
                    CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.action_accept))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private val showIcon: ImageVector get() = Icons.Outlined.Visibility
private val hideIcon: ImageVector get() = Icons.Outlined.VisibilityOff
