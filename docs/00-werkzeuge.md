# Werkzeuginventar

Stand: 27.10.2026

## Verfügbar

| Bereich | Befund |
|---|---|
| Terminal und Git | Linux-Terminal und Git 2.55.0 verfügbar. Arbeitsbranch `init/grundgeruest`; der lokale Ausgangsstand war gegenüber `origin/main` vier Commits zurück. |
| GitHub CLI | `gh` 2.100.0 verfügbar, aber nicht angemeldet (`gh auth status`: „You are not logged into any GitHub hosts.“). |
| Java und Gradle | JDK 17.0.20.1 ist installiert; `JAVA_HOME` zeigt aktuell standardmäßig auf JDK 25.0.4.1. Der Gradle Wrapper verwendet Gradle 8.9. |
| Android SDK | Nicht vorhanden; weder `ANDROID_HOME` noch `ANDROID_SDK_ROOT` ist gesetzt. Die Installation der offiziellen SDK-Komponenten setzt die Annahme der Android-SDK-Lizenz voraus; diese wurde bisher nicht bestätigt. |
| Devcontainer | `.devcontainer/devcontainer.json` richtet JDK 17 und Python 3.12 ein. Der Pfad für das Android SDK ist vorgesehen; API 35 wird erst nach persönlicher Lizenzannahme installiert. |
| Python | Python 3.14.2 verfügbar. |
| Import und Tests | Notion-Import und beide vorhandenen `unittest`-Tests laufen erfolgreich (131 Wesen, 12 Regel-Seiten; zwei leere Platzhalter übersprungen). |
| Web-Recherche | Webseitenabruf über `web_fetch` funktioniert; die offizielle Android-Entwicklerseite war erreichbar. Eine eigenständige Websuchschnittstelle ist in den verfügbaren Werkzeugen nicht sichtbar. |
| MCP | GitHub-MCP-Suche und -Lesezugriff sowie Pylance-MCP-Werkzeuge sind verfügbar. |
| Skills | Im Werkzeugkontext sichtbar: `add-python-version-support`, `address-pr-comments`, `agent-customization`, `analyze-prompt`, `create-pull-request`, `drop-python-version-support`, `fix-custom-instructions`, `git-branch`, `ssh-key`, `update-agent`, `write-tests` und weitere; die Auswahl ist kontextabhängig. |
| Pre-commit | In Phase 0 zunächst nicht installiert und kein Manifest vorhanden. In Phase 3 mit `requirements-dev.txt` eingerichtet, aktiviert und mit Hooks auf feste Versionen konfiguriert. |

## Externe Vergleichswerkzeuge (Vorschlag)

Die nachstehende Produktreferenz stammt aus einer vom Nutzer bereitgestellten Beschreibung der ArtCraft-Crafting-Apps. Sie dient ausschließlich als Vergleichs- und Strukturhinweis und ist derzeit kein Teil des Ruff-Produktumfangs, kein Kanon und keine implementierte App-Funktion.

| Produkt | Beschreibung | Status |
|---|---|---|
| PhotoCraft | Bildbearbeitung mit Ebenen, Masken, Typografie und echten PSD-Dateien. Teil der freien, eigenständigen Crafting-Apps-Suite. | Vergleichsreferenz; nicht in Ruff integriert. |
| VectorCraft | Vektorgrafik-Tool für Illustration. | Vergleichsreferenz; nicht in Ruff integriert. |
| FilmCraft | Video- und Schnittwerkzeug mit Farbe, Klang und visueller Bearbeitung. | Vergleichsreferenz; nicht in Ruff integriert. |
| LightCraft | Foto-Bibliothek und Raw-Entwicklung. | Vergleichsreferenz; nicht in Ruff integriert. |
| PdfCraft | Lesen, Ordnen und Schutz von PDFs. | Vergleichsreferenz; nicht in Ruff integriert. |
| EffectCraft | Motion Graphics und visuelle Effekte. | Vergleichsreferenz; nicht in Ruff integriert. |
| DesignCraft | Seitenlayout und Publishing. | Vergleichsreferenz; nicht in Ruff integriert. |

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
Der aktuelle 3D-Pipeline-Lauf bestätigt außerdem Python 3.14.2, Linux
x86_64, rund 15.6 GiB RAM und rund 13.4 GiB freien Speicher. CUDA/MPS,
PyTorch, SF3D-Code/Gewichte, Hugging-Face-Token und `gltf_validator` fehlen;
der öffentliche GitHub-API-Endpunkt ist erreichbar und das ignorierte
Modellausgabeverzeichnis beschreibbar. Es wurde keine Inferenz gestartet.

## Ausgangsdaten und offene Verfügbarkeit

- Der entpackte Export liegt bereits unter `.source-export/Privat und Geteilt/Ruff`; der Importbefehl aus der README funktioniert damit.
- Im Root wurden drei `abacktools-ExportBlock-…-part*-of3.zip`-Quellexportdateien gefunden und in den ignorierten `.source-export/archives/`-Bereich verschoben. Die vier Referenzarchive fehlten hier entsprechend und konnten nicht als Projekte übernommen werden.
- `.github/agents/ruff-worldbuilding.agent.md` war bereits vorhanden und wurde vor Änderungen gelesen.
- `markdownlint`, `actionlint`, `pre-commit` und die Android-SDK-Werkzeuge waren anfangs nicht verfügbar.
- Für `gh` ist keine Anmeldung eingerichtet. Erstellen oder Veröffentlichen eines Pull Requests kann daher eine Anmeldung erfordern.
