# Ruff · Game Dimension

Ein nativer Android-MVP für das Ruff-Regelwerk und Wesen-Register.

Die App bietet:

## Schnellstart

## Schnellstart

Voraussetzungen: JDK 17, Android SDK (API 35)

```bash
./gradlew :app:assembleDebug
```

Die Debug-APK liegt danach unter `app/build/outputs/apk/debug/`.

## Daten aus dem Notion-Export aktualisieren

1. Die drei `abacktools-ExportBlock-*-part*-of3.zip` entpacken (oder einen frischen Export holen).
2. Den Ordner `Privat und Geteilt/Ruff` an das Importskript übergeben:

```bash
python tools/import_export.py ".source-export/Privat und Geteilt/Ruff"
```

Das Skript erzeugt die JSON-Daten in `app/src/main/assets` und kopiert die benötigten Design-Assets nach `app/src/main/res/drawable-nodpi`.

- Leere Wesen-Platzhalter werden übersprungen.
- Machtstufen bleiben als Quellenangabe erhalten.
- Einträge mit dem Zusatz „Vorschlag Bot“ werden nicht als bestätigter Kanon ausgegeben.

**Hinweis zu den großen ZIP-Dateien:** Die Roh-Exporte und Referenz-ZIPs sind im `.gitignore` und sollten nicht dauerhaft im Repo liegen (sie blähen die History auf). Für lokale Arbeit genügt ein einmaliger Download.

## Projektstruktur

```
app/src/main/java/de/ruff/dimension/   # Compose-UI (aktuell noch in MainActivity.kt)
app/src/main/assets/                   # creatures.json + rules.json
tools/import_export.py                 # Notion-Export → App-Daten
.github/agents/ruff-worldbuilding.agent.md  # Agent-Prompt für Lore & Regeln
```

## Nächste sinnvolle Schritte

- Code in Screens, ViewModel und Theme aufteilen
- LazyColumn + Sortierung für die Listen
- Favoriten und visuelle Kennzeichnung von „Vorschlag Bot“-Einträgen
- CI läuft bereits (siehe `.github/workflows/android.yml`)

## Agent / Worldbuilding

Der Prompt unter `.github/agents/ruff-worldbuilding.agent.md` ist für konsistente Lore- und Regelarbeit mit Copilot, Claude o.Ä. gedacht. Er respektiert den aktuellen Kanon (v3) und trennt SL- und Spielerinformationen.

ArtCraft dient nur als visuelle Referenz. Die App verwendet ausschließlich Ruff-eigene Exportdaten und Assets.
