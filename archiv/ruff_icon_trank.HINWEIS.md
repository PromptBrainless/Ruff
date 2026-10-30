# Hinweis: `ruff_icon_trank.jpg` (defekt)

- **Befund (30.10.2026):** Die Datei ist kein gültiges JPG. Sie hat nur
  200 Bytes und enthält den abgeschnittenen Anfang eines Base64-Texts
  (`/9j/4QfsRXhpZgAA…`) statt Binärdaten. Kein Bildbetrachter und kein
  Android-Gerät kann sie anzeigen.
- **Herkunft:** Commit `caa55e1` („Add ruff_icon_trank.jpg (Heiltrank,
  Eisen-Glühen-Stil)“, 28.10.2026), damals unter
  `app/src/main/res/drawable-nodpi/`.
- **Verwendung:** Weder App-Code noch `creatures.json` verweisen darauf.
- **Vorgehen:** Unverändert hierher verschoben (nicht gelöscht). Sobald das
  Originalbild auftaucht, als WebP (Icon max. 256 px) wieder unter
  `app/src/main/res/drawable-nodpi/` ablegen und diese Datei samt Hinweis
  entfernen oder aktualisieren (Vorschlag).
