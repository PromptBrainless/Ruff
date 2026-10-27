# Phase 0 · Bestandsaufnahme und Werkzeuginventar

## Getan

- README, vorhandene Agentenrolle, Importwerkzeug, Importtests und Gradle-Konfiguration gelesen.
- Arbeitsbranch `init/grundgeruest` angelegt, ohne die Änderungen im Arbeitsbaum zu verwerfen.
- Werkzeuge und Ausgangsdaten in [00-werkzeuge.md](../00-werkzeuge.md) inventarisiert.
- JDK 17 aus den System-Paketquellen installiert, da der initiale Build mit dem voreingestellten JDK 25 an einer Versionsparser-Ausnahme scheiterte.
- Den vorhandenen Notion-Export importiert: 131 Wesen und 12 Regel-Seiten; zwei leere Platzhalter wurden übersprungen.

## Geprüft

- `python tools/import_export.py ".source-export/Privat und Geteilt/Ruff"`: erfolgreich.
- `python -m unittest discover -s tools -v`: 2 Tests erfolgreich.
- `./gradlew :app:assembleDebug`: zuerst fehlgeschlagen mit `java.lang.IllegalArgumentException: 25.0.4.1`.
- Derselbe Build mit JDK 17: fehlgeschlagen, weil kein Android SDK konfiguriert ist.
- In der zunächst ausgecheckten lokalen Basis fehlten die vier Referenz-ZIPs;
  gefunden wurden stattdessen drei Notion-Export-ZIPs. Die Basis lag vier
  Commits hinter `origin/main`. Nach dem Abgleich wurden die Referenzarchive
  auf `origin/main` gefunden; sie wurden vor Phase 1 lokal geprüft und
  extrahiert.
- `gh auth status`: keine GitHub-Anmeldung.

## Offen

- Android SDK API 35 erfordert die Annahme der offiziellen SDK-Lizenz; diese wurde nicht ohne Zustimmung angenommen.
- Die Lizenzen nicht einzeln geprüfter Projekte aus den Referenzsammlungen
  bleiben offen; es wurden daraus keine Inhalte übernommen.
- Pull-Request-Erstellung erfordert eine GitHub-Anmeldung oder einen autorisierten Connector.
