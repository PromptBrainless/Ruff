# Ruff · Game Dimension

Ruff ist ein eigenständiges Dark-Fantasy-Pen-and-Paper-Projekt: Weltbibel,
Regelwerk, Wesenregister und eine native Android-App zum Durchsuchen der
Regel- und Exportdaten. Der vorhandene App-MVP bietet Suche, Machtstufenfilter,
Detailansichten, durchsuchbare Regeltexte und Ruff-Design-Assets im
Eisen-/Ruß-/Höllenrot-Thema.

## Schnellstart

### Notion-Export importieren

Der entpackte Export muss die Verzeichnisse `Wesen & Figuren` und `Regeln`
enthalten. Die Quelldaten liegen lokal unter
`.source-export/Privat und Geteilt/Ruff` und sind nicht Teil der Veröffentlichung.

```bash
python tools/import_export.py ".source-export/Privat und Geteilt/Ruff"
python -m unittest discover -s tools -v
```

Der Import erzeugt `app/src/main/assets/creatures.json` und `rules.json` und
kopiert Ruff-Design-Assets nach `app/src/main/res/drawable-nodpi`. Leere
Wesen-Platzhalter werden übersprungen. Die Machtstufe bleibt Quellenangabe;
der Zusatz „Vorschlag Bot“ wird niemals als bestätigter Kanon markiert.

### Android-App bauen

Benötigt werden JDK 17 und Android SDK API 35. SDK-Lizenzen müssen lokal
angenommen sein.

```bash
./gradlew :app:assembleDebug
```

ArtCraft dient als Referenz für visuelles Arbeiten. Die App verwendet Ruff-eigene
Exportdaten und Assets und übernimmt keinen fremden ArtCraft-Quellcode.

## Projektstruktur

- `welt/`: Kanon, Orte, Fraktionen, Zeitleiste, Namen und Stilguide
- `regelwerk/`: Regeln, Skalenanker und Beispiele
- `bestiarium/`: Schema und einzeln gepflegte JSON-Einträge
- `tools/`: Notion-Import, Validierung und Tests
- `app/`: native Android-App und importierte Laufzeitdaten
- `docs/`: Werkzeuginventar, Quellen, Annahmen, Recherche und Phasenberichte
- `.github/agents/`: Agentenrollen mit Quellen- und Freigabeverträgen
- `ref/`: ignorierter lokaler Bereich für bereitgestellte Referenz-Repositories
- `.source-export/`: ignorierte private Quellexporte
- `archiv/`: aussortierte, nachvollziehbar archivierte Projektdateien

## Kanon und Status

Vorgaben für Zusammenarbeit stehen in [AGENTS.md](AGENTS.md). Neue Inhalte sind
**Vorschläge**, bis sie ausdrücklich bestätigt wurden. Für Bestiariumseinträge
gilt [bestiarium/schema.json](bestiarium/schema.json); die Prüfung erfolgt mit:

```bash
python tools/validate_canon.py
```

Arbeitsentscheidungen, geprüfte Werkzeuge und Quellen stehen in `docs/`.
Der optionale 3D-Modellworkflow mit reproduzierbarer Stable-Fast-3D-Pipeline,
Voraussetzungen und aktuellen Blockaden ist in
[docs/06-stable-fast-3d.md](docs/06-stable-fast-3d.md) beschrieben.

## Kontinuierliche Integration

Die Workflows unter `.github/workflows/` testen und bauen die Android-App bei
Pull Requests und Pushes. Der Qualitätsworkflow prüft zusätzlich Python,
Dokumentationslinks, Schema und Kotlin-Formatierung.
