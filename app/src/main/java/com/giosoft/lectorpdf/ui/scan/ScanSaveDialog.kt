package com.giosoft.lectorpdf.ui.scan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
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
import com.giosoft.lectorpdf.data.PublicDocuments

/**
 * Que hacer con el PDF recien escaneado.
 *
 * El caso habitual (ponerle nombre y guardarlo en "Mis PDF") se resuelve sin
 * salir de la app; quien necesite otra carpeta sigue teniendo el selector del
 * sistema a un toque.
 */
@Composable
fun ScanSaveDialog(
    onSaveToMisPdf: (String) -> Unit,
    onChooseFolder: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val suggested = remember { PublicDocuments.defaultScanName() }
    var value by remember {
        mutableStateOf(TextFieldValue(suggested, selection = TextRange(suggested.length)))
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val name = value.text.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.DocumentScanner, contentDescription = null) },
        title = { Text(stringResource(R.string.scan_name_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.rename_label)) },
                    singleLine = true,
                    suffix = { Text(".pdf") },
                    isError = name.isBlank(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (name.isNotBlank()) onSaveToMisPdf(name) },
                    ),
                    modifier = Modifier.focusRequester(focusRequester),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.scan_save_hint, PublicDocuments.displayPath),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                // Va dentro del cuerpo para dejar libres los dos botones de
                // abajo: guardar y, sobre todo, cancelar.
                TextButton(
                    onClick = { onChooseFolder(name) },
                    enabled = name.isNotBlank(),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Text(stringResource(R.string.scan_choose_folder))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSaveToMisPdf(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.scan_save_here))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
