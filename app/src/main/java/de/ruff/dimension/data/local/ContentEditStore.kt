package de.ruff.dimension.data.local

import androidx.room.useReaderConnection
import androidx.room.useWriterConnection

internal data class ContentEdit(
    val contentType: String,
    val refId: String,
    val editedJson: String,
)

internal interface ContentEditStore {
    suspend fun getAll(): List<ContentEdit>

    suspend fun upsert(edit: ContentEdit)

    suspend fun delete(
        contentType: String,
        refId: String,
    )
}

/** Kleine lokale Zusatztabelle, analog zum benutzerdefinierten FTS5-Index. */
internal class RoomContentEditStore(
    private val database: RuffDatabase,
) : ContentEditStore {
    override suspend fun getAll(): List<ContentEdit> =
        database.useReaderConnection { connection ->
            connection.usePrepared(SELECT_ALL) { statement ->
                buildList {
                    while (statement.step()) {
                        add(
                            ContentEdit(
                                contentType = statement.getText(0),
                                refId = statement.getText(1),
                                editedJson = statement.getText(2),
                            ),
                        )
                    }
                }
            }
        }

    override suspend fun upsert(edit: ContentEdit) {
        database.useWriterConnection { connection ->
            connection.usePrepared(UPSERT) { statement ->
                statement.bindText(1, edit.contentType)
                statement.bindText(2, edit.refId)
                statement.bindText(3, edit.editedJson)
                statement.step()
            }
        }
    }

    override suspend fun delete(
        contentType: String,
        refId: String,
    ) {
        database.useWriterConnection { connection ->
            connection.usePrepared(DELETE) { statement ->
                statement.bindText(1, contentType)
                statement.bindText(2, refId)
                statement.step()
            }
        }
    }

    companion object {
        const val CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS `content_edits` " +
                "(`contentType` TEXT NOT NULL, `refId` TEXT NOT NULL, `editedJson` TEXT NOT NULL, " +
                "PRIMARY KEY(`contentType`, `refId`))"

        private const val SELECT_ALL = "SELECT contentType, refId, editedJson FROM content_edits"
        private const val UPSERT =
            "INSERT INTO content_edits(contentType, refId, editedJson) VALUES(?, ?, ?) " +
                "ON CONFLICT(contentType, refId) DO UPDATE SET editedJson = excluded.editedJson"
        private const val DELETE = "DELETE FROM content_edits WHERE contentType = ? AND refId = ?"
    }
}
