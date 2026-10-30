package de.ruff.dimension.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Dark-Fantasy-Palette von Ruff: Blutrot und Glut auf fast schwarzem Grund.
 * Die bisherigen Markenfarben (Ruß #16171A, Eisen, Höllenrot #B3201B) bleiben
 * erhalten; alle Textrollen erreichen mindestens WCAG AA (4,5:1), geprüft in
 * `RuffColorsContrastTest`.
 */

/** Höllenrot aus dem bisherigen Theme; Füllfarbe für Hauptaktionen. */
internal val Hoellenrot = Color(0xFFB3201B)

/** Glut-Orange für Akzente (früher `Flame`), aufgehellt für Text auf Schwarz. */
internal val Glut = Color(0xFFFFB77C)

/** Pergament für Fließtext auf dunklem Grund. */
internal val Pergament = Color(0xFFEDE3D6)

/** Ruß: Hintergrund, fast schwarz mit warmem Stich. */
internal val Russ = Color(0xFF0E0B0C)

/** Halbtransparentes Schwarz für Lesbarkeit über Bildern. */
internal val Scrim = Color(0xD9000000)

internal val RuffDarkColors: ColorScheme =
    darkColorScheme(
        primary = Color(0xFFFFB4A8),
        onPrimary = Color(0xFF5C1A12),
        primaryContainer = Hoellenrot,
        onPrimaryContainer = Color(0xFFFFE9E5),
        inversePrimary = Color(0xFFA4372C),
        secondary = Glut,
        onSecondary = Color(0xFF4A2800),
        secondaryContainer = Color(0xFF6E3A0B),
        onSecondaryContainer = Color(0xFFFFDCC2),
        tertiary = Color(0xFFE5C07B),
        onTertiary = Color(0xFF3F2E00),
        tertiaryContainer = Color(0xFF5B4300),
        onTertiaryContainer = Color(0xFFFFDEA1),
        background = Russ,
        onBackground = Pergament,
        surface = Color(0xFF141012),
        onSurface = Pergament,
        surfaceVariant = Color(0xFF2A2123),
        onSurfaceVariant = Color(0xFFCDBFB4),
        surfaceTint = Color(0xFFFFB4A8),
        inverseSurface = Pergament,
        inverseOnSurface = Color(0xFF2F2628),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF9E8C85),
        outlineVariant = Color(0xFF4E4340),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF3A3032),
        surfaceDim = Color(0xFF0E0B0C),
        surfaceContainerLowest = Color(0xFF0A0809),
        surfaceContainerLow = Color(0xFF181314),
        surfaceContainer = Color(0xFF1D1618),
        surfaceContainerHigh = Color(0xFF2A2123),
        surfaceContainerHighest = Color(0xFF352B2D),
    )
