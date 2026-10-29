# 07 · Sync Bestiarium v2 in die App (Vorschlag)

Stand: 29.10.2026, Quellen-Schnappschuss 13:07 (Europe/Berlin).
Branch: `sync/bestiarium-v2-20261029`. Alle Inhalte sind **Vorschlag** und
nicht bestätigter Kanon.

## Quellen (nur gelesen, nicht verändert)

- `katalog_v2.json`: 118 spielbare Wesen (Stufe II–VI), IDs `V2-001` bis
  `V2-118`.
- `katalog_v2_erweitert.json`: dieselben 118 Wesen unverändert plus 23
  spielbare Wesen der Stufe I (`V2-I-001` bis `V2-I-023`) und 46 nicht
  spielbare Archivwesen der Stufen VI und VII (nur Lore).
- `v2/asset_pipeline/ausgabe/<ID>/`: Porträt, Icon und Cover der Ruff Asset
  Pipeline. Fertig sind `V2-001` bis `V2-056`; die übrigen laufen noch.
- `ui_kit/platzhalter_kapuze.png`: Platzhalter für Wesen ohne Bild.
- `v2/bilder_manifest.csv`: Referenzbilder von Wikimedia Commons, siehe
  [Referenzbilder](../bestiarium-v2-referenzbilder.md).

## Ergebnis

- `app/src/main/assets/creatures.json`: 187 Wesen, davon 141 spielbar
  (118 aus Katalog v2 und 23 der Stufe I) und 46 nur Lore. Jeder Eintrag hat
  `status: Vorschlag`, das Feld `spielbar` und die Bildressourcen `portrait`,
  `icon` und `cover`, sofern vorhanden.
- `app/src/main/res/drawable-nodpi/`: 168 WebP-Bilder für 56 Wesen, jeweils
  Porträt (768 px), Icon (256 px) und Cover (960 × 540 px), dazu
  `ruff_platzhalter_kapuze.webp`.
- Die Detailansicht der App zeigt Cover oder Porträt. Ohne Bild erscheint der
  Platzhalter.
- Die bisherigen App-Wesendaten aus dem Notion-Export liegen unverändert unter
  `archiv/creatures_notion_export_v1.json`.

## Bewusst nicht übernommen

- **Keine Einträge in `bestiarium/daten/`.** Das Schema verlangt
  `core_values`. Für Kernwerte gibt es noch keine beschlossenen Ankerleitern,
  und die Kataloge enthalten keine Kernwerte. Nach `AGENTS.md`,
  `docs/annahmen.md` und der Rolle Bestiarium-Kurator dürfen sie nicht erfunden
  werden. `python tools/validate_canon.py` läuft deshalb weiter mit 0 Einträgen
  durch.
- Die Machtstufen stehen nur als Quellenangabe im App-Feld `powerLabel`.

## Folge-PR

- Bilder für `V2-057` bis `V2-118` (62 Wesen), sobald die Pipeline fertig ist.
- Bilder für Stufe I (23) und, falls gewünscht, für die Archivwesen VI/VII (46).
- Beispielkarte `v2/karten/karte_v2_001_aesops_fuchs.png` und weitere Karten.
- Weitere UI-Kit-Elemente (Rahmen, Trenner, Banner, Texturen) in die App.
- Icons in der Wesenliste anzeigen.
- Bestiariumseinträge nach Schema, sobald Kernwerte beschlossen sind.
