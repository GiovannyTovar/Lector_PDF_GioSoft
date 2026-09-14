package com.giosoft.lectorpdf.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.giosoft.lectorpdf.R
import com.giosoft.lectorpdf.data.db.CategoryEntity

/**
 * Elige a que categoria va un documento (o varios, en seleccion multiple).
 * "Sin categoria" es siempre una opcion: quitar la etiqueta debe ser tan facil
 * como ponerla.
 */
@Composable
fun MoveToCategoryDialog(
    categories: List<CategoryEntity>,
    currentCategoryId: Long?,
    documentCount: Int,
    onConfirm: (Long?) -> Unit,
    onCreateCategory: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(currentCategoryId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
        title = {
            Text(
                if (documentCount > 1) {
                    stringResource(R.string.move_to_category_many, documentCount)
                } else {
                    stringResource(R.string.move_to_category)
                },
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                CategoryOption(
                    label = stringResource(R.string.filter_uncategorized),
                    color = null,
                    selected = selected == null,
                    onClick = { selected = null },
                )
                categories.forEach { category ->
                    CategoryOption(
                        label = category.name,
                        color = Color(category.colorArgb),
                        selected = selected == category.id,
                        onClick = { selected = category.id },
                    )
                }
                Spacer(Modifier.size(4.dp))
                TextButton(onClick = onCreateCategory) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.category_new))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text(stringResource(R.string.action_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun CategoryOption(
    label: String,
    color: Color?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        if (color != null) {
            Surface(modifier = Modifier.size(12.dp), shape = CircleShape, color = color) {}
            Spacer(Modifier.width(10.dp))
        } else {
            Icon(
                imageVector = Icons.Outlined.FolderOff,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Crear, renombrar y borrar categorias. */
@Composable
fun ManageCategoriesDialog(
    categories: List<CategoryEntity>,
    onCreate: (String) -> Unit,
    onRename: (CategoryEntity, String) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<CategoryEntity?>(null) }
    var confirmDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
        title = { Text(stringResource(R.string.categories_title)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (categories.isEmpty()) {
                    Text(
                        text = stringResource(R.string.categories_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                categories.forEach { category ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(12.dp),
                            shape = CircleShape,
                            color = Color(category.colorArgb),
                        ) {}
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { renaming = category }) {
                            Icon(
                                Icons.Outlined.DriveFileRenameOutline,
                                stringResource(R.string.action_rename),
                                Modifier.size(18.dp),
                            )
                        }
                        IconButton(onClick = { confirmDelete = category }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.action_delete_category),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.category_new)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newName.isNotBlank()) {
                                onCreate(newName)
                                newName = ""
                            }
                        },
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    onCreate(newName)
                                    newName = ""
                                }
                            },
                            enabled = newName.isNotBlank(),
                        ) {
                            Icon(Icons.Outlined.Add, stringResource(R.string.category_add))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.about_close)) }
        },
    )

    renaming?.let { category ->
        SimpleTextDialog(
            title = stringResource(R.string.category_rename),
            initial = category.name,
            onConfirm = {
                onRename(category, it)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }

    confirmDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            icon = {
                Icon(
                    Icons.Outlined.Delete,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(stringResource(R.string.category_delete_title)) },
            // Se aclara que no se pierde ningun documento: es la duda inmediata
            // al borrar algo que agrupa archivos.
            text = { Text(stringResource(R.string.category_delete_message, category.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(category)
                    confirmDelete = null
                }) {
                    Text(
                        text = stringResource(R.string.delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SimpleTextDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                isError = value.isBlank(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (value.isNotBlank()) onConfirm(value) },
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
