package de.ruff.dimension.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Speichert `fields`-Maps als JSON; Gson erhält die Schlüsselreihenfolge. */
internal class Converters {
    @TypeConverter
    fun fieldsToJson(fields: Map<String, String>): String = gson.toJson(fields, fieldsType)

    @TypeConverter
    fun jsonToFields(json: String): Map<String, String> = gson.fromJson(json, fieldsType) ?: emptyMap()

    private companion object {
        val gson = Gson()
        val fieldsType = object : TypeToken<LinkedHashMap<String, String>>() {}.type
    }
}
