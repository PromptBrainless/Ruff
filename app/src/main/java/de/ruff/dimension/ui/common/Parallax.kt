package de.ruff.dimension.ui.common

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Porträt läuft mit 35 % der Scrollgeschwindigkeit nach – ruhig, nicht schwammig. */
internal const val PARALLAX_FAKTOR = 0.35f

/**
 * Reine Regel: Verschiebung (px) des Porträts für einen Scrollstand. Im reduzierten
 * Modus 0; sonst proportional, begrenzt auf [maxPx] und nie negativ (Overscroll).
 */
internal fun parallaxOffset(
    scrollPx: Int,
    maxPx: Float,
    reduced: Boolean,
    factor: Float = PARALLAX_FAKTOR,
): Float = if (reduced || maxPx <= 0f) 0f else (scrollPx * factor).coerceIn(0f, maxPx)

/** Leichtes Ausblenden während des Parallax-Wegs (1 → 0,6). */
internal fun parallaxAlpha(
    offsetPx: Float,
    maxPx: Float,
): Float = if (maxPx <= 0f) 1f else 1f - 0.4f * (offsetPx / maxPx).coerceIn(0f, 1f)

/**
 * Parallax über den Grafik-Layer: [ScrollState.value] wird nur im graphicsLayer-
 * Block gelesen, also pro Scrollpixel nur neu platziert/gezeichnet, nie neu komponiert.
 */
internal fun Modifier.ruffParallax(
    scroll: ScrollState,
    reduced: Boolean,
    maxPx: () -> Float,
): Modifier =
    if (reduced) {
        this
    } else {
        graphicsLayer {
            val max = maxPx()
            val offset = parallaxOffset(scroll.value, max, reduced = false)
            translationY = offset
            alpha = parallaxAlpha(offset, max)
        }
    }
