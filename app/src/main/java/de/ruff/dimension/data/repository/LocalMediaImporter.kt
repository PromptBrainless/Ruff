package de.ruff.dimension.data.repository

import android.content.ContentResolver
import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import de.ruff.dimension.data.MediaAttachment
import java.io.File
import java.util.UUID

internal enum class MediaKind {
    IMAGE,
    VIDEO,
    FONT,
    BACKGROUND,
}

internal fun interface MediaImporter {
    suspend fun import(
        uri: Uri,
        kind: MediaKind,
    ): MediaAttachment
}

/** Kopiert ausgewählte Dateien in den privaten App-Speicher; es werden keine externen Pfade behalten. */
internal class PrivateMediaImporter(
    context: Context,
) : MediaImporter {
    private val resolver: ContentResolver = context.applicationContext.contentResolver
    private val directory = File(context.applicationContext.filesDir, MEDIA_DIRECTORY)

    override suspend fun import(
        uri: Uri,
        kind: MediaKind,
    ): MediaAttachment {
        val mimeType =
            resolver.getType(uri)?.lowercase()
                ?: throw IllegalArgumentException("Dateityp konnte nicht ermittelt werden.")
        require(
            when (kind) {
                MediaKind.IMAGE, MediaKind.BACKGROUND -> mimeType.startsWith("image/")
                MediaKind.VIDEO -> mimeType.startsWith("video/")
                MediaKind.FONT -> mimeType in FONT_MIME_TYPES
            },
        ) { "Dieser Dateityp ist für die Auswahl nicht zugelassen." }

        val displayName = displayName(resolver, uri)
        val extension =
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                ?: displayName.substringAfterLast('.', "")
        require(extension.isNotBlank()) { "Dateiendung fehlt." }
        require(extension.lowercase() in extensions(kind)) { "Dateiendung passt nicht zum Dateityp." }

        check(directory.exists() || directory.mkdirs()) { "Privater Medienordner konnte nicht angelegt werden." }
        val destination = File(directory, "${UUID.randomUUID()}.${extension.lowercase()}")
        try {
            val input = resolver.openInputStream(uri) ?: error("Datei konnte nicht geöffnet werden.")
            input.use { source ->
                destination.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= MAX_FILE_BYTES) { "Datei ist größer als 100 MB." }
                        output.write(buffer, 0, read)
                    }
                    require(total > 0) { "Leere Dateien können nicht importiert werden." }
                }
            }
            if (kind == MediaKind.FONT) Typeface.createFromFile(destination)
        } catch (error: Exception) {
            destination.delete()
            throw error
        }
        return MediaAttachment(destination.absolutePath, mimeType, displayName)
    }

    private fun displayName(
        resolver: ContentResolver,
        uri: Uri,
    ): String =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.takeIf(String::isNotBlank) else null
        } ?: "Ruff-Medium"

    private fun extensions(kind: MediaKind): Set<String> =
        when (kind) {
            MediaKind.IMAGE, MediaKind.BACKGROUND -> setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")
            MediaKind.VIDEO -> setOf("mp4", "webm", "3gp", "mkv")
            MediaKind.FONT -> setOf("ttf", "otf")
        }

    private companion object {
        const val MEDIA_DIRECTORY = "ruff-media"
        const val MAX_FILE_BYTES = 100L * 1024 * 1024
        val FONT_MIME_TYPES = setOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream")
    }
}
