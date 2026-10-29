# Quellen und Lizenzen

Dieses Verzeichnis dokumentiert externe Quellen, die bei der Projektarbeit
tatsächlich geprüft oder verwendet wurden. Ruff-eigene Notion-Exporte und vom
Auftraggeber vorgegebene Regeln sind Projektquellen, keine externen Vorlagen.

| Quelle | Verwendung | Lizenz / Status |
|---|---|---|
| `.source-export/Privat und Geteilt/Ruff/` (vom Auftraggeber bereitgestellter Notion-Export) | Bestehende Projekt- und Importdaten; der Export wurde unverändert im ignorierten Quellbereich erhalten. | Nutzungs-/Veröffentlichungslizenz wurde nicht unabhängig geprüft; Inhalte werden nicht als neue Ruff-Lore veröffentlicht. |
| [Android Developers · Android Studio](https://developer.android.com/studio) | Prüfung der offiziellen Android-SDK-Installationsmöglichkeit und Lizenzvoraussetzung am 27.10.2026. | Offizielle Dokumentation; SDK-Lizenzbedingungen sind nicht als Projektlizenz übernommen. |
| [Android Developers · SDK Manager](https://developer.android.com/tools/sdkmanager) und [Android CLI SDK](https://developer.android.com/tools/agents/android-cli/commands/sdk) | Offizielle Paketverwaltung und aktueller CLI-Befehl für die Devcontainer-Anleitung geprüft, 27.10.2026. | SDK-Komponenten unterliegen separaten Lizenzbedingungen; keine Inhalte oder Tools daraus kopiert. |
| [Ironsworn · Licensing](https://tomkinpress.com/pages/licensing) | Produktabhängige Lizenzlage für Strukturvergleich geprüft, 27.10.2026. | Konkrete Produktlizenz der SRD-Datei nicht verifiziert; keine Inhalte übernommen. |
| [D&D Beyond · SRD](https://www.dndbeyond.com/srd) | Offizielle SRD-Seite und Versions-/Lizenzhinweis geprüft, 27.10.2026. | SRD 5.2.1: CC BY 4.0 laut Rechteinhaber; nicht auf andere D&D-Inhalte übertragen. |
| [Blades in the Dark · Licensing](https://bladesinthedark.com/licensing) | Offizielle SRD-Lizenzseite geprüft, 27.10.2026. | SRD-Text: CC BY 3.0 Unported, mit den dort beschriebenen Grenzen. |
| [Justin Alexander · Don't Prep Plots](https://thealexandrian.net/wordpress/4147/roleplaying-games/dont-prep-plots) | Rollenspiel-Vorbereitung als Situation statt Ereigniskette eingeordnet, 27.10.2026. | Keine offene Lizenz auf der geprüften Seite ausgewiesen; keine Textstellen übernommen. |
| [Android Developers · Search](https://developer.android.com/develop/ui/views/search) und [Room FTS](https://developer.android.com/training/data-storage/room/defining-data#fts) | Suche über Datenquellen und lokale Volltextsuche geprüft, 27.10.2026. | Offizielle Dokumentation; auf den abgerufenen Seiten keine konkrete Inhaltslizenz verifiziert; kein Beispielcode übernommen. |
| [Android Developers · Accessibility](https://developer.android.com/guide/topics/ui/accessibility) und [Compose Semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics) | Semantik und barrierefreie UI-Prüfung geprüft, 27.10.2026. | Offizielle Dokumentation; keine Text- oder Codeübernahme. |
| [markdownlint-cli2](https://github.com/DavidAnson/markdownlint-cli2) | Markdown-Hook v0.24.0. | MIT, `LICENSE` im Repository geprüft. |
| [pre-commit-hooks](https://github.com/pre-commit/pre-commit-hooks) und [pre-commit](https://github.com/pre-commit/pre-commit) | Hook-Runtime v4.7.0 und Standardprüfungen v6.0.0. | MIT laut jeweiligen `LICENSE`-Dateien. |
| [detect-secrets](https://github.com/Yelp/detect-secrets) und [check-jsonschema](https://github.com/python-jsonschema/check-jsonschema) | Geheimnisprüfung v1.5.0 und Schema-Prüfung v0.39.0. | Apache-2.0 laut jeweiligen `LICENSE`-Dateien. |
| [Flake8](https://github.com/PyCQA/flake8) | Python-Lint v7.3.0; bewusst nicht Ruff-Linter. | MIT laut `LICENSE`. |
| [ktlint-gradle](https://github.com/JLLeitschuh/ktlint-gradle) und [ktlint](https://github.com/pinterest/ktlint) | Kotlin-Format-/Lint-Plugin v12.1.2 und ktlint v1.5.0. | MIT laut `LICENSE.txt` des Gradle-Plugins und `LICENSE` von ktlint. |
| [actions/checkout](https://github.com/actions/checkout/tree/11bd71901bbe5b1630ceea73d27597364c9af683), [actions/setup-java](https://github.com/actions/setup-java/tree/c5195efecf7bdfc987ee8bae7a71cb8b11521c00) und [actions/setup-python](https://github.com/actions/setup-python/tree/a26af69be951a213d495a4c3e4e4022e16d87065) | CI-Actions, jeweils auf unveränderliche Commit-SHAs gepinnt. | MIT laut den geprüften `LICENSE`-Dateien der Releases. |
| [Dev Containers base image](https://github.com/devcontainers/images) sowie [Java-Feature](https://github.com/devcontainers/features/tree/main/src/java) und [Python-Feature](https://github.com/devcontainers/features/tree/main/src/python) | Basiscontainer, JDK 17 und Python 3.12 im Codespace. | MIT laut `LICENSE` der Dev-Containers-Repositories; enthaltene Betriebssystempakete behalten ihre eigenen Lizenzen. |
| [paperclipai/paperclip](https://github.com/paperclipai/paperclip) | Agentenverträge und Zuständigkeitsgrenzen als Strukturvergleich, geprüft im bereitgestellten Snapshot unter `ref/1-*` und `ref/2-*`. | MIT laut enthaltenem `LICENSE` in Archiv 2. Keine Vorlagentexte übernommen. |
| [cobusgreyling/grok-bot-templates](https://github.com/cobusgreyling/grok-bot-templates) | Strukturvergleich für Agentenrollen mit Job, Grenzen und Freigabe, im bereitgestellten Snapshot geprüft. | MIT laut enthaltenem `LICENSE`. Keine Inhalte übernommen. |
| [sdras/awesome-actions](https://github.com/sdras/awesome-actions) | Rechercheeinstieg für GitHub-Actions-Auswahl. | CC0 1.0 laut Snapshot-`LICENSE`; keine Listeneinträge übernommen. |
| [compscilauren/awesome-git-hooks](https://github.com/compscilauren/awesome-git-hooks) | Rechercheeinstieg für Hook-Kategorien. | Snapshot-`LICENSE` nennt CC0 1.0, `package.json` nennt ISC. Widersprüchliche Angaben; keine Inhalte übernommen. |
| [analysis-tools-dev/static-analysis](https://github.com/analysis-tools-dev/static-analysis) | Rechercheeinstieg für Static-Analysis-Werkzeuge. | MIT laut Snapshot-`LICENSE`; keine Inhalte übernommen. |
| [dustinspecker/awesome-eslint](https://github.com/dustinspecker/awesome-eslint) | Allgemeine Lint-Recherche; Ruff verwendet Flake8 statt den gleichnamigen Python-Linter. | CC0 1.0 laut Snapshot-`license`; keine Inhalte übernommen. |
| [matiassingers/awesome-readme](https://github.com/matiassingers/awesome-readme) | README-Struktur als Recherchehinweis. | Im bereitgestellten Snapshot keine Lizenzdatei gefunden; keine Inhalte übernommen. |
| [BubuAnabelas/awesome-markdown](https://github.com/BubuAnabelas/awesome-markdown) | Markdown-Werkzeugrecherche. | CC0 1.0 laut Snapshot-`LICENSE.md`; keine Inhalte übernommen. |
| [brunopulis/awesome-a11y](https://github.com/brunopulis/awesome-a11y) | Recherche zu Barrierefreiheit. | CC0 1.0 laut Snapshot-`license`; keine Inhalte übernommen. |
| [storytold/vectorcraft](https://github.com/storytold/vectorcraft) | Niedrig priorisierter Vergleich für plattformübergreifende Produkt-/Asset-Organisation. | MIT oder Apache-2.0 laut Projekt-README und `LICENSE-MIT`/`LICENSE-APACHE`; einzelne Assets und Abhängigkeiten können eigene Lizenzen haben. Keine Assets oder Implementierung übernommen. |
| [Wikimedia Commons](https://commons.wikimedia.org/) (Referenzbilder Bestiarium v2) | Nur als Referenz für Steckbriefe und Bildgenerierung, Liste in [bestiarium-v2-referenzbilder.md](bestiarium-v2-referenzbilder.md); keine Bilder im Repository. | Lizenzen je Bild laut Manifest (überwiegend gemeinfrei, sonst CC BY/CC BY-SA/CC0/FAL); nicht einzeln neu geprüft. |

Die vier bereitgestellten Referenzarchive liegen lokal unter `ref/` und sind
ignoriert. Die Lizenzangaben oben beziehen sich auf enthaltene Lizenzdateien im
Snapshot; sie sind nicht pauschal auf andere Repositories oder einzelne Assets
übertragbar. Nicht einzeln geprüfte Projekte daraus bleiben ungenutzt. Weitere
Recherchequellen werden mit Datum, Link und geprüfter Lizenz in
`docs/recherche/` erfasst.
