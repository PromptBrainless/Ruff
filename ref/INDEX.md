

# Referenz-Repositories

Stand: 27.10.2026. Die Archive fehlten zunächst in der lokalen Arbeitskopie,
deren `main` vier Commits hinter `origin/main` lag. Nach dem Abgleich wurden
die vier Archive geprüft und in je einen ignorierten Unterordner extrahiert.
Die Snapshot-Inhalte sind nicht Teil des Ruff-Repositories; nur dieser Index
wird versioniert. Es wurden keine Fremdtexte, Regeln, Namen oder Quelldateien
übernommen.

## Archive

| Archiv | Zweck und Stack | Lizenz | Nutzbare Muster | Ruff-Relevanz |
|---|---|---|---|---|
| [1 · Paperclip Server und kleine Repositories](./1-paperclip-server-und-kleine-repos/) | Sammlung mehrerer Projekte, darunter der serverseitige Teil von Paperclip und kleinere Agenten-/Kollaborations-Repositories; heterogene TypeScript/Node-, Rust- und Web-Stacks. | Je enthaltenem Repository unterschiedlich; für Paperclip siehe unten. | Agentenverträge und klare Zuständigkeitsgrenzen; nur als Strukturbeobachtung. | Hoch für Agentenrollen, sonst gering. |
| [2 · Paperclip REST](./2-paperclip-rest/) | Ergänzende Momentaufnahme des Paperclip-Monorepos, TypeScript/Node.js und pnpm. | MIT laut enthaltenem `LICENSE` im Snapshot. | Rollen, Grenzen und getrennte Verantwortlichkeiten; keine Vorlagentexte übernommen. | Hoch für Agentenverträge. |
| [3 · VectorCraft](./3-vectorcraft/) | Native Vektorzeichen-Anwendung, überwiegend Rust, mit Desktop- und Web-Zielen. | MIT oder Apache-2.0 laut Projekt-README und beiden Lizenzdateien; einzelne Assets und Abhängigkeiten können eigene Lizenzen haben. | Produkt-/Asset-Organisation und plattformübergreifende Projektstruktur. | Gering; keine Assets oder Implementierung übernommen. |
| [4 · Awesome-Listen](./4-awesome-listen/) | Sammlung kuratierter Listen zu Actions, Git-Hooks, Static Analysis, ESLint, Markdown, READMEs und Barrierefreiheit; überwiegend Markdown, aber kein gemeinsamer Stack. | Pro enthaltenem Repository unterschiedlich; siehe geprüfte Auswahl unten. | Kategorien und Auswahl geeigneter Qualitätswerkzeuge als Rechercheeinstieg. | Mittel für Qualitätsschranken; Einträge nicht ungeprüft übernehmen. |

## Geprüfte Einzel-Repositories

| Repository | Zweck / Stack | Lizenzbefund im Snapshot | Muster und Relevanz |
|---|---|---|---|
| [paperclipai/paperclip](https://github.com/paperclipai/paperclip) | Agenten-Orchestrierung; TypeScript/Node.js, pnpm. Die Archive 1 und 2 enthalten verschiedene Teile desselben Repositories. | MIT laut `LICENSE` in Archiv 2. | Rollen-/Aufgabenverträge als Strukturvorbild; hoch. |
| [cobusgreyling/grok-bot-templates](https://github.com/cobusgreyling/grok-bot-templates) | Agenten- und Teamvorlagen; Markdown und Node.js. | MIT laut `LICENSE`. | Auftrag, Quellen, Grenzen und Freigabe trennscharf formulieren; hoch. Keine Texte kopiert. |
| [sdras/awesome-actions](https://github.com/sdras/awesome-actions) | Kuratierte GitHub-Actions-Liste; Markdown. | CC0 1.0 laut `LICENSE`. | Actions als Recherchehinweise; mittel. |
| [compscilauren/awesome-git-hooks](https://github.com/compscilauren/awesome-git-hooks) | Git-Hook-Ressourcen; Markdown/Node.js. | Snapshot enthält CC0-`LICENSE`, `package.json` nennt jedoch ISC. Der Lizenzbefund ist widersprüchlich; keine Inhalte übernommen. | Hook-Kategorien als Recherchehinweis; mittel. |
| [analysis-tools-dev/static-analysis](https://github.com/analysis-tools-dev/static-analysis) | Katalog statischer Analysewerkzeuge; Markdown. | MIT laut `LICENSE`. | Werkzeugauswahl; mittel. |
| [dustinspecker/awesome-eslint](https://github.com/dustinspecker/awesome-eslint) | ESLint-Ressourcen; Markdown/Node.js. | CC0 1.0 laut `license`. | Lint-Kategorien; gering, da Ruff Python/Kotlin nutzt. |
| [matiassingers/awesome-readme](https://github.com/matiassingers/awesome-readme) | README-Beispiele; Markdown. | Im Snapshot keine Lizenzdatei gefunden; keine Inhalte übernommen. | Gliederung als unverbindlicher Recherchehinweis; gering. |
| [BubuAnabelas/awesome-markdown](https://github.com/BubuAnabelas/awesome-markdown) | Markdown-Ressourcen; Markdown/Node.js. | CC0 1.0 laut `LICENSE.md`. | Markdown-Werkzeugrecherche; gering. |
| [brunopulis/awesome-a11y](https://github.com/brunopulis/awesome-a11y) | Barrierefreiheitsressourcen; Markdown. | CC0 1.0 laut `license`. | Recherche zu Accessibility-Prüfung; mittel. |

Die Archive enthalten weitere, hier nicht einzeln geprüfte Repositories. Deren
Lizenzen sind nicht aus den obigen Einträgen ableitbar; vor einer späteren
Nutzung müssen Quelle und Lizenz separat geprüft werden. Der Snapshot-Index
unter den jeweiligen Archivordnern nennt die enthaltenen Projekte. Die
Referenzverzeichnisse werden durch `.gitignore` ausgeschlossen.
