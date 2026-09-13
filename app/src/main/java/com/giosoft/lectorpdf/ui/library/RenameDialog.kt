package com.giosoft.lectorpdf.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.giosoft.lectorpdf.R
import com.giosoft.lectorpdf.data.db.DocumentEntity

/**
 * Renombra el archivo ORIGINAL en el celular, no una copia.
 *
 * Preselecciona solo el nombre sin la extension ".pdf", que es lo que el
 * usuario quiere cambiar; la extension se vuelve a anadir sola si se borra.
 */
@Composable
fun RenameDialog(
    document: DocumentEntity,
    canRename: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val baseName = document.name.removeSuffix(".pdf").removeSuffix(".PDF")
    var value by remember {
        mutableStateOf(TextFieldValue(baseName, selection = TextRange(0, baseName.length)))
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val confirm = { if (value.text.isNotBlank()) onConfirm(value.text.trim()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null) },
        title = { Text(stringResource(R.string.rename_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.rename_label)) },
                    singleLine = true,
                    enabled = canRename,
                    suffix = { Text(".pdf") },
                    isError = value.text.isBlank(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    modifier = Modifier.focusRequester(focusRequester),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        if (canRename) R.string.rename_explains else R.string.rename_failed,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (canRename) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = confirm, enabled = canRename && value.text.isNotBlank()) {
                Text(stringResource(R.string.action_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
