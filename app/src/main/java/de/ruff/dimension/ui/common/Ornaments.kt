package de.ruff.dimension.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.ruff.dimension.R
import de.ruff.dimension.ui.theme.Pergament
import kotlin.math.roundToInt

/*
 * Dark-Fantasy-Bausteine aus Fynns Design-Elementen (res/drawable-nodpi/ruff_ui_*).
 * Maße der Vorlagen in Pixeln; alle Bilder behalten ihr Seitenverhältnis, nur die
 * Mitte der Titelleiste wird in die Breite gezogen (3-Slice).
 */

private const val BAR_W = 1095f
private const val BAR_H = 226f
private const val BAR_CAP_LEFT = 175
private const val BAR_CAP_RIGHT = 150
private const val BAR_TEXT_TOP = 44f
private const val BAR_TEXT_BOTTOM = 186f

/** Textschatten, damit Schrift auf den Ornamenten klar steht. */
internal val TextOnArtShadow = Shadow(color = Color.Black, blurRadius = 8f)

/**
 * Titelleiste `ruff_ui_bar_titel` als 3-Slice: linke Kappe (Flammen-Emblem) und
 * rechte Kappe bleiben unverzerrt, die Mitte wird gestreckt. Inhalt liegt im
 * Textfeld der Leiste auf einem dezenten Scrim.
 */
@Composable
internal fun TitleBarFrame(
    modifier: Modifier = Modifier,
    height: Dp = 60.dp,
    content: @Composable () -> Unit,
) {
    val bar = ImageBitmap.imageResource(R.drawable.ruff_ui_bar_titel)
    val scale = height.value / BAR_H
    Box(
        modifier =
            modifier
                .height(height)
                .drawBehind { drawThreeSlice(bar) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier =
                Modifier
                    .padding(
                        start = (BAR_CAP_LEFT * scale).dp,
                        end = (BAR_CAP_RIGHT * scale * 0.6f).dp,
                        top = (BAR_TEXT_TOP * scale).dp,
                        bottom = ((BAR_H - BAR_TEXT_BOTTOM) * scale).dp,
                    ).fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            content()
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawThreeSlice(bar: ImageBitmap) {
    val h = size.height
    val s = h / bar.height
    var left = BAR_CAP_LEFT * s
    var right = BAR_CAP_RIGHT * s
    if (left + right > size.width) {
        val f = size.width / (left + right)
        left *= f
        right *= f
    }
    val ih = bar.height
    val midSrcW = bar.width - BAR_CAP_LEFT - BAR_CAP_RIGHT
    val q = FilterQuality.Medium
    drawImage(
        bar,
        IntOffset(0, 0),
        IntSize(BAR_CAP_LEFT, ih),
        IntOffset.Zero,
        IntSize(left.roundToInt(), h.roundToInt()),
        filterQuality = q,
    )
    drawImage(
        bar,
        IntOffset(BAR_CAP_LEFT, 0),
        IntSize(midSrcW, ih),
        IntOffset(left.roundToInt(), 0),
        IntSize((size.width - left - right).roundToInt().coerceAtLeast(0), h.roundToInt()),
        filterQuality = q,
    )
    drawImage(
        bar,
        IntOffset(bar.width - BAR_CAP_RIGHT, 0),
        IntSize(BAR_CAP_RIGHT, ih),
        IntOffset((size.width - right).roundToInt(), 0),
        IntSize(right.roundToInt(), h.roundToInt()),
        filterQuality = q,
    )
}

/** Abschnittsüberschrift auf der Titelleiste (Detailseiten). */
@Composable
internal fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    TitleBarFrame(modifier = modifier.fillMaxWidth(), height = 46.dp) {
        Text(
            text = text, // Cinzel setzt Kleinbuchstaben ohnehin als Kapitälchen.
            style = MaterialTheme.typography.titleSmall.copy(shadow = TextOnArtShadow),
            color = Pergament,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Ketten-Trenner `ruff_ui_divider_kette`, mittig, höchstens [maxWidth] breit. */
@Composable
internal fun ChainDivider(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 360.dp,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.ruff_ui_divider_kette),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .aspectRatio(1129f / 445f),
        )
    }
}

/**
 * Klingen-Knopf `ruff_ui_button_klinge` (1089×331). Wird nicht verzerrt; die
 * Beschriftung sitzt im glatten Teil der Klinge (x 330–850, y 70–230) auf einem Scrim.
 */
@Composable
internal fun BladeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier =
            modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .aspectRatio(1089f / 331f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClickLabel = text, onClick = onClick),
    ) {
        Image(
            painter = painterResource(R.drawable.ruff_ui_button_klinge),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        val w = this.maxWidth // nicht der gleichnamige Parameter
        val h = maxHeight
        Box(
            modifier =
                Modifier
                    .offset(x = w * (330f / 1089f), y = h * (70f / 331f))
                    .size(width = w * (520f / 1089f), height = h * (160f / 331f))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.55f), Color.Transparent),
                        ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text, // Cinzel setzt Kleinbuchstaben ohnehin als Kapitälchen.
                style = MaterialTheme.typography.titleMedium.copy(shadow = TextOnArtShadow),
                color = Pergament,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Ovaler Ausschnitt für das Bild im Porträtrahmen. */
private val OvalShape = GenericShape { size, _ -> addOval(Rect(0f, 0f, size.width, size.height)) }

/**
 * Porträtrahmen `ruff_ui_frame_portrait` (784×1021). Das Bild liegt im Rechteck
 * L136/T205/R136/B299, oval beschnitten, der Rahmen darüber.
 */
@Composable
internal fun PortraitFrame(
    imageRes: Int?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 300.dp,
) {
    BoxWithConstraints(
        modifier =
            modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .aspectRatio(784f / 1021f),
    ) {
        val w = this.maxWidth // nicht der gleichnamige Parameter
        val h = maxHeight
        Image(
            painter = painterResource(imageRes ?: PlaceholderImage),
            contentDescription = contentDescription,
            contentScale = if (imageRes != null) ContentScale.Crop else ContentScale.Fit,
            modifier =
                Modifier
                    .offset(x = w * (136f / 784f), y = h * (205f / 1021f))
                    .size(width = w * (512f / 784f), height = h * (517f / 1021f))
                    .clip(OvalShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        )
        Image(
            painter = painterResource(R.drawable.ruff_ui_frame_portrait),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Detailrahmen `ruff_ui_frame_detail` (1168×784, Fenster x198–968, y206–576).
 * Seitenverhältnis bleibt; Inhalt liegt innerhalb der sicheren Ränder
 * (L249/T226/R247/B217) auf dunklem Grund und scrollt bei Bedarf selbst.
 */
@Composable
internal fun DetailFrame(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 520.dp,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier =
            modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .aspectRatio(1168f / 784f),
    ) {
        val w = this.maxWidth // nicht der gleichnamige Parameter
        val h = maxHeight
        Box(
            modifier =
                Modifier
                    .offset(x = w * (198f / 1168f), y = h * (206f / 784f))
                    .size(width = w * (770f / 1168f), height = h * (370f / 784f))
                    .background(Color.Black.copy(alpha = 0.82f)),
        )
        Image(
            painter = painterResource(R.drawable.ruff_ui_frame_detail),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier =
                Modifier
                    .offset(x = w * (249f / 1168f), y = h * (226f / 784f))
                    .size(width = w * ((1168f - 249f - 247f) / 1168f), height = h * ((784f - 226f - 217f) / 784f)),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/**
 * Kleiner Eisenring `ruff_ui_avatar_rahmen` (aus Fynns Notion-Theme) um ein Wesen-Icon;
 * ohne Bild erscheint die Ruff-Glyphe als Platzhalter.
 */
@Composable
internal fun AvatarFrame(
    iconRes: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (iconRes != null) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(size * 0.76f)
                        .clip(androidx.compose.foundation.shape.CircleShape),
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ruff_glyph),
                contentDescription = null,
                modifier = Modifier.size(size * 0.5f),
            )
        }
        Image(
            painter = painterResource(R.drawable.ruff_ui_avatar_rahmen),
            contentDescription = null,
            modifier = Modifier.size(size),
        )
    }
}
