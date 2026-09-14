package com.giosoft.lectorpdf.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.giosoft.lectorpdf.R
import com.giosoft.lectorpdf.data.db.DocumentEntity
import com.giosoft.lectorpdf.ui.components.AppDialog
import com.giosoft.lectorpdf.ui.components.AppTextField

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

    val name = value.text.trim()
    val valid = canRename && name.isNotEmpty()

    AppDialog(
        icon = Icons.Outlined.DriveFileRenameOutline,
        title = stringResource(R.string.rename_title),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_accept),
        confirmEnabled = valid,
        onConfirm = { onConfirm(name) },
        dismissText = stringResource(R.string.action_cancel),
    ) {
        Column {
            AppTextField(
                value = value,
                onValueChange = { value = it },
                label = stringResource(R.string.rename_label),
                enabled = canRename,
                suffix = ".pdf",
                isError = name.isEmpty(),
                keyboardActions = KeyboardActions(onDone = { if (valid) onConfirm(name) }),
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
    }
}
