package de.ruff.dimension.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.ruff.dimension.ui.theme.Glut
import de.ruff.dimension.ui.theme.Hoellenrot
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Favoriten-Herz im Ruff-Stil.
 *
 * Bewegung beim Favorisieren: Das Herz füllt sich in Höllenrot mit Glut-Rand,
 * springt kurz auf (Skalierung 1 → 1,28 → 0,94 → 1) und ein Glutring mit sechs
 * Funken läuft nach außen und verglimmt. Beim Entfernen sinkt es kurz ein und
 * leert sich. Alle Animationswerte werden nur in der Zeichenphase gelesen –
 * keine Recomposition pro Frame. Reduzierter Modus (Motion.kt): Zustand wechselt
 * sofort, kein Sprung, kein Ring. Haptik (RuffHaptik) bleibt in beiden Modi.
 *
 * Der erste Aufbau (z. B. beim Scrollen in der Liste) animiert nie – nur echte
 * Zustandswechsel tun es.
 */

/** Haptik für einen Wechsel: Favorisieren bestätigt deutlich, Entfernen ist ein leichter Tick. */
internal fun favoriteHaptik(becomesFavorite: Boolean): RuffHaptik = if (becomesFavorite) RuffHaptik.BESTAETIGUNG else RuffHaptik.AUSWAHL

/** Kurzbeschreibung für Screenreader. */
internal fun favoriteStateDescription(favorite: Boolean): String = if (favorite) "Favorit" else "Kein Favorit"

/** Testtag eines Herzens; Liste und Detail hängen einen Ort an, damit beide eindeutig sind. */
internal fun favoriteHeartTag(
    creatureId: String,
    place: String,
): String = "favorit_herz_${place}_$creatureId"

/** Skalierungsspitzen des Favorisier-Sprungs (für Tests und Doku zentral). */
internal object HerzSprung {
    const val SPITZE = 1.28f
    const val NACHFEDERN = 0.94f
    const val EINSINKEN = 0.86f

    /** Funken im Glutring. */
    const val FUNKEN = 6
}

@Composable
internal fun FavoriteHeart(
    favorite: Boolean,
    label: String,
    onToggle: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    heartSize: Dp = 24.dp,
) {
    val motion = currentMotion
    val haptik = rememberRuffHaptik()
    val currentOnToggle by rememberUpdatedState(onToggle)
    val interaction = remember { MutableInteractionSource() }

    val fill by animateFloatAsState(
        targetValue = if (favorite) 1f else 0f,
        animationSpec = motion.spec(RuffDurations.MITTEL),
        label = "herz-fuellung",
    )
    val scale = remember { Animatable(1f) }
    val burst = remember { Animatable(BURST_IDLE) }
    // Merkt den zuletzt gezeichneten Zustand: der erste Aufbau springt nicht.
    val previous = remember { HerzMerker(favorite) }

    LaunchedEffect(favorite, motion.reduced) {
        val changed = previous.value != favorite
        previous.value = favorite
        if (!changed || motion.reduced) {
            scale.snapTo(1f)
            burst.snapTo(BURST_IDLE)
            return@LaunchedEffect
        }
        if (favorite) {
            burst.snapTo(0f)
            val total = motion.duration(RuffDurations.LANG)
            coroutineScope {
                launch {
                    scale.animateTo(
                        1f,
                        keyframes {
                            durationMillis = total
                            1f at 0
                            HerzSprung.SPITZE at total * 35 / 100 using FastOutSlowInEasing
                            HerzSprung.NACHFEDERN at total * 70 / 100 using FastOutSlowInEasing
                        },
                    )
                }
                burst.animateTo(1f, tween(motion.duration(RuffDurations.LANG + RuffDurations.KURZ), easing = LinearOutSlowInEasing))
            }
            burst.snapTo(BURST_IDLE)
        } else {
            burst.snapTo(BURST_IDLE)
            val total = motion.duration(RuffDurations.MITTEL)
            scale.animateTo(
                1f,
                keyframes {
                    durationMillis = total
                    1f at 0
                    HerzSprung.EINSINKEN at total * 40 / 100 using FastOutSlowInEasing
                },
            )
        }
    }

    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    val stateText = favoriteStateDescription(favorite)
    Box(
        modifier =
            modifier
                .size(TOUCH_TARGET)
                .testTag(testTag)
                .toggleable(
                    value = favorite,
                    interactionSource = interaction,
                    indication = ripple(bounded = false, radius = TOUCH_TARGET / 2),
                    role = Role.Switch,
                    onValueChange = { becomes ->
                        haptik(favoriteHaptik(becomes))
                        currentOnToggle()
                    },
                ).semantics {
                    contentDescription = label
                    stateDescription = stateText
                },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(TOUCH_TARGET)) {
            val progress = burst.value
            if (progress in 0f..1f) drawGlutBurst(progress, heartSize.toPx())
            val heartPx = heartSize.toPx()
            val s = scale.value
            withTransform({
                scale(s, s, center)
                translate((size.width - heartPx) / 2f, (size.height - heartPx) / 2f)
            }) {
                drawHeart(heartPx, fill, outline)
            }
        }
    }
}

/** Statisches Herz (z. B. im Filter-Chip), dekorativ ohne eigene Semantik. */
@Composable
internal fun HeartGlyph(
    filled: Boolean,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) { drawHeart(size.minDimension, if (filled) 1f else 0f, outline) }
}

/** Herz mit Füllgrad [fill] (0 = Umriss, 1 = Höllenrot mit Glut-Rand). */
private fun DrawScope.drawHeart(
    heartPx: Float,
    fill: Float,
    outline: Color,
) {
    val path = heartPath(heartPx)
    if (fill > 0f) drawPath(path, color = Hoellenrot.copy(alpha = fill))
    drawPath(
        path,
        color = lerp(outline, Glut, fill),
        style = Stroke(width = heartPx * (STROKE_LEER + (STROKE_VOLL - STROKE_LEER) * fill)),
    )
}

/** Glutring und Funken für Fortschritt [progress] 0..1; verglimmt zum Ende hin. */
private fun DrawScope.drawGlutBurst(
    progress: Float,
    heartPx: Float,
) {
    val alpha = (1f - progress).coerceIn(0f, 1f)
    val minR = heartPx * 0.35f
    val maxR = size.minDimension / 2f
    val radius = minR + (maxR - minR) * progress
    drawCircle(
        color = Glut.copy(alpha = alpha * 0.7f),
        radius = radius,
        center = center,
        style = Stroke(width = heartPx * 0.08f * (1f - progress * 0.6f)),
    )
    val sparkR = heartPx * 0.06f * alpha + 0.5f
    for (i in 0 until HerzSprung.FUNKEN) {
        val angle = (2 * PI * i / HerzSprung.FUNKEN - PI / 2).toFloat()
        val r = radius * 1.05f
        drawCircle(
            color = lerp(Glut, Hoellenrot, progress).copy(alpha = alpha),
            radius = sparkR,
            center = Offset(center.x + cos(angle) * r, center.y + sin(angle) * r),
        )
    }
}

/** Herzpfad im Quadrat der Kantenlänge [sizePx] (Geometrie wie das Material-Herz, 24er-Raster). */
internal fun heartPath(sizePx: Float): Path {
    val k = sizePx / 24f
    return Path().apply {
        moveTo(12f * k, 21.35f * k)
        lineTo(10.55f * k, 20.03f * k)
        cubicTo(5.4f * k, 15.36f * k, 2f * k, 12.28f * k, 2f * k, 8.5f * k)
        cubicTo(2f * k, 5.42f * k, 4.42f * k, 3f * k, 7.5f * k, 3f * k)
        cubicTo(9.24f * k, 3f * k, 10.91f * k, 3.81f * k, 12f * k, 5.09f * k)
        cubicTo(13.09f * k, 3.81f * k, 14.76f * k, 3f * k, 16.5f * k, 3f * k)
        cubicTo(19.58f * k, 3f * k, 22f * k, 5.42f * k, 22f * k, 8.5f * k)
        cubicTo(22f * k, 12.28f * k, 18.6f * k, 15.36f * k, 13.45f * k, 20.04f * k)
        close()
    }
}

/** Ruhewert des Rings: außerhalb 0..1 = nicht zeichnen. */
private const val BURST_IDLE = -1f
private val TOUCH_TARGET = 48.dp
private const val STROKE_LEER = 0.085f
private const val STROKE_VOLL = 0.06f

/** Einfacher, nicht beobachteter Merker (kein State: Änderung soll nichts neu zeichnen). */
private class HerzMerker(
    var value: Boolean,
)
