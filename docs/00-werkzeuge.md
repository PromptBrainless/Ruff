# Werkzeuginventar

Stand: 27.10.2026

## Verfügbar

| Bereich | Befund |
|---|---|
| Terminal und Git | Linux-Terminal und Git 2.55.0 verfügbar. Arbeitsbranch `init/grundgeruest`; der lokale Ausgangsstand war gegenüber `origin/main` vier Commits zurück. |
| GitHub CLI | `gh` 2.100.0 verfügbar, aber nicht angemeldet (`gh auth status`: „You are not logged into any GitHub hosts.“). |
| Java und Gradle | JDK 17.0.20.1 wurde aus den System-Paketquellen installiert. Der Gradle Wrapper verwendet Gradle 8.9. |
| Android SDK | Nicht vorhanden; weder `ANDROID_HOME` noch `ANDROID_SDK_ROOT` ist gesetzt. Die Installation der offiziellen SDK-Komponenten setzt die Annahme der Android-SDK-Lizenz voraus; diese wurde nicht stellvertretend angenommen. |
| Devcontainer | `.devcontainer/devcontainer.json` richtet JDK 17 und Python 3.12 ein. Der Pfad für das Android SDK ist vorgesehen; API 35 wird erst nach persönlicher Lizenzannahme installiert. |
| Python | Python 3.14.2 verfügbar. |
| Import und Tests | Notion-Import und beide vorhandenen `unittest`-Tests laufen erfolgreich (131 Wesen, 12 Regel-Seiten; zwei leere Platzhalter übersprungen). |
| Web-Recherche | Webseitenabruf über `web_fetch` funktioniert; die offizielle Android-Entwicklerseite war erreichbar. Eine eigenständige Websuchschnittstelle ist in den verfügbaren Werkzeugen nicht aufgetaucht. |
| MCP | GitHub-MCP-Suche und -Lesezugriff sowie Pylance-MCP-Werkzeuge sind verfügbar. |
| Skills | Im Werkzeugkontext sichtbar: `add-python-version-support`, `address-pr-comments`, `agent-customization`, `analyze-prompt`, `create-pull-request`, `drop-python-version-support`, `fix-customization-evaluation-diagnostics`, `form-github-search-query`, `pylance-docs`, `pylance-python-profiling`, `pylance-refactoring`, `python-add-type-annotations`, `python-fact-grounded-coding`, `python-type-inference`, `python-write-stubs`, `show-github-search-result`, `suggest-fix-issue`, `summarize-github-issue-pr-notification` und `troubleshoot`. Die vorhandene Ruff-Rolle wurde gelesen und archiviert; sie ist kein geladener Skill. |
| Pre-commit | In Phase 0 zunächst nicht installiert und kein Manifest vorhanden. In Phase 3 mit `requirements-dev.txt` eingerichtet, aktiviert und mit Hooks auf feste Versionen konfiguriert. |

## Build-Prüfung

Der erste Lauf mit dem voreingestellten JDK 25.0.4.1 scheiterte in Kotlin mit:

```text
* What went wrong:
25.0.4.1
java.lang.IllegalArgumentException: 25.0.4.1
```

Nach Installation und expliziter Auswahl von JDK 17 scheiterte der Build an der fehlenden Android-SDK-Angabe:

```text
Could not determine the dependencies of task ':app:compileDebugJavaWithJavac'.
> SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at '/workspaces/Ruff/local.properties'.
```

Die Android-SDK-Lizenz wurde nicht angenommen und es wurde kein SDK installiert.

## Ausgangsdaten und offene Verfügbarkeit

- Der entpackte Export liegt bereits unter `.source-export/Privat und Geteilt/Ruff`; der Importbefehl aus der README funktioniert damit.
- Im Root wurden drei `abacktools-ExportBlock-…-part*-of3.zip`-Quellexportdateien gefunden und in den ignorierten `.source-export/archives/`-Bereich verschoben. Die vier Referenzarchive fehlten zunächst, weil der lokale Ausgangsstand vier Commits hinter `origin/main` lag. Nach dem Abgleich wurden sie dort gefunden, geprüft und unter `ref/` extrahiert; die Snapshots bleiben ignoriert.
- `.github/agents/ruff-worldbuilding.agent.md` war bereits vorhanden und wurde vor Änderungen gelesen.
- `markdownlint`, `actionlint`, `pre-commit` und die Android-SDK-Werkzeuge waren anfangs nicht verfügbar.
- Für `gh` ist keine Anmeldung eingerichtet. Erstellen oder Veröffentlichen eines Pull Requests kann daher eine Anmeldung erfordern.
