package de.ruff.dimension.ui.theme

import android.graphics.Typeface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.ruff.dimension.data.AdminAppearance
import java.io.File

/**
 * Theme von Ruff: festes dunkles Farbschema, Cinzel/Alegreya.
 *
 * Dynamic Color (Material You) ist bewusst aus: Die Farben aus dem Hintergrundbild
 * des Geräts würden die Dark-Fantasy-Identität und die auf die Bildelemente
 * (Rahmen, Leisten, Klinge) abgestimmten Kontraste überschreiben. Es gibt
 * absichtlich auch kein helles Schema; die Bildelemente sind für dunklen Grund gemacht.
 */
@Composable
internal fun RuffTheme(
    appearance: AdminAppearance = AdminAppearance(),
    content: @Composable () -> Unit,
) {
    val accent = parseColor(appearance.accentColor, Hoellenrot)
    val frame = parseColor(appearance.frameColor, Color(0xFF8B5A3C))
    val radius = appearance.cornerRadiusDp.coerceIn(0f, 48f).dp
    val colors = RuffDarkColors.copy(primary = accent, primaryContainer = accent, secondary = accent, outline = frame)
    val shapes =
        Shapes().copy(
            extraSmall = RoundedCornerShape(radius / 3),
            small = RoundedCornerShape(radius / 2),
            medium = RoundedCornerShape(radius),
            large = RoundedCornerShape(radius),
            extraLarge = RoundedCornerShape(radius),
        )
    val uploadedFont =
        remember(appearance.fontPath) {
            runCatching {
                if (appearance.fontPath.isBlank() || !File(appearance.fontPath).isFile) {
                    null
                } else {
                    androidx.compose.ui.text.font
                        .FontFamily(Typeface.createFromFile(appearance.fontPath))
                }
            }.getOrNull()
        }
    MaterialTheme(
        colorScheme = colors,
        typography = uploadedFont?.let { RuffTypography.withFont(it) } ?: RuffTypography,
        shapes = shapes,
        content = content,
    )
}

private fun parseColor(
    value: String,
    fallback: Color,
): Color =
    runCatching { Color(android.graphics.Color.parseColor(value)) }
        .getOrDefault(fallback)
