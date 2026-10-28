# Phase 6 · Stable-Fast-3D-Pipeline

## Getan

- Eine wiederaufnehmbare CLI für Manifest, Preflight, Einzelgenerierung,
  sequenziellen Batch, GLB-Prüfung und Notion-Sync angelegt.
- 131 exportierte Wesen- und Seiten-IDs erfasst; fünf Bilder eindeutig
  zugeordnet, 126 Referenzbilder fehlen.
- Generierung an den geprüften Stable-Fast-3D-Commit gebunden. Lizenz- und
  Asset-Hosting-Freigaben sind standardmäßig deaktiviert. Die Gewichte sind
  auf die dokumentierte Hugging-Face-Revision fixiert und werden nur als
  lokaler Snapshot dieser Revision akzeptiert.
- Notion-Sync verwendet bestehende Seiten-IDs, verlangt einen erreichbaren
  HTTPS-Asset-Link und schreibt keine Seiten ohne Credential und Freigabe.
- Bedienung, Ressourcen, Lizenzhinweise und Blockaden in
  `docs/06-stable-fast-3d.md` beschrieben.

## Geprüft

- Gesamte Python-Testsammlung: 37 Tests bestanden, darunter Manifest,
  Wiederaufnahme, Retries, fehlende Gewichte, GLB-Prüfung,
  Notion-Rate-Limits und idempotente Behandlung unklarer Schreibfehler.
- Vollständiger Pre-Commit-Lauf bestanden: Secret-Scan, Markdown-Lint,
  Flake8, JSON-Schemata und Manifestprüfung.
- Dokumentationslinkprüfung, Kanonprüfung und 131er-Manifestprüfung
  bestanden; das Bestiarium enthält derzeit 0 Einträge.
- Kotlin-Format-/Lint-Task mit JDK 17 bestanden.
- Preflight: Python 3.14.2, Linux x86_64, rund 15.6 GiB RAM und 13.4 GiB
  freier Speicher; Ausgabeverzeichnis beschreibbar und GitHub-Netzwerkprobe
  HTTP 200. Keine GPU, PyTorch, SF3D-Installation, Gewichte, Hugging-Face-
  Token, Khronos-Validator oder Notion-Token.
- Android-Build mit JDK 17 versucht; er scheitert konkret an fehlendem
  Android-SDK (`ANDROID_HOME`/`sdk.dir`). Ein vorheriger Lauf mit JDK 25
  scheiterte am nicht kompatiblen Versionsparser im vorhandenen Kotlin-Plugin.
- Manifeststatus: 131 Wesen, davon 5 `queued`, 126 `needs_reference_image`,
  0 Modelldateien und 0 Notion-Synchronisierungen.
- Kein echter SF3D-Lauf, keine GLB-Datei und kein Notion-Update.

## Offen

- Lizenzprüfung und Freigabe durch eine verantwortliche Person.
- Verifizierte Referenzbilder für 126 Wesen.
- Autorisierte GPU-Umgebung und Hugging-Face-Gewichtszugriff.
- Installation/Verfügbarkeit des Khronos-Validators und erfolgreicher echter
  Pilotlauf.
- Dauerhafter Asset-Host, dessen HTTPS-Link für Notion-Leser erreichbar ist,
  sowie Notion-Schreibberechtigung.
- Android SDK API 35 gemäß lokaler Lizenzbedingungen bereitstellen, dann
  Android-Build und Android-Lint wiederholen.
- `gh auth login` ist in dieser Umgebung nicht erfolgt; der lokale Commit ist
  möglich, Push und PR-Veröffentlichung benötigen jedoch GitHub-Authentifizierung.
