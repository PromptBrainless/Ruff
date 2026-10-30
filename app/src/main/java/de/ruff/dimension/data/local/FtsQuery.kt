package de.ruff.dimension.data.local

/**
 * Baut aus einer Nutzereingabe einen sicheren FTS5-Ausdruck.
 *
 * Die Eingabe wird an allem zerlegt, was kein Buchstabe oder keine Ziffer ist;
 * FTS5-Operatoren wie `"`, `*`, `-`, `:`, `^`, `(`, `NEAR` oder `OR` können
 * so nicht eingeschleust werden. Jedes Wort wird in Anführungszeichen gesetzt
 * (innere `"` verdoppelt) und als Präfix gesucht; alle Wörter müssen
 * vorkommen (implizites AND). Liefert `null`, wenn kein Wort übrig bleibt.
 */
internal object FtsQuery {
    private const val MAX_TERMS = 16
    private val separator = Regex("[^\\p{L}\\p{N}]+")

    fun build(input: String): String? {
        val terms =
            input
                .split(separator)
                .filter { it.isNotEmpty() }
                .take(MAX_TERMS)
        if (terms.isEmpty()) return null
        return terms.joinToString(" ") { "\"" + it.replace("\"", "\"\"") + "\"*" }
    }
}
