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
