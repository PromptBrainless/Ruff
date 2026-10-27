# Ruff · Arbeitsregeln für Agenten

## Kanon und Quellen

- Bestehende Notion-Inhalte und bestätigten Kanon nicht stillschweigend ändern.
- Neue Welt-, Regel- und Bestiariumsinhalte standardmäßig als **Vorschlag**
  markieren, bis ein Mensch sie bestätigt.
- Keine unbelegten Weltfakten erfinden. Prüfe zuerst die einschlägigen Dateien
  unter `welt/`, `regelwerk/` und `bestiarium/`; dokumentiere offene Konflikte.
- Keine Fremdinhalte übernehmen. Quellen und Lizenzen für verwendete externe
  Materialien gehören in `docs/quellen.md`.
- Geschützte Regelwerke und Welten dürfen nur als Strukturanregung dienen,
  niemals als Inhalt.

## Ruff-Vorgaben

- Es gibt kein Rangsystem für Kernwerte. Werte liegen zwischen 0 und 1000;
  der Durchschnittsmensch liegt bei 100.
- Attribute erhalten jeweils eine eigene Ankerleiter. Der Anker für Körperkraft
  ist ein unbewaffneter Schlag mit Körperkraft + 0; die Skala steigt davon
  schrittweise an. Keine weiteren Anker erfinden, bevor sie festgelegt sind.
- Machtstufen sind in diesem Datenmodell Quellenangaben, keine Ersatz-Skala
  für Kernwerte.
- Dasselbe Wesen unter verschiedenen Namen erhält für jeden Namen einen
  eigenen Haupteintrag mit eigener Beschreibung. Einträge nicht zusammenführen.
- Stimmungstexte benennen die beabsichtigte Stimmung klar (etwa Armut,
  Niedergang und Hoffnungslosigkeit), statt rätselhafte Einzelheiten als
  vermeintliche Hinweise einzustreuen. Maßgeblich ist `welt/stilguide.md`.
- Ruff bezeichnet hier das Spiel und seine Welt, nicht den gleichnamigen
  Python-Linter. Für Python wird ein anderes Lint-Werkzeug verwendet.

## Branch, Archiv und Freigabe

- Für diese Initialisierung ausschließlich auf `init/grundgeruest` arbeiten;
  nie direkt auf `main`. Änderungen zur Freigabe als Pull Request gegen `main`
  einreichen; Freigabe und Merge bleiben menschlichen Personen vorbehalten.
- Keine Force-Pushes, History-Rewrites oder endgültigen Löschungen. Nicht mehr
  benötigte Dateien nach `archiv/` verschieben und den Grund notieren.
- Commits klein, thematisch einzeln und mit `feat:`, `fix:`, `docs:` oder
  `chore:` kennzeichnen. Vor jedem Commit den Secret-Scan ausführen.
- Tokens, Schlüssel, lokale Exporte und private Daten niemals versionieren.

## Änderungen und Prüfung

- Änderungen eng am Auftrag halten; vorhandene App- und Importfunktionen
  erhalten.
- Vor einem Commit Geheimnisse prüfen. Tokens, Schlüssel und private Exporte
  gehören nicht ins Repository.
- Relevante Tests und Prüfungen ausführen und Ergebnisse ehrlich dokumentieren.
- Agentenrollen in `.github/agents/` haben eigene Freigabegrenzen; insbesondere
  Kanon-Hüter und Reviewer ändern nichts selbst.
