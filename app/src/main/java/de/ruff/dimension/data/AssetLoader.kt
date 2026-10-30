package de.ruff.dimension.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Reader

/**
 * Liest die gebündelten JSON-Daten der App.
 *
 * Die Funktionen nehmen einen [Reader] entgegen und brauchen keinen
 * Android-Context, damit sie in JVM-Unit-Tests direkt geprüft werden können.
 */
internal object AssetLoader {
    const val CREATURES_FILE = "creatures.json"
    const val RULES_FILE = "rules.json"

    private val gson = Gson()
    private val creaturesType = object : TypeToken<List<Creature>>() {}.type
    private val rulesType = object : TypeToken<List<RulePage>>() {}.type

    fun parseCreatures(reader: Reader): List<Creature> = gson.fromJson(reader, creaturesType)

    fun parseRules(reader: Reader): List<RulePage> = gson.fromJson(reader, rulesType)
}
