package de.ruff.dimension.ui.common

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import de.ruff.dimension.data.ContentPresentation
import de.ruff.dimension.ui.theme.withFont
import java.io.File

@Composable
internal fun ContentPresentationFrame(
    presentation: ContentPresentation,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(presentation.cornerRadiusDp.dp.coerceIn(0.dp, 48.dp))
    val frameColor =
        runCatching { Color(android.graphics.Color.parseColor(presentation.frameColor)) }
            .getOrDefault(Color(0xFF8B5A3C))
    val fontFamily =
        remember(presentation.fontPath) {
            runCatching {
                if (presentation.fontPath.isBlank() || !File(presentation.fontPath).isFile) {
                    null
                } else {
                    FontFamily(Typeface.createFromFile(presentation.fontPath))
                }
            }.getOrNull()
        }
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(Color.Black.copy(alpha = 0.3f))
                .border(presentation.frameWidthDp.dp.coerceIn(0.dp, 12.dp), frameColor, shape),
    ) {
        if (presentation.backgroundPath.isNotBlank()) {
            LocalMediaImage(
                path = presentation.backgroundPath,
                modifier = Modifier.fillMaxSize().clip(shape),
            )
        }
        Box(Modifier.fillMaxSize().padding(PaddingValues(6.dp))) {
            if (fontFamily == null) {
                content()
            } else {
                MaterialTheme(typography = MaterialTheme.typography.withFont(fontFamily)) {
                    content()
                }
            }
        }
    }
}
