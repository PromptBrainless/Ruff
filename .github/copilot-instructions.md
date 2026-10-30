# Copilot-Anweisungen für Ruff

Vollständiger Masterprompt mit Stack, Architektur, Stand und nächsten
Schritten: [docs/MASTERPROMPT_CODESPACE.md](../docs/MASTERPROMPT_CODESPACE.md).
Inhaltliche Kanonregeln: [AGENTS.md](../AGENTS.md).

Kurzfassung:

- Antworte und dokumentiere auf Deutsch. Ruff ist ein Dark-Fantasy-Spiel,
  nicht der Python-Linter.
- Android-App `:app`, Paket `de.ruff.dimension`: Kotlin 2.0.21, AGP 8.7.3,
  Gradle 8.9, Compose BOM 2024.12.01 (Material 3), Room 2.7.2 +
  `sqlite-bundled` 2.5.2 (FTS5), Hilt 2.55, KSP 2.0.21-1.0.28. Versionen stehen
  in `app/build.gradle.kts`, es gibt keine `libs.versions.toml`.
- Clean Architecture (UI → Domain → Data), Room ist Single Source of Truth,
  die JSON-Assets sind nur Seed-Daten. Evolution statt Revolution.
- Nie direkt auf `main`: Themen-Branch, kleine Commits (`feat:`, `fix:`,
  `docs:`, `chore:`), PR gegen `main`. Nicht mergen, kein Force-Push.
- Nichts löschen oder umbenennen, vor allem keine Bilder, Assets oder
  Asset-JSONs. Workflows nicht ändern oder duplizieren.
- Vor DB-Migrationen einen Migrationstest mit echten Seed-Daten schreiben
  (187 Wesen, 12 Seiten, 1308 Blöcke). Keine destruktive Migration.
- Vor jedem Commit `pre-commit run --all-files` ausführen; dazu
  `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:ktlintCheck`.
- Berichte gliedern in Untersucht / Umgesetzt / Dateien / Tests / Offen /
  Nächster Schritt. Nichts als getestet ausgeben, was nicht gelaufen ist.
