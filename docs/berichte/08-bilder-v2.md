# 08 · Bilder Bestiarium v2, Folge-PR (Vorschlag)

Stand: 29.10.2026, Quellen-Schnappschuss 14:27 (Europe/Berlin).
Branch: `sync/bilder-v2-20261029`, gestapelt auf
`sync/bestiarium-v2-20261029` (PR #8). Alle Bilder sind **Vorschlag**.

## Ergebnis

- 233 neue WebP-Bilder in `app/src/main/res/drawable-nodpi/`, im gleichen
  Format wie in PR #8: Porträt mit 768 px, Icon mit 256 px, Cover mit
  960 × 540 px.
  - V2-057 bis V2-118: 62 Wesen mit je drei Bildern.
  - Stufe I: 13 Wesen. Bei V2-I-012 Aziza fehlt das Icon; dort steht in der
    Liste die Ruff-Glyphe.
  - Stufe VI: 3 Archivwesen, nur Lore (V2-VI-003 Perun, V2-VI-010 Kami,
    V2-VI-011 Nun).
- Zusammen mit PR #8 haben jetzt 134 von 187 Wesen Bilder. Es sind 401
  Wesenbilder plus der Platzhalter.
- Die Wesenliste zeigt das Icon. Ohne Icon bleibt die bisherige Glyphe. Die
  Detailansicht zeigt Cover oder Porträt, sonst den Platzhalter.

## Abgleich der 16 Zusatz-Bildordner

Die Pipeline hat 16 Wesen außerhalb von V2-001 bis V2-118 bebildert. Alle IDs
stimmen mit `katalog_v2_erweitert.json` überein:

- **Stufe I:** 13 von 23 haben Bilder. Ohne Bilder sind I-001 Brownie,
  I-002 Irrlicht, I-003 Klabautermann, I-004 Heinzelmännchen, I-005 Nisse,
  I-007 Kodama, I-008 Tsukumogami, I-015 Duende, I-018 Pombero und
  I-022 Nasnas.
- **Stufe VI:** 3 von 19 haben Bilder.
- **Stufe VII:** keines der 27 Wesen hat Bilder, auch V2-VII-009
  Weltenverschlinger nicht (laut Pipeline fehlt dafür der Prompt).
- 53 Wesen behalten den Platzhalter: 10 aus Stufe I, 16 aus VI und 27 aus VII.
