package com.giosoft.pdf.ui.scan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.giosoft.pdf.R
import com.giosoft.pdf.data.PublicDocuments
import com.giosoft.pdf.ui.components.AppDialog
import com.giosoft.pdf.ui.components.AppTextField

/**
 * Que hacer con el PDF recien escaneado.
 *
 * El caso habitual (ponerle nombre y guardarlo en "Mis PDF") se resuelve sin
 * salir de la app; quien necesite otra carpeta sigue teniendo el selector del
 * sistema a un toque. El nombre viene preseleccionado para poder sustituirlo
 * de una sola pulsacion.
 */
@Composable
fun ScanSaveDialog(
    onSaveToMisPdf: (String) -> Unit,
    onChooseFolder: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val suggested = remember { PublicDocuments.defaultScanName() }
    var value by remember {
        mutableStateOf(TextFieldValue(suggested, selection = TextRange(0, suggested.length)))
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val name = value.text.trim()

    AppDialog(
        icon = Icons.Outlined.DocumentScanner,
        title = stringResource(R.string.scan_name_title),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.scan_save_here),
        confirmEnabled = name.isNotEmpty(),
        onConfirm = { onSaveToMisPdf(name) },
        dismissText = stringResource(R.string.action_cancel),
        dismissIsDestructive = true,
    ) {
        Column {
            AppTextField(
                value = value,
                onValueChange = { value = it },
                label = stringResource(R.string.rename_label),
                suffix = ".pdf",
                isError = name.isEmpty(),
                keyboardActions = KeyboardActions(
                    onDone = { if (name.isNotEmpty()) onSaveToMisPdf(name) },
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
            TextButton(
                onClick = { onChooseFolder(name) },
                enabled = name.isNotEmpty(),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Text(stringResource(R.string.scan_choose_folder))
            }
        }
    }
}
