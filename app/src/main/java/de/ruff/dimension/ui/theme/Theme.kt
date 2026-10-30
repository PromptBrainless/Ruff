package de.ruff.dimension.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Theme von Ruff: festes dunkles Farbschema, Cinzel/Alegreya.
 *
 * Dynamic Color (Material You) ist bewusst aus: Die Farben aus dem Hintergrundbild
 * des Geräts würden die Dark-Fantasy-Identität und die auf die Bildelemente
 * (Rahmen, Leisten, Klinge) abgestimmten Kontraste überschreiben. Es gibt
 * absichtlich auch kein helles Schema; die Bildelemente sind für dunklen Grund gemacht.
 */
@Composable
internal fun RuffTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RuffDarkColors,
        typography = RuffTypography,
        content = content,
    )
}
