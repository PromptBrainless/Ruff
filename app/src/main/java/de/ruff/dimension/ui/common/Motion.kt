package de.ruff.dimension.ui.common

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/*
 * Zentrale Bewegungsregeln von Ruff. Alle Animationen der App holen Dauer und
 * Übergänge von hier, damit der reduzierte Modus überall gleich greift.
 *
 * Reduzierter Modus: Steht in den Entwickleroptionen bzw. Bedienungshilfen die
 * „Animator-Dauerskala“ (Settings.Global.ANIMATOR_DURATION_SCALE) auf 0
 * („Animationen entfernen“), laufen keine Ruff-Animationen: Dauer 0, keine
 * Übergänge, kein Glut-Puls, keine Shared Elements. Bei Werten > 0 skaliert
 * Compose die Dauer ohnehin selbst über die Systemskala.
 */

/** Dauerstufen in Millisekunden (Basiswerte vor dem reduzierten Modus). */
internal object RuffDurations {
    const val KURZ = 120
    const val MITTEL = 280
    const val LANG = 420

    /** Eine Periode des Glut-Pulses (hin und zurück je eine Periode). */
    const val PULS = 1400
}

/** Liest die Animator-Dauerskala des Systems; Standard 1, wenn nicht gesetzt. */
internal fun readAnimatorDurationScale(context: Context): Float =
    runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)

/** Reine Regel: reduziert, wenn die Systemskala 0 (oder ungültig/negativ) ist. */
internal fun isReducedMotion(animatorScale: Float): Boolean = !(animatorScale > 0f)

/** Reine Regel: Dauer für eine Basisdauer; im reduzierten Modus immer 0. */
internal fun motionDuration(
    baseMillis: Int,
    animatorScale: Float,
): Int = if (isReducedMotion(animatorScale)) 0 else baseMillis.coerceAtLeast(0)

/** Bewegungseinstellungen für einen Bildschirmbaum. */
@Immutable
internal data class RuffMotion(
    val animatorScale: Float = 1f,
) {
    val reduced: Boolean get() = isReducedMotion(animatorScale)

    fun duration(baseMillis: Int): Int = motionDuration(baseMillis, animatorScale)

    /** Tween mit Ruff-Kurve; im reduzierten Modus ein sofortiger Sprung. */
    fun <T> spec(baseMillis: Int = RuffDurations.MITTEL): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(durationMillis = duration(baseMillis), easing = FastOutSlowInEasing)

    /** Bildschirmwechsel vorwärts: Einblenden und leicht von rechts hereingleiten. */
    fun screenEnter(): EnterTransition =
        if (reduced) {
            EnterTransition.None
        } else {
            fadeIn(spec(RuffDurations.MITTEL)) +
                slideInHorizontally(spec(RuffDurations.MITTEL)) { width -> width / SLIDE_FRACTION }
        }

    fun screenExit(): ExitTransition =
        if (reduced) {
            ExitTransition.None
        } else {
            fadeOut(spec(RuffDurations.KURZ)) +
                slideOutHorizontally(spec(RuffDurations.MITTEL)) { width -> -width / SLIDE_FRACTION }
        }

    /** Zurück: Gegenrichtung. */
    fun screenPopEnter(): EnterTransition =
        if (reduced) {
            EnterTransition.None
        } else {
            fadeIn(spec(RuffDurations.MITTEL)) +
                slideInHorizontally(spec(RuffDurations.MITTEL)) { width -> -width / SLIDE_FRACTION }
        }

    fun screenPopExit(): ExitTransition =
        if (reduced) {
            ExitTransition.None
        } else {
            fadeOut(spec(RuffDurations.KURZ)) +
                slideOutHorizontally(spec(RuffDurations.MITTEL)) { width -> width / SLIDE_FRACTION }
        }

    private companion object {
        /** Nur ein Achtel der Breite: wirkt ruhig und bleibt günstig zu zeichnen. */
        const val SLIDE_FRACTION = 8
    }
}

internal val LocalRuffMotion = staticCompositionLocalOf { RuffMotion() }

/** Shared-Element-Bühne (gesetzt in RuffNavHost); null = keine Shared Elements. */
@OptIn(ExperimentalSharedTransitionApi::class)
internal val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * Sichtbarkeits-Scope des aktuellen Panes für Shared Elements. Wird nur dort
 * gesetzt, wo ein Übergang sinnvoll ist (einspaltige Liste → Detail); sonst null.
 */
internal val LocalSharedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Schlüssel für das Bild eines Wesens (Karte ↔ Detail). */
internal fun creatureImageKey(id: String): String = "wesen-bild-$id"

/**
 * Verbindet Karte und Detail über gemeinsame Grenzen (sharedBounds): Das kleine
 * Icon der Karte wächst in den Porträtrahmen und blendet dabei über. Fehlt eine
 * der beiden Bühnen oder ist der reduzierte Modus aktiv, bleibt der Modifier leer.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
internal fun Modifier.ruffSharedBounds(key: String): Modifier =
    composed {
        val transitionScope = LocalSharedTransitionScope.current
        val visibilityScope = LocalSharedVisibilityScope.current
        val motion = LocalRuffMotion.current
        if (transitionScope == null || visibilityScope == null || motion.reduced) {
            this
        } else {
            with(transitionScope) {
                this@composed.sharedBounds(
                    sharedContentState = rememberSharedContentState(key),
                    animatedVisibilityScope = visibilityScope,
                )
            }
        }
    }

/** Bequemer Zugriff in Composables. */
internal val currentMotion: RuffMotion
    @Composable get() = LocalRuffMotion.current
