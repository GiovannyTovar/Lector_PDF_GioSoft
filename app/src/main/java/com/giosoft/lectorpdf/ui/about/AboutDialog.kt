package com.giosoft.lectorpdf.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.giosoft.lectorpdf.BuildConfig
import com.giosoft.lectorpdf.R

/** Las dos secciones que ofrece el menu de la barra superior. */
enum class AboutSection { ABOUT, PRIVACY }

/**
 * Un unico dialogo que muestra una seccion u otra.
 *
 * Se reutiliza el mismo componente en lugar de duplicarlo: solo cambian el
 * icono, el titulo y el cuerpo.
 */
@Composable
fun AboutDialog(
    section: AboutSection,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = when (section) {
                    AboutSection.ABOUT -> Icons.Outlined.PictureAsPdf
                    AboutSection.PRIVACY -> Icons.Outlined.Shield
                },
                contentDescription = null,
            )
        },
        title = {
            Text(
                stringResource(
                    when (section) {
                        AboutSection.ABOUT -> R.string.about_title
                        AboutSection.PRIVACY -> R.string.about_privacy_title
                    },
                ),
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when (section) {
                    AboutSection.ABOUT -> {
                        Text(
                            text = stringResource(
                                R.string.about_version,
                                BuildConfig.VERSION_NAME,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.about_developed_by),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.about_text),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    AboutSection.PRIVACY -> {
                        Text(
                            text = stringResource(R.string.about_privacy_text),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.about_close)) }
        },
    )
}
