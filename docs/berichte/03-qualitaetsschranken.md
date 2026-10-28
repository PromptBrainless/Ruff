# Phase 3 · Qualitätsschranken

## Getan

- Pre-commit mit Markdown-Lint, Secrets-Scan, JSON-Syntax/-Schema, Dateigröße,
  privatem Schlüsselcheck und Flake8 eingerichtet und lokal aktiviert.
- CI-Workflow mit auf Commit-SHAs gepinnten Checkout-, Java- und Python-Actions
  ergänzt. Er führt Python-Tests, Kanon- und Dokumentenlinkprüfung,
  Pre-commit, Android-Build, Android Lint und ktlint aus.
- Android Lint läuft mit `abortOnError`; damit werden auch die standardmäßigen
  Android-Accessibility-Befunde im CI-Lauf geprüft. Ruff verwendet für Python
  Flake8, nicht den gleichnamigen Python-Linter.
- KtLint auf das bestehende Compose-Projekt eingestellt; der vorhandene
  Kotlin-Code wurde ausschließlich formatiert.
- Quellen und Versionen der tatsächlich eingesetzten Werkzeuge werden in
  `docs/quellen.md` weiter nachgewiesen.

## Geprüft

- `pre-commit run --all-files`: alle installierten Hooks erfolgreich;
  Bestiarium-Dateien werden bei späteren Einträgen gegen das Schema geprüft.
- `:app:ktlintMainSourceSetFormat :app:ktlintCheck`: erfolgreich.
- `python -m unittest discover -s tools -v`: 9 Tests erfolgreich.
- `python tools/check_docs_links.py`: erfolgreich.
- `python tools/validate_canon.py`: erfolgreich, derzeit 0 Einträge.
- Der Secrets-Scan fand 0 Kandidaten. Der leere detect-secrets-Baseline-Stand
  wird zusammen mit der Hook-Konfiguration versioniert.

## Offen

- Android-Build und Android-Accessibility-Lint können lokal ohne installiertes
  SDK API 35 nicht ausgeführt werden. Der CI-Job setzt auf das SDK der
  GitHub-Runner-Umgebung; ein erfolgreicher CI-Lauf muss nach Veröffentlichung
  noch bestätigt werden.
- Die zuvor fehlenden Referenzarchive wurden nach dem Abgleich mit
  `origin/main` gefunden. Ausgewählte Listen zu Actions, Git-Hooks, Static
  Analysis und Accessibility wurden als Rechercheeinstieg geprüft; die
  Werkzeugwahl stützt sich weiterhin auf die dokumentierten offiziellen
  Quellen. Kein Referenzinhalt wurde übernommen.
