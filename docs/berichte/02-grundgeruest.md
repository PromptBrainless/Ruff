# Phase 2 · Grundgerüst

## Getan

- Grundregeln für Agenten, Projektstruktur, Weltbereiche, Stilguide, Regelwerk,
  Skalenanker, Bestiarium-Schema, Annahmen, Quellen und Archiv angelegt.
- Die Beispiele im Stilguide sind ausdrücklich keine Weltfakten; die offenen
  Attribut-Anker wurden nicht ergänzt oder erfunden.
- Den Importer additiv um ein Statusfeld ergänzt: Nur ein expliziter
  `Bestätigt`-Status ohne den Marker `Vorschlag Bot` wird `bestätigt`; alle
  Entwürfe und unbekannten Statuswerte bleiben `Vorschlag`.
- App-Code und Notion-Inhalte nicht verändert. Das zusätzliche Datenfeld ist
  mit dem vorhandenen Gson-Modell kompatibel.

## Geprüft

- `python -m unittest discover -s tools -v`: 7 Tests erfolgreich.
- Import erzeugt weiterhin 131 Wesen und 12 Regelseiten; alle 131 Einträge
  tragen gemäß Quellstatus `Vorschlag`.
- Separater Prüfschritt bestätigt, dass kein Eintrag mit `Vorschlag Bot` als
  `bestätigt` ausgegeben wird.
- `python tools/validate_canon.py`: erfolgreich bei derzeit 0
  schema-konformen neuen Bestiariumseinträgen.
- `bestiarium/schema.json` ist syntaktisch valides JSON.

## Offen

- Die bestehenden Notion-Datensätze wurden nicht ins neue Bestiarium-Schema
  kopiert, da sie nicht die beschlossenen Kernwerte enthalten.
- Vollständige Attribut-Ankerleitern und echte Kanon-Inhalte bleiben
  unbestätigt und müssen später festgelegt werden.
