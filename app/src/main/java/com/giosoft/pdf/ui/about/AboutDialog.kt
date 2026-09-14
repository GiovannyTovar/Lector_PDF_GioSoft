package com.giosoft.pdf.ui.about

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.giosoft.pdf.BuildConfig
import com.giosoft.pdf.R
import com.giosoft.pdf.ui.components.AppDialog

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
    AppDialog(
        icon = when (section) {
            AboutSection.ABOUT -> Icons.Outlined.PictureAsPdf
            AboutSection.PRIVACY -> Icons.Outlined.Shield
        },
        title = stringResource(
            when (section) {
                AboutSection.ABOUT -> R.string.about_title
                AboutSection.PRIVACY -> R.string.about_privacy_title
            },
        ),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.about_close),
        onConfirm = onDismiss,
    ) {
        Column(
            modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
        ) {
            when (section) {
                AboutSection.ABOUT -> AboutContent()
                AboutSection.PRIVACY -> PrivacyContent()
            }
        }
    }
}

@Composable
private fun AboutContent() {
    // La frase que resume la app va primero y centrada: es lo que se lee
    // aunque no se siga leyendo nada mas.
    Text(
        text = stringResource(R.string.about_tagline),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.size(20.dp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(Modifier.padding(14.dp)) {
            InfoLine(
                label = stringResource(R.string.about_version, "").trim(),
                value = BuildConfig.VERSION_NAME,
            )
            Spacer(Modifier.size(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.size(8.dp))
            InfoLine(
                label = stringResource(R.string.about_developer_label),
                value = stringResource(R.string.about_developed_by),
            )
        }
    }

    Spacer(Modifier.size(20.dp))

    Section(
        title = stringResource(R.string.about_why_title),
        body = stringResource(R.string.about_why_text),
    )
    Spacer(Modifier.size(16.dp))
    Section(
        title = stringResource(R.string.about_what_title),
        body = stringResource(R.string.about_what_text),
    )

    Spacer(Modifier.size(20.dp))
    Text(
        text = stringResource(R.string.about_thanks),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PrivacyContent() {
    Text(
        text = stringResource(R.string.privacy_tagline),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.size(20.dp))

    // Cada garantia con su icono: se hojean de un vistazo en vez de leerse
    // como un parrafo largo.
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PrivacyPoint(
            icon = Icons.Outlined.Lock,
            title = stringResource(R.string.privacy_point_permissions_title),
            body = stringResource(R.string.privacy_point_permissions_text),
        )
        PrivacyPoint(
            icon = Icons.Outlined.CloudOff,
            title = stringResource(R.string.privacy_point_network_title),
            body = stringResource(R.string.privacy_point_network_text),
        )
        PrivacyPoint(
            icon = Icons.Outlined.ContentCopy,
            title = stringResource(R.string.privacy_point_copies_title),
            body = stringResource(R.string.privacy_point_copies_text),
        )
        PrivacyPoint(
            icon = Icons.Outlined.DeleteOutline,
            title = stringResource(R.string.privacy_point_delete_title),
            body = stringResource(R.string.privacy_point_delete_text),
        )
        PrivacyPoint(
            icon = Icons.Outlined.Fingerprint,
            title = stringResource(R.string.privacy_point_lock_title),
            body = stringResource(R.string.privacy_point_lock_text),
        )
        PrivacyPoint(
            icon = Icons.Outlined.Security,
            title = stringResource(R.string.privacy_point_isolated_title),
            body = stringResource(R.string.privacy_point_isolated_text),
        )
    }
}

@Composable
private fun PrivacyPoint(icon: ImageVector, title: String, body: String) {
    Row {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.size(4.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
