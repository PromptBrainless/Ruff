# Phase 1 · Ordnen

## Getan

- Die drei vorhandenen Notion-Export-ZIPs wurden nach
  `.source-export/archives/` verschoben. Sie liegen dort weiterhin auf der
  Festplatte, sind durch die bestehende Ignore-Regel ausgeschlossen und wurden
  aus der Versionsverfolgung entfernt; die Historie wurde nicht umgeschrieben.
- Das bereits entpackte Notion-Verzeichnis liegt am README-Zielpfad
  `.source-export/Privat und Geteilt/Ruff`.
- `ref/` wurde in `.gitignore` aufgenommen und `ref/INDEX.md` angelegt.
- Nach dem Abgleich des zunächst vier Commits veralteten Branches mit
  `origin/main` wurden die vier Referenzarchive gefunden. Jedes wurde geprüft,
  in den passenden ignorierten Unterordner von `ref/` entpackt und in
  `ref/INDEX.md` mit ausgewählten Lizenzbefunden und Mustern erfasst.
- Die Referenz-ZIPs werden in ihren jeweiligen ignorierten `ref/`-Ordnern
  erhalten; die Root-Dateien werden aus der Versionsverfolgung entfernt.

## Geprüft

- Die Archive wurden mit `unzip -t` geprüft; die Pfadlisten enthielten keine
  absoluten oder über `..` ausbrechenden Einträge.
- Der vorhandene Quelltext-Export ist weiterhin lesbar; Import und Tests waren
  bereits in Phase 0 erfolgreich.

## Offen

- Lizenzen und Inhalte der übrigen, nicht einzeln geprüften Repositories in
  den Sammlungen bleiben offen; sie wurden nicht verändert oder übernommen.
