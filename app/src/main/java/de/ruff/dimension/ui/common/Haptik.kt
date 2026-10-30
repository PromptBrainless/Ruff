package de.ruff.dimension.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/*
 * Zentrale Haptik von Ruff über LocalHapticFeedback. Compose 1.7 kennt nur
 * LongPress und TextHandleMove; die Zuordnung liegt hier, damit ein späteres
 * Upgrade (Confirm/SegmentTick) an einer Stelle passiert. Das System entscheidet
 * selbst, ob Haptik an ist (Einstellung „Haptisches Feedback“). Der reduzierte
 * Animationsmodus schaltet Haptik bewusst nicht ab – sie ist keine Bewegung.
 */

/** Art der Rückmeldung. */
internal enum class RuffHaptik {
    /** Leichter Tick: Auswahl in Filtern, Tabs/Bereichswechsel. */
    AUSWAHL,

    /** Deutlicher Impuls: Klingen-Knopf bestätigt. */
    BESTAETIGUNG,
}

/** Reine Zuordnung Ruff-Haptik → Compose-Haptik. */
internal fun hapticType(kind: RuffHaptik): HapticFeedbackType =
    when (kind) {
        RuffHaptik.AUSWAHL -> HapticFeedbackType.TextHandleMove
        RuffHaptik.BESTAETIGUNG -> HapticFeedbackType.LongPress
    }

internal fun HapticFeedback.perform(kind: RuffHaptik) = performHapticFeedback(hapticType(kind))

/** Stabile Lambda für Klick-Handler. */
@Composable
internal fun rememberRuffHaptik(): (RuffHaptik) -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { { kind -> haptic.perform(kind) } }
}
