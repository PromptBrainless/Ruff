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
- Das 3D-Manifest verwendet ausschließlich exportierte Notion-Seiten-IDs als
  stabile Wesen-IDs. Bildzuordnungen erfolgen nur über eigene Seite oder
  eindeutigen exakten Dateinamen; mehrdeutige und fehlende Bilder bleiben
  unzugeordnet.
- Stable Fast 3D bleibt bis zur Lizenzprüfung gesperrt. Eine vorhandene
  Bilddatei oder GLB allein belegt weder Kanonfreigabe noch erfolgreiche
  Generierung.
- Die Notion-Integration aktualisiert bestehende Seiten nur nach
  ausdrücklicher Asset-Hosting-Freigabe, nachweisbarer HTTPS-Erreichbarkeit
  und erfolgreicher lokaler GLB-Validierung.

## Offen

- Lizenzen der nicht einzeln geprüften Repositories in den Referenzsammlungen
  sind offen; daraus wird bis zur Einzelprüfung nichts übernommen.
- Die Android-SDK-Lizenz wurde nicht stellvertretend angenommen; ohne lokal
  installiertes API 35 ist der Android-Build nicht ausführbar.
- Der lokale GitHub-CLI-Zugang ist nicht angemeldet. PR-Veröffentlichung bleibt
  bis zur Authentifizierung offen.
- Für 3D-Inferenz fehlen im geprüften Codespace CUDA-/MPS-GPU,
  SF3D-Installation und Modellgewichte, Hugging-Face- und Notion-Zugangsdaten,
  ein Khronos-Validator sowie ein autorisierter Asset-Host. Es ist kein
  separater GPU-Worker verfügbar.
