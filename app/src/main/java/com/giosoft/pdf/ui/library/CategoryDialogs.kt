package com.giosoft.pdf.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.giosoft.pdf.R
import com.giosoft.pdf.data.CATEGORY_COLORS
import com.giosoft.pdf.data.db.CategoryEntity
import com.giosoft.pdf.ui.components.AppDialog
import com.giosoft.pdf.ui.components.AppTextField
import com.giosoft.pdf.ui.theme.FavoriteGold

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
        dismissText = stringResource(R.string.action_cancel),
    ) {
        Column(
            modifier = Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
            TextButton(onClick = onCreateCategory, modifier = Modifier.padding(top = 4.dp)) {
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
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
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
    favoritesPosition: Int,
    onCreate: (String, Int?) -> Unit,
    onRename: (CategoryEntity, String) -> Unit,
    onRecolor: (CategoryEntity, Int) -> Unit,
    onMove: (Int, Boolean) -> Unit,
    onMoveFavorites: (Boolean) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    // Favoritos ocupa un hueco entre las categorias: la lista intercala ambas
    // cosas para que la numeracion salga correlativa y el usuario vea en que
    // posicion queda realmente cada ficha.
    val favoritesIndex = favoritesPosition.coerceIn(0, categories.size)

    AppDialog(
        icon = Icons.Outlined.Folder,
        title = stringResource(R.string.categories_title),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.about_close),
        onConfirm = onDismiss,
    ) {
        Column(
            modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.categories_reorder_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            var numero = 0
            repeat(categories.size + 1) { slot ->
                if (slot == favoritesIndex) {
                    numero++
                    FavoritesRow(
                        position = numero,
                        canMoveUp = favoritesIndex > 0,
                        canMoveDown = favoritesIndex < categories.size,
                        onMove = onMoveFavorites,
                    )
                } else {
                    // Cada paso muestra UNA fila: o Favoritos, o una categoria.
                    numero++
                    val categoryIndex = if (slot > favoritesIndex) slot - 1 else slot
                    val category = categories[categoryIndex]
                    CategoryRow(
                        position = numero,
                        category = category,
                        canMoveUp = categoryIndex > 0,
                        canMoveDown = categoryIndex < categories.lastIndex,
                        onMove = { up -> onMove(categoryIndex, up) },
                        onEdit = { editing = category },
                        onDelete = { confirmDelete = category },
                    )
                }
            }

            if (categories.isEmpty()) {
                Text(
                    text = stringResource(R.string.categories_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
            existingNames = categories.map { it.name },
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
            existingNames = categories.filter { it.id != category.id }.map { it.name },
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

/** Numero de orden, para que se entienda en que posicion queda cada ficha. */
@Composable
private fun PositionBadge(position: Int) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$position",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FavoritesRow(
    position: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PositionBadge(position)
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = FavoriteGold,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.favorites),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            SmallAction(
                icon = Icons.Outlined.ArrowUpward,
                description = stringResource(R.string.category_move_up),
                enabled = canMoveUp,
                onClick = { onMove(true) },
            )
            SmallAction(
                icon = Icons.Outlined.ArrowDownward,
                description = stringResource(R.string.category_move_down),
                enabled = canMoveDown,
                onClick = { onMove(false) },
            )
            // Favoritos no se renombra ni se borra; el hueco mantiene las filas
            // alineadas con las de categoria.
            Spacer(Modifier.width(40.dp))
        }
    }
}

@Composable
private fun CategoryRow(
    position: Int,
    category: CategoryEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        // Tocar la fila abre el editor de nombre y color.
        modifier = Modifier.clickable(onClick = onEdit),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PositionBadge(position)
            Spacer(Modifier.width(10.dp))
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
                enabled = canMoveUp,
                onClick = { onMove(true) },
            )
            SmallAction(
                icon = Icons.Outlined.ArrowDownward,
                description = stringResource(R.string.category_move_down),
                enabled = canMoveDown,
                onClick = { onMove(false) },
            )
            SmallAction(
                icon = Icons.Outlined.Delete,
                description = stringResource(R.string.action_delete_category),
                tint = MaterialTheme.colorScheme.error,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun SmallAction(
    icon: ImageVector,
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorDialog(
    title: String,
    initialName: String,
    initialColor: Int,
    existingNames: List<String>,
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
    // Se avisa mientras escribe, no al pulsar Aceptar: es menos frustrante.
    val duplicate = existingNames.any { it.equals(name, ignoreCase = true) }
    val valid = name.isNotEmpty() && !duplicate

    AppDialog(
        icon = Icons.Outlined.Folder,
        iconTint = Color(color),
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_accept),
        confirmEnabled = valid,
        onConfirm = { onConfirm(name, color) },
        dismissText = stringResource(R.string.action_cancel),
    ) {
        Column {
            AppTextField(
                value = value,
                onValueChange = { value = it },
                label = stringResource(R.string.category_name_hint),
                isError = duplicate,
                supportingText = if (duplicate) {
                    stringResource(R.string.category_duplicate)
                } else {
                    null
                },
                keyboardActions = KeyboardActions(onDone = { if (valid) onConfirm(name, color) }),
                modifier = Modifier.focusRequester(focusRequester),
            )

            Spacer(Modifier.size(18.dp))

            Text(
                text = stringResource(R.string.category_color),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(10.dp))
            // FlowRow y no Row: con 18 colores una sola fila se salia del
            // dialogo y recortaba los ultimos circulos.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
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
            .size(34.dp)
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape,
            )
            .padding(if (selected) 4.dp else 0.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
