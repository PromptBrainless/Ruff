# Ruff · Game Dimension

Ein nativer Android-MVP für das Ruff-Regelwerk und Wesen-Register. Die App bietet:

- Suche über Wesen und ihre exportierten Eigenschaften
- Filter nach Machtstufe
- Detailansichten mit den vollständigen Registerfeldern
- durchsuchbare Regel-Dokumente mit lesbarer Abschnittsstruktur
- die originale Ruff-Glyphe und Design-Assets im Eisen-/Ruß-/Höllenrot-Thema

## Daten aus dem Notion-Export aktualisieren

Die drei bereitgestellten ZIP-Dateien zuerst in einen Ordner entpacken. Dann den
Ordner `Privat und Geteilt/Ruff` an das Importskript übergeben:

```bash
python tools/import_export.py ".source-export/Privat und Geteilt/Ruff"
```

Das Skript erzeugt die JSON-Daten in `app/src/main/assets` und kopiert die
benötigten Design-Assets nach `app/src/main/res/drawable-nodpi`.
Zwei leere Wesen-Platzhalter aus dem Export werden übersprungen; andere
unvollständige Einträge bleiben erhalten.

Die im Export enthaltenen Machtstufen bleiben als Quellenangabe erhalten;
Einträge mit dem Zusatz „Vorschlag Bot“ werden nicht als bestätigter Kanon
ausgegeben.

## Android-App starten

Mit JDK 17 und Android SDK API 35:

```bash
./gradlew :app:assembleDebug
```

ArtCraft dient als Referenz für visuelles Arbeiten. Die App verwendet Ruff-eigene
Exportdaten und Assets und übernimmt keinen fremden ArtCraft-Quellcode.

## Repo-Struktur

| Pfad | Inhalt |
| --- | --- |
| `app/` | Android-App (Kotlin, Jetpack Compose) |
| `app/src/main/assets/` | `creatures.json`, `rules.json` (vom Importskript erzeugt) |
| `tools/` | Notion-Import (`import_export.py`) und Test |
| `.github/agents/` | Weltbau-Agent für Codespaces |
| `.github/workflows/` | CI: Import-Test und Debug-APK |
| `abacktools-ExportBlock-*.zip` | Notion-Export (Quelle, nicht entpacken) |
| `ref-*.zip` | Referenzmaterial, nicht Teil des App-Builds |

## Kontinuierliche Integration

`.github/workflows/android.yml` testet das Importskript und baut bei jedem Push
und Pull Request eine Debug-APK, die als Artefakt heruntergeladen werden kann.
