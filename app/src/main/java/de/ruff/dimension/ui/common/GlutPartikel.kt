package de.ruff.dimension.ui.common

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import de.ruff.dimension.ui.theme.Glut
import de.ruff.dimension.ui.theme.Hoellenrot
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/*
 * Glut-Partikel: wenige aufsteigende Funken über dem Hintergrund.
 *
 * Leistung: Alle Partikelwerte liegen in einmalig angelegten FloatArrays; die
 * Position ist eine reine Funktion der Zeit (kein Zustand pro Partikel, keine
 * Allokation pro Frame). Die Zeit ist ein Float-State, der nur in der
 * Zeichenphase gelesen wird – pro Frame wird neu gezeichnet, nicht neu komponiert.
 * Die Frame-Schleife läuft nur, solange der Bildschirm mindestens STARTED ist,
 * der Aufrufer nicht pausiert und der reduzierte Modus aus ist.
 */

/** Obergrenze, damit niemand versehentlich ein teures Feld anlegt. */
internal const val MAX_GLUT_PARTIKEL = 24

/** Vorberechnete, unveränderliche Startwerte eines Partikelfelds (normierte Einheiten). */
internal class GlutPartikelFeld(
    count: Int,
    seed: Int = 7,
) {
    val size: Int = count.coerceIn(0, MAX_GLUT_PARTIKEL)
    val baseX = FloatArray(size)
    val speed = FloatArray(size)
    val phase = FloatArray(size)
    val sway = FloatArray(size)
    val radius = FloatArray(size)

    init {
        val random = Random(seed)
        for (i in 0 until size) {
            baseX[i] = random.nextFloat()
            speed[i] = 0.04f + random.nextFloat() * 0.06f // Bildhöhen pro Sekunde
            phase[i] = random.nextFloat()
            sway[i] = 0.005f + random.nextFloat() * 0.02f
            radius[i] = 0.8f + random.nextFloat() * 1.4f // dp
        }
    }
}

/** Partikel sind nur außerhalb des reduzierten Modus aktiv. */
internal fun particlesEnabled(motion: RuffMotion): Boolean = !motion.reduced

/** Fortschritt eines Partikels von unten (0) nach oben (1), periodisch. */
internal fun emberProgress(
    timeSeconds: Float,
    speed: Float,
    phase: Float,
): Float {
    val raw = (timeSeconds * speed + phase) % 1f
    return if (raw < 0f) raw + 1f else raw
}

/** Weiches Ein- und Ausglühen über die Lebensdauer (0 an beiden Enden). */
internal fun emberAlpha(progress: Float): Float = sin(PI.toFloat() * progress.coerceIn(0f, 1f)).coerceIn(0f, 1f)

/** Normierte Höhe: 1 = unten, 0 = oben. */
internal fun emberY(progress: Float): Float = 1f - progress

/** Normierte Breite mit leichtem Schwanken, auf [0, 1] begrenzt. */
internal fun emberX(
    baseX: Float,
    sway: Float,
    phase: Float,
    timeSeconds: Float,
): Float = (baseX + sway * sin(2f * PI.toFloat() * (timeSeconds * SWAY_HZ + phase))).coerceIn(0f, 1f)

private const val SWAY_HZ = 0.25f

/**
 * Zeichnet [count] Glutfunken in die Fläche von [modifier]. Im reduzierten Modus
 * wird gar nichts komponiert. [paused] hält die Frame-Schleife an (z. B. wenn der
 * Bereich weggescrollt ist); es sollte selten wechseln.
 */
@Composable
internal fun GlutPartikel(
    modifier: Modifier = Modifier,
    count: Int = 16,
    paused: Boolean = false,
) {
    val motion = currentMotion
    if (!particlesEnabled(motion)) return
    val field = remember(count) { GlutPartikelFeld(count) }
    var timeSeconds by remember { mutableFloatStateOf(0f) }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle
        .currentStateAsState()
    val running = !paused && lifecycleState.isAtLeast(Lifecycle.State.STARTED)

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val startOffset = timeSeconds
        var startNanos = -1L
        while (true) {
            withInfiniteAnimationFrameNanos { nanos ->
                if (startNanos < 0L) startNanos = nanos
                timeSeconds = startOffset + (nanos - startNanos) / 1_000_000_000f
            }
        }
    }

    Canvas(modifier.testTag("glut_partikel")) {
        val t = timeSeconds // nur hier gelesen → nur Neuzeichnen
        val dpPx = 1.dp.toPx()
        for (i in 0 until field.size) {
            val progress = emberProgress(t, field.speed[i], field.phase[i])
            val alpha = emberAlpha(progress)
            if (alpha <= 0.01f) continue
            val center =
                Offset(
                    emberX(field.baseX[i], field.sway[i], field.phase[i], t) * size.width,
                    emberY(progress) * size.height,
                )
            val r = field.radius[i] * dpPx
            drawCircle(color = Hoellenrot.copy(alpha = alpha * 0.22f), radius = r * 3.2f, center = center)
            drawCircle(color = Glut.copy(alpha = alpha * 0.75f), radius = r, center = center)
        }
    }
}
