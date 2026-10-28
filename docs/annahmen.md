# Annahmen und offene Entscheidungen

Stand: 27.10.2026

## Getroffene Arbeitsannahmen

- Der entpackte Export unter `.source-export/Privat und Geteilt/Ruff` ist die
  lokal bereitgestellte Importquelle; seine Inhalte werden nicht inhaltlich
  umgeschrieben.
- Drei bereits versionierte Notion-ZIP-Teile wurden in den ignorierten
  Quellbereich verschoben und aus der Versionsverfolgung entfernt. Sie bleiben
  lokal erhalten; die Git-Historie bleibt unverändert.
- Der Importer darf eine ergänzende Statusangabe ausgeben. Nur ein expliziter
  Status `Bestätigt` ohne den Marker `Vorschlag Bot` wird als `bestätigt`
  klassifiziert; unbekannte und Entwurfsstatus bleiben `Vorschlag`.
- Solange für Kernwerte keine vollständigen Attribut-Ankerleitern beschlossen
  wurden, werden keine Werte oder Attribute für neue Bestiariumseinträge
  erfunden.
- Die Referenzarchive aus `origin/main` werden lokal unter `ref/` ausgewertet;
  Referenzinhalte bleiben ignoriert und werden nicht in Ruff-Inhalte kopiert.

## Offen

- Lizenzen der nicht einzeln geprüften Repositories in den Referenzsammlungen
  sind offen; daraus wird bis zur Einzelprüfung nichts übernommen.
- Die Android-SDK-Lizenz wurde nicht stellvertretend angenommen; ohne lokal
  installiertes API 35 ist der Android-Build nicht ausführbar.
- Der lokale GitHub-CLI-Zugang ist nicht angemeldet. PR-Veröffentlichung bleibt
  bis zur Authentifizierung offen.
