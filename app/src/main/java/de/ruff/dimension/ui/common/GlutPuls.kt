package de.ruff.dimension.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.ruff.dimension.ui.theme.Glut
import de.ruff.dimension.ui.theme.Hoellenrot

/**
 * Glut-Puls: ein weicher Höllenrot-Schein mit Glut-Kern, der langsam atmet, hinter
 * dem Inhalt (z. B. dem Ladekreis). Gezeichnet wird nur im Draw-Schritt; der Puls
 * löst weder Layout noch Recomposition aus. Im reduzierten Modus steht der Schein still.
 */
@Composable
internal fun GlutPuls(
    modifier: Modifier = Modifier,
    size: Dp = 112.dp,
    content: @Composable () -> Unit,
) {
    val motion = currentMotion
    val pulse: State<Float> =
        if (motion.reduced) {
            remember { StillePuls }
        } else {
            rememberInfiniteTransition(label = "glut-puls").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(RuffDurations.PULS, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "glut-puls-wert",
            )
        }
    Box(
        modifier =
            modifier
                .size(size)
                .testTag("glut_puls")
                .drawBehind {
                    val t by pulse
                    val radius = this.size.minDimension / 2f * (0.72f + 0.28f * t)
                    drawCircle(
                        brush =
                            Brush.radialGradient(
                                0f to Glut.copy(alpha = 0.30f + 0.25f * t),
                                0.45f to Hoellenrot.copy(alpha = 0.28f + 0.22f * t),
                                1f to Color.Transparent,
                                center = center,
                                radius = radius,
                            ),
                        radius = radius,
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Ruhender Mittelwert für den reduzierten Modus. */
private object StillePuls : State<Float> {
    override val value: Float = 0.5f
}
