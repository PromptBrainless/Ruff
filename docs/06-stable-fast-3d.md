# Stable Fast 3D · Integrationsleitfaden

## Status und Grenzen

Die Pipeline verwaltet Zuordnung, Generierung, GLB-Prüfung und optionale
Notion-Verknüpfungen. Sie lädt keine Modellgewichte herunter, installiert
keine GPU-Abhängigkeiten, lädt keine GLB-Dateien hoch und verwendet keinen
Remote-Worker.

Der aktuelle Export enthält 131 eindeutige Wesen- und Notion-Seiten-IDs.
Fünf Referenzbilder sind eindeutig zuordenbar; 126 fehlen. In dieser Umgebung
gibt es keine CUDA-/MPS-GPU, keine Stable-Fast-3D-Installation oder Gewichte,
keinen Khronos-Validator, kein Hugging-Face-Token und kein Notion-Token.
Es wurden daher keine Modelle generiert oder Notion-Seiten geändert.

Ein vorhandener Dateiname oder ein JSON-Eintrag gilt nicht als Modell. Die
Pipeline meldet ein Modell nur nach erfolgreicher GLB-Prüfung als `validated`.
Vollständig ist ein Wesen erst nach erfolgreicher Notion-Synchronisierung
(`notion_synced`). Stable Fast 3D rekonstruiert aus einem Einzelbild; verdeckte
Geometrie ist nicht garantiert, Rigging wird nicht zugesichert.

## Voraussetzungen und Installation

- Python 3.10+ für Ruff-Werkzeuge; Stable Fast 3D selbst dokumentiert Python
  3.8+.
- Eine passende PyTorch-Installation für die konkrete CUDA-/GPU-Plattform.
  Die offiziellen Hinweise nennen rund 6 GB VRAM für eine Eingabe. CPU ist
  technisch unterstützt, aber für den Produktionsbatch nicht vorgesehen.
- Zugriff auf die freigeschalteten Hugging-Face-Gewichte und ausreichend
  freier Speicher.
- Der Khronos-`gltf_validator` auf `PATH`.
- Für Notion-Sync: autorisiertes dauerhaftes HTTPS-Asset-Hosting und
  Schreibzugriff auf die bereits exportierten Notion-Seiten.

Stable Fast 3D wird außerhalb des Ruff-Repositories in einem festgelegten
Code-Commit ausgecheckt; auch die Modellgewichte sind auf einen von Hugging
Face gemeldeten Modell-Commit fixiert:

```bash
git clone https://github.com/Stability-AI/stable-fast-3d.git \
  "$HOME/stable-fast-3d"
git -C "$HOME/stable-fast-3d" checkout \
  ff21fc491b4dc5314bf6734c7c0dabd86b5f5bb2
```

Nach Annahme der Modellbedingungen und Einrichtung eines sicheren
Hugging-Face-Zugangs können die Gewichte exakt in den lokalen, revisions-
benannten Ordner geladen werden:

```bash
hf download stabilityai/stable-fast-3d \
  --revision f0c9a8ffd62cb1bbc8a7a53c9f87a0be1b6be778 \
  --local-dir "$HOME/stable-fast-3d-weights/f0c9a8ffd62cb1bbc8a7a53c9f87a0be1b6be778"
```

Setze danach `SF3D_PRETRAINED_MODEL` auf genau diesen lokalen Ordner. Der
Worker verwendet nur einen Snapshot, dessen Verzeichnisname der festgelegten
Revision entspricht und der `config.yaml` sowie `model.safetensors` enthält.
Eine beliebige `main`-Version aus dem Hugging-Face-Cache wird nicht
akzeptiert. Das Gewicht wird dadurch vor der Inferenz lokal bereitgestellt;
die Inferenz selbst löst keine veränderliche Remote-Revision auf.

Lege dafür eine separate Python-Umgebung an, installiere PyTorch passend zur
GPU über die offizielle PyTorch-Anleitung und folge danach der am Commit
fixierten Stable-Fast-3D-README: `pip install -U setuptools==69.5.1`,
`pip install wheel` und `pip install -r requirements.txt`. Die Installation
der CUDA-Abhängigkeiten und Gewichte wird nicht durch Ruff automatisiert.

Die Model-Card nennt die Stability-AI-Community-Lizenz mit Bedingungen,
darunter eine Enterprise-Lizenzpflicht für Organisationen oder Personen mit
mehr als 1 Mio. US-Dollar Jahresumsatz für kommerzielle Nutzung. Prüfe vor
einer produktiven Nutzung die aktuelle Model-Card, Lizenz und Acceptable Use
Policy für den konkreten Einsatz. Der Code-Repository-Endpunkt meldet
`NOASSERTION`; Code- und Modelllizenzen sind daher getrennt zu prüfen. Die
Modellrevision `f0c9a8ffd62cb1bbc8a7a53c9f87a0be1b6be778` wurde am
28.10.2026 über die Hugging-Face-API als Modellstand gemeldet; sie bestätigt
nicht die Nutzungsberechtigung. Ruff
markiert `license_reviewed` standardmäßig `false` und blockiert die
Generierung, bis eine verantwortliche Person die Prüfung dokumentiert und
die Konfiguration bewusst freigibt.

## Konfiguration und sichere Zugangsdaten

Kopiere keine Tokens in die Konfiguration, Logs oder das Repository. Richte
`HF_TOKEN` oder `HUGGINGFACE_HUB_TOKEN` und für Notion `NOTION_TOKEN` über den
Codespace-Secrets-Mechanismus oder eine gleichwertige sichere Umgebung ein.
Die Pipeline gibt diese Werte nicht aus und entfernt Geheimnisse aus der
Umgebung des SF3D-Unterprozesses. Hugging-Face-Zugriff wird nur für den
separaten Gewichtsdownload benötigt; die Inferenz verwendet den lokalen,
fixierten Snapshot.

Für lokale Inferenz setze die Umgebungsvariablen `SF3D_REPO_DIR` und
`SF3D_PYTHON`. Das Ruff-Manifest und Modellverzeichnis liegen standardmäßig
unter `tools/ruff_3d/manifest.json` und `.artifacts/3d/models/`; letzteres ist
ignoriert und soll nicht eingecheckt werden.

In `tools/ruff_3d/config.json` bleiben `license_reviewed`,
`asset_hosting_authorized` und `asset_base_url` standardmäßig nicht
freigegeben. Ändere `license_reviewed` erst nach der Lizenzprüfung. Aktiviere
`asset_hosting_authorized` erst, wenn der konkrete Speicherort genehmigt und
für die vorgesehenen Notion-Leser zugänglich ist. Die Pipeline lädt Dateien
nicht hoch: `asset_base_url` muss auf ein bereits bereitgestelltes,
autorisierte HTTPS-Asset-Hosting zeigen. Vor einem Notion-Schreibzugriff prüft
die Pipeline die tatsächliche Erreichbarkeit des GLB-Links ohne Zugangsdaten.
Private Inhalte dürfen nicht ohne Zustimmung öffentlich gemacht werden.

`NOTION_DATABASE_ID` ist nicht erforderlich: synchronisiert werden
ausschließlich die anhand ihrer Export-IDs vorhandenen Seiten. Die Integration
prüft die Seite und hängt einen idempotent markierten Modellabschnitt an; sie
ändert keine bestehenden Eigenschaften oder Texte.

## Prüfen, einzelnes Wesen, Batch

Zuerst den lokalen Datenbestand und die Laufzeit prüfen:

```bash
python tools/import_export.py ".source-export/Privat und Geteilt/Ruff"
python -m tools.ruff_3d.cli init-manifest --expected-count 131
python -m tools.ruff_3d.cli validate-manifest --expected-count 131
python -m tools.ruff_3d.cli preflight
```

`preflight` meldet Python-Version, Betriebssystem, Architektur, RAM, GPU-Typ
und VRAM (falls verfügbar), freien Speicher, Schreibrechte, Netzwerk,
Gewichtsrevision und Hugging-Face-Token-Präsenz aus Umgebungsvariablen,
SF3D-Code-Commit und Validator. Ein lokal gespeicherter Hugging-Face-Login
wird nicht als Umgebungsvariable ausgewertet.
Es gibt keine Tokenwerte aus. Netzwerkzugriff wird nur zum öffentlichen
GitHub-Endpunkt geprüft; Notion- und Modell-Asset-Zugriff werden erst bei
ihren jeweiligen Vorgängen geprüft.

Ein Einzelmodell wird über die tatsächliche exportierte `creature_id`
gestartet:

```bash
python -m tools.ruff_3d.cli generate --id <exportierte-32-stellige-id>
```

Die Implementierung ruft die am festen Commit dokumentierte Schnittstelle
`python run.py <bild> --output-dir <ordner> --texture-resolution 1024
--remesh_option none --target_vertex_count -1 --batch_size 1` auf. Für ein
einzelnes Bild wird `0/mesh.glb` erwartet. Die Datei wird nur nach interner
GLB-Prüfung und erfolgreichem Khronos-Validator unter dem stabilen Wesen-ID-
Pfad abgelegt.

Erst nach einem erfolgreich validierten Pilotmodell den Batch ausführen.
Standardparallelität ist eins; der Batch speichert Status nach jedem Schritt
und setzt unterbrochene Vorgänge fort:

```bash
python -m tools.ruff_3d.cli batch
python -m tools.ruff_3d.cli status
```

Fehlgeschlagene Einträge werden nur mit ausdrücklicher Anweisung zurückgesetzt:

```bash
python -m tools.ruff_3d.cli batch --retry-failed
```

Ein validiertes Modell wird nicht erneut generiert, wenn Manifeststatus,
Dateipfad und Hash übereinstimmen. `--allow-cpu` ist eine explizite Ausnahme
für Tests, keine Empfehlung für den Produktionslauf. Ohne passende GPU
bleiben Einträge als `blocked_no_gpu` offen. Es ist kein autorisierter
separater GPU-Worker eingerichtet.

## GLB-Prüfung und Notion-Sync

Die interne Prüfung kontrolliert GLB-Header, eingebettete Daten, Meshes,
Primitive, Positions-Accessor, Materialien und Texturen. Zusätzlich ist der
externe Khronos-Validator standardmäßig Pflicht:

```bash
python -m tools.ruff_3d.cli validate-glb \
  .artifacts/3d/models/<creature_id>/model.glb
```

Die Notion-Synchronisierung ist erst nach erfolgreicher Modellvalidierung,
expliziter Asset-Hosting-Freigabe, erreichbarem HTTPS-Link und gesetztem
`NOTION_TOKEN` möglich:

```bash
python -m tools.ruff_3d.cli sync-notion --id <exportierte-32-stellige-id>
```

Ein erfolgreicher Sync ergänzt die bestehende Seite um einen markierten
Modellabschnitt mit Downloadlink, Validierungsstatus, Modellversion,
Generierungsdatum und Referenzbild-Dateiname. Ein vorhandener Marker verhindert
Duplikate. Fehlende Berechtigungen, nicht erreichbare Links und API-Fehler
bleiben als Blockade oder Fehler im Manifest erhalten. Es gibt keinen
automatischen Upload und keinen Ersatzlink. Sichere Lesezugriffe beachten
`Retry-After`; mehrdeutige Schreibfehler werden nicht automatisch wiederholt,
weil Notion einen Schreibvorgang trotz Fehlerantwort bereits gespeichert haben
kann. Ein erneuter manueller Sync prüft zuerst den Idempotenzmarker.

## Kosten, Fehlerbehebung und Ergebnisstatus

Es werden keine kostenpflichtigen GPUs, APIs oder Cloud-Worker gestartet.
Hugging-Face-Zugriff, Speicherplatz und GPU-Betrieb können eigenen
Lizenzbedingungen oder Kosten unterliegen; kläre sie vorab. Das Modellgewicht
und generierte GLBs werden nicht versioniert.

- `blocked_no_gpu`: CUDA-/MPS-GPU aktivieren oder einen autorisierten Worker
  einrichten; kein Modell wurde generiert.
- fehlende Gewichte: Hugging-Face-Zugang beantragen, Token sicher setzen und
  Gewichtedateien in der Preflight-Ausgabe prüfen.
- Lizenzblockade: verantwortliche Lizenzprüfung durchführen; keine
  Konfigurationsfreigabe durch den Agenten.
- fehlender `gltf_validator`: Khronos-Validator separat und vertrauenswürdig
  bereitstellen, dann Preflight und Modellprüfung wiederholen.
- `needs_reference_image`: ein passendes Bild mit nachprüfbarer Herkunft
  bereitstellen. Die Pipeline entwirft keine Ruff-Kreaturenbilder.
- Notion-Blockade: Seitenzugriff und Asset-URL-Berechtigung überprüfen; kein
  Page-Update wird bei fehlender Freigabe vorgenommen.

Automatisierte CPU-unabhängige Tests:

```bash
python -m unittest tools.ruff_3d.test_pipeline -v
python -m tools.ruff_3d.cli validate-manifest --expected-count 131
```

Ein bestandener Testlauf belegt nicht, dass Modellgewichte verfügbar sind
oder eine Inferenz stattgefunden hat. Verlässliche Live-Zahlen stehen im
Manifest und unter `status`; fehlende Modelle dürfen nicht als fertig gezählt
werden.
