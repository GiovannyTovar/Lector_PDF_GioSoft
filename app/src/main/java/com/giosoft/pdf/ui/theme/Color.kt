package com.giosoft.pdf.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta derivada del azul de marca de la version 4.x (#0E314A).
// Se conserva la identidad visual de la app original.

val BrandNavy = Color(0xFF0E314A)
val BrandNavyDark = Color(0xFF0A2438)
val BrandNavyLight = Color(0xFF1D5077)

// El icono PDF va en rojo SOLO en el tema claro, que es donde ese rojo se
// reconoce. En oscuro se usa el azul claro del tema, el mismo de la ruta del
// documento, porque el rojo saturado sobre fondo negro resulta agresivo.
val PdfRed = Color(0xFFD93025)
/** Azul de la ubicacion del archivo en el tema claro. */
val LocationBlue = Color(0xFF1F547E)
/** Dorado de la estrella de favorito, igual en ambos temas. */
val FavoriteGold = Color(0xFFFFC107)
/** Fondo de una tarjeta seleccionada. Solido para que no se mezcle con nada. */
val SelectedCardLight = Color(0xFFE1E8EF)
val SelectedCardDark = Color(0xFF1E2A33)

/**
 * Resaltado de las coincidencias de busqueda.
 *
 * OPACOS y CLAROS a proposito: el visor multiplica el color sobre la pagina en
 * vez de mezclarlo, asi que un color con transparencia se premultiplica y sale
 * casi negro, tapando el texto. Con un tono claro, el fondo queda del color del
 * marcador y las letras siguen leyendose.
 */
val SearchHighlightActive = Color(0xFFFFAB40)
val SearchHighlightOther = Color(0xFFFFF59D)
val PdfRedContainerLight = Color(0x1FD93025)

// Claro
val LightPrimary = BrandNavy
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFCFE4F6)
val LightOnPrimaryContainer = Color(0xFF001D31)
val LightSecondary = Color(0xFF50606E)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD3E5F5)
val LightOnSecondaryContainer = Color(0xFF0C1D29)
val LightBackground = Color(0xFFF9F9F9)
val LightOnBackground = Color(0xFF3A3A42)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF3A3A42)
val LightSurfaceVariant = Color(0xFFDDE3EA)
val LightOnSurfaceVariant = Color(0xFF9A9AA4)
val LightOutline = Color(0xFFB8B8C0)
val LightOutlineVariant = Color(0xFFEDEDEF)
// Tonos de "contenedor": los usan menus, dialogos y hojas. Si no se definen,
// Material 3 cae en su paleta base, que tira a morado y desentona con el azul.
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF9F9F9)
val LightSurfaceContainer = Color(0xFFFFFFFF)
val LightSurfaceContainerHigh = Color(0xFFF4F7FA)
val LightSurfaceContainerHighest = Color(0xFFEDF1F6)
val LightSurfaceDim = Color(0xFFEDEDEE)
val LightSurfaceBright = Color(0xFFFFFFFF)
val LightInverseSurface = Color(0xFF2E3134)
val LightInverseOnSurface = Color(0xFFF0F1F4)

val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)

// Oscuro
val DarkPrimary = Color(0xFF9BCBF0)
val DarkOnPrimary = Color(0xFF003351)
val DarkPrimaryContainer = Color(0xFF004B73)
val DarkOnPrimaryContainer = Color(0xFFCFE4F6)
val DarkSecondary = Color(0xFFB7C9D9)
val DarkOnSecondary = Color(0xFF22323F)
val DarkSecondaryContainer = Color(0xFF384956)
val DarkOnSecondaryContainer = Color(0xFFD3E5F5)
val DarkBackground = Color(0xFF0D1012)
val DarkOnBackground = Color(0xFFE1E2E5)
val DarkSurface = Color(0xFF191D21)
val DarkOnSurface = Color(0xFFE1E2E5)
val DarkSurfaceVariant = Color(0xFF41484D)
val DarkOnSurfaceVariant = Color(0xFF8E939C)
val DarkOutline = Color(0xFF6B7078)
val DarkOutlineVariant = Color(0xFF2A3136)
val DarkSurfaceContainerLowest = Color(0xFF0D1012)
val DarkSurfaceContainerLow = Color(0xFF181C1F)
val DarkSurfaceContainer = Color(0xFF191D21)
val DarkSurfaceContainerHigh = Color(0xFF262B2E)
val DarkSurfaceContainerHighest = Color(0xFF313639)
val DarkSurfaceDim = Color(0xFF0D1012)
val DarkSurfaceBright = Color(0xFF363A3D)
val DarkInverseSurface = Color(0xFFE1E2E5)
val DarkInverseOnSurface = Color(0xFF2E3134)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
