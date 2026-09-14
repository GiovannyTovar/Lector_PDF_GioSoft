package com.giosoft.lectorpdf.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.giosoft.lectorpdf.R
import com.giosoft.lectorpdf.data.CATEGORY_COLORS
import com.giosoft.lectorpdf.data.db.CategoryEntity
import com.giosoft.lectorpdf.ui.components.AppDialog

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

    AppDialog(
        icon = Icons.Outlined.Folder,
        title = if (documentCount > 1) {
            stringResource(R.string.move_to_category_many, documentCount)
        } else {
            stringResource(R.string.move_to_category)
        },
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_accept),
        onConfirm = { onConfirm(selected) },
    ) {
        Column(
            modifier = Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
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
            TextButton(
                onClick = onCreateCategory,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.category_new))
            }
        }
    }
}

/** Fila seleccionable con el punto de color de la categoria. */
@Composable
private fun CategoryOption(
    label: String,
    color: Color?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (color != null) {
                Surface(modifier = Modifier.size(14.dp), shape = CircleShape, color = color) {}
            } else {
                Icon(
                    imageVector = Icons.Outlined.FolderOff,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Crear, renombrar, recolorear, reordenar y borrar categorias. */
@Composable
fun ManageCategoriesDialog(
    categories: List<CategoryEntity>,
    onCreate: (String, Int?) -> Unit,
    onRename: (CategoryEntity, String) -> Unit,
    onRecolor: (CategoryEntity, Int) -> Unit,
    onMove: (Int, Boolean) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    AppDialog(
        icon = Icons.Outlined.Folder,
        title = stringResource(R.string.categories_title),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.about_close),
        onConfirm = onDismiss,
    ) {
        Column(
            modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (categories.isEmpty()) {
                Text(
                    text = stringResource(R.string.categories_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.categories_reorder_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            categories.forEachIndexed { index, category ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    // Tocar la fila abre el editor de nombre y color: asi no
                    // hacen falta cuatro botones, que dejaban al nombre sin
                    // sitio y lo partian en dos lineas.
                    modifier = Modifier.clickable { editing = category },
                ) {
                    Row(
                        modifier = Modifier.padding(
                            start = 12.dp,
                            end = 2.dp,
                            top = 2.dp,
                            bottom = 2.dp,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(14.dp),
                            shape = CircleShape,
                            color = Color(category.colorArgb),
                        ) {}
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        SmallAction(
                            icon = Icons.Outlined.ArrowUpward,
                            description = stringResource(R.string.category_move_up),
                            enabled = index > 0,
                            onClick = { onMove(index, true) },
                        )
                        SmallAction(
                            icon = Icons.Outlined.ArrowDownward,
                            description = stringResource(R.string.category_move_down),
                            enabled = index < categories.lastIndex,
                            onClick = { onMove(index, false) },
                        )
                        SmallAction(
                            icon = Icons.Outlined.Delete,
                            description = stringResource(R.string.action_delete_category),
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { confirmDelete = category },
                        )
                    }
                }
            }

            TextButton(onClick = { creating = true }, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.category_new))
            }
        }
    }

    if (creating) {
        CategoryEditorDialog(
            title = stringResource(R.string.category_create_title),
            initialName = "",
            initialColor = CATEGORY_COLORS[categories.size % CATEGORY_COLORS.size],
            onConfirm = { name, color ->
                onCreate(name, color)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }

    editing?.let { category ->
        CategoryEditorDialog(
            title = stringResource(R.string.category_edit_title),
            initialName = category.name,
            initialColor = category.colorArgb,
            onConfirm = { name, color ->
                if (name != category.name) onRename(category, name)
                if (color != category.colorArgb) onRecolor(category, color)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    confirmDelete?.let { category ->
        AppDialog(
            icon = Icons.Outlined.Delete,
            iconTint = MaterialTheme.colorScheme.error,
            title = stringResource(R.string.category_delete_title),
            onDismiss = { confirmDelete = null },
            confirmText = stringResource(R.string.delete_confirm),
            confirmIsDestructive = true,
            onConfirm = {
                onDelete(category)
                confirmDelete = null
            },
            dismissText = stringResource(R.string.action_cancel),
        ) {
            // Se aclara que no se pierde ningun documento: es la duda inmediata
            // al borrar algo que agrupa archivos.
            Text(
                text = stringResource(R.string.category_delete_message, category.name),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SmallAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(18.dp),
            tint = if (enabled) tint else MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/** Nombre y color de una categoria, al crearla o al editarla. */
@Composable
private fun CategoryEditorDialog(
    title: String,
    initialName: String,
    initialColor: Int,
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember {
        mutableStateOf(
            TextFieldValue(initialName, selection = TextRange(0, initialName.length)),
        )
    }
    var color by remember { mutableStateOf(initialColor) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val name = value.text.trim()

    AppDialog(
        icon = Icons.Outlined.Folder,
        iconTint = Color(color),
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_accept),
        confirmEnabled = name.isNotEmpty(),
        onConfirm = { onConfirm(name, color) },
        dismissText = stringResource(R.string.action_cancel),
    ) {
        Column {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(stringResource(R.string.category_name_hint)) },
                singleLine = true,
                isError = name.isEmpty(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (name.isNotEmpty()) onConfirm(name, color) },
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )

            Spacer(Modifier.size(18.dp))

            Text(
                text = stringResource(R.string.category_color),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CATEGORY_COLORS.forEach { option ->
                    ColorSwatch(
                        color = Color(option),
                        selected = option == color,
                        onClick = { color = option },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(color, CircleShape)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
