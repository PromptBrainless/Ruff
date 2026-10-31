package de.ruff.dimension.ui.common

import android.graphics.BitmapFactory
import android.media.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.ruff.dimension.data.MediaAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun LocalMediaImage(
    path: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val bitmap =
        produceState<android.graphics.Bitmap?>(initialValue = null, path) {
            value = withContext(Dispatchers.IO) { decodeImage(path) }
        }.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    }
}

@Composable
internal fun MediaGallery(
    media: List<MediaAttachment>,
    modifier: Modifier = Modifier,
) {
    if (media.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(media, key = { it.path }) { attachment ->
            if (attachment.mimeType.startsWith("video/")) {
                EmbeddedVideo(attachment, Modifier.width(300.dp).height(190.dp))
            } else if (attachment.mimeType.startsWith("image/")) {
                Card(
                    modifier = Modifier.width(280.dp).height(190.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    LocalMediaImage(
                        path = attachment.path,
                        contentDescription = attachment.displayName,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmbeddedVideo(
    attachment: MediaAttachment,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val controller = MediaController(context)
    var videoView: VideoView? = null
    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        factory = { viewContext ->
            VideoView(viewContext).also { view ->
                videoView = view
                controller.setAnchorView(view)
                view.setMediaController(controller)
                view.setVideoPath(attachment.path)
            }
        },
        update = { view ->
            videoView = view
            controller.setAnchorView(view)
        },
    )
    DisposableEffect(attachment.path) {
        onDispose { videoView?.stopPlayback() }
    }
}

@Composable
internal fun MediaPlaceholder(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun decodeImage(path: String): android.graphics.Bitmap? {
    val file = File(path)
    if (!file.isFile) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > MAX_PREVIEW_SIZE || bounds.outHeight / sampleSize > MAX_PREVIEW_SIZE) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
}

private const val MAX_PREVIEW_SIZE = 1600
