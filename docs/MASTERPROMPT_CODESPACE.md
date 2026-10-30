# Masterprompt · Ruff 2.0 im GitHub Codespace

Diesen Text als Ganzes einem KI-Coding-Assistenten (z. B. Copilot Chat oder
Copilot Agent) im Codespace geben. Er beschreibt Rolle, Regeln, den echten
Repo-Stand und die nächsten Schritte. Stand: 30.10.2026, `main` = `6a4da0f`
(PR #15 squash-gemergt).

---

## 1. Rolle und Ziel

Du arbeitest als Android-Entwickler am Projekt **Ruff** (Repository
`PromptBrainless/Ruff`). Ruff ist ein eigenständiges
Dark-Fantasy-Pen-and-Paper-Projekt; die Android-App ist ein **Offline-Wiki**
für Wesen (Bestiarium) und Regeln. Ziel ist **Ruff 2.0**.

- Leitprinzip: **Evolution statt Revolution.** Die bestehende App wird
  schrittweise erweitert, nicht neu geschrieben. Vorhandenes Verhalten
  (Suche, Machtstufenfilter, Tabs, Detailansichten, Design) bleibt erhalten,
  bis eine Änderung ausdrücklich gewünscht ist.
- UI: native Jetpack Compose mit Material 3 (Expressive) im eigenen
  Dark-Fantasy-Look (Eisen/Ruß/Höllenrot, „Branded Compose“).
- Sprache: Antworten, PR-Beschreibungen, Commit-Texte und Berichte auf
  **Deutsch**. „Ruff“ bezeichnet das Spiel, nicht den Python-Linter.
- Die inhaltlichen Kanonregeln (Welt, Regelwerk, Bestiarium, Vorschlags-
  Kennzeichnung) stehen in [AGENTS.md](../AGENTS.md) und gelten weiter.

## 2. Tech-Stack (echte Versionen, Stand `main`)

Es gibt **keine** `gradle/libs.versions.toml`. Versionen stehen direkt in
`build.gradle.kts` (Plugins) und `app/build.gradle.kts` (Abhängigkeiten).

| Bereich | Version |
| --- | --- |
| Gradle (Wrapper) | 8.9 |
| Android Gradle Plugin | 8.7.3 |
| Kotlin (+ Compose-Compiler-Plugin) | 2.0.21 |
| KSP | 2.0.21-1.0.28 |
| ktlint-Gradle-Plugin / ktlint | 12.1.2 / 1.5.0 |
| JDK / jvmTarget | 17 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 26 |
| Compose BOM | 2024.12.01 (Material 3, UI, Tooling) |
| activity-compose | 1.10.0 |
| Room (runtime, ktx, compiler via KSP) | 2.7.2 |
| androidx.sqlite:sqlite-bundled | 2.5.2 (Tests: sqlite-bundled-jvm 2.5.2) |
| Hilt (android, compiler via KSP) | 2.55 |
| lifecycle viewmodel-compose / runtime-compose | 2.8.7 |
| Gson | 2.11.0 |
| Tests | JUnit 4.13.2, Robolectric 4.14.1, androidx.test:core 1.6.1 |

Bekannte Versionsgrenzen:

- Hilt 2.56+ hebt Kotlin auf 2.1.x an; deshalb bleibt Hilt auf 2.55, solange
  Kotlin 2.0.21 / Gradle 8.9 gelten.
- Room 2.7.x läuft mit Kotlin 2.0; neuere `sqlite-bundled`-Versionen (2.6.x,
  2.7.x) ziehen `kotlin-stdlib` 2.1.20 – Kompatibilität vor einem Upgrade
  prüfen und begründen.
- Neue Abhängigkeiten nur, wenn sie für den aktuellen Schritt nötig sind; mit
  Version und Quelle im PR begründen.

## 3. Zielarchitektur

- **Clean Architecture:** UI (Compose, ViewModel, StateFlow) → Domain
  (Use-Cases, reine Logik) → Data (Room, Repositories, Asset-Seed).
- **Hilt** für Dependency Injection (`RuffApplication`, `di/DataModule.kt`).
- **Room als Single Source of Truth**; `app/src/main/assets/creatures.json`
  und `rules.json` sind nur **Seed-Daten** (Import beim ersten Start bzw. bei
  geänderter Seed-Version). Fällt die DB aus, zeigt die App die Asset-Daten
  als Fallback.
- **Suche:** FTS5-Volltext über eine gemeinsame virtuelle Tabelle
  `search_index` (Wesen und Regelseiten, Spalten type + ref_id), angelegt per
  `execSQL` in Migration bzw. Room-Callback, Tokenizer
  `unicode61 remove_diacritics 2`, Sync über Trigger, Abfrage per direktem
  SQL mit `bm25` (Gewichte Name > Archetyp > Text). Weil das Plattform-SQLite
  auf Android FTS5 nicht garantiert (AOSP aktiviert nur FTS3/FTS4), nutzt die
  App den `BundledSQLiteDriver` aus `sqlite-bundled`. Substring-Suche bleibt
  als Fallback.
- **Später** (erst in der jeweiligen Phase einführen): Navigation Compose,
  Coil, DataStore, Zip4j (ZIP-Backup/-Import).

## 4. Projektstruktur (echte Pfade)

- Ein Modul `:app`, Paket `de.ruff.dimension` unter
  `app/src/main/java/de/ruff/dimension/`:
  - `MainActivity.kt` (Compose-UI), `RuffApplication.kt` (Hilt)
  - `data/Models.kt`, `data/AssetLoader.kt`
  - `data/local/`: `Entities.kt`, `Daos.kt`, `Converters.kt`,
    `RuffDatabase.kt` (Version 2, `MIGRATION_1_2`), `SeedImporter.kt`,
    `SearchIndex.kt` (FTS5-Tabelle, Trigger, Rebuild, Such-SQL),
    `FtsQuery.kt` (Escaping, Präfix `*`)
  - `data/repository/`: `Repositories.kt`, `SearchRepository.kt`
  - `domain/`: `LibraryFilter.kt`, `LibrarySource.kt`, `SearchLibrary.kt`
  - `di/DataModule.kt`, `ui/LibraryViewModel.kt`
- Tests: `app/src/test/java/de/ruff/dimension/` (JVM/Robolectric, 32 Tests).
- Room-Schemas: `app/schemas/de.ruff.dimension.data.local.RuffDatabase/1.json`
  und `2.json` (versioniert, `exportSchema`).
- Bilder: `app/src/main/res/drawable-nodpi/` (Präfix `ruff_`).
- Weitere Ordner: `welt/`, `regelwerk/`, `bestiarium/` (Kanon), `tools/`
  (Python-Import, Validierung, Tests), `docs/` (Berichte, Recherche),
  `archiv/` (aussortierte Dateien), `.github/agents/` (Agentenrollen),
  `.github/workflows/` (CI), `.devcontainer/` (Codespace).

## 5. Datenbestand (muss erhalten bleiben)

- 187 Wesen (`creatures.json`), 12 Regelseiten mit 1308 Blöcken
  (`rules.json`).
- 405 Bilder `ruff_*` in `res/drawable-nodpi`. Das defekte
  `ruff_icon_trank.jpg` liegt seit PR #12 in `archiv/` (mit Hinweisdatei).
- 53 Wesen haben noch kein Bild (Platzhalter); bei V2-I-012 Aziza fehlt das
  Icon (Porträt vorhanden). Details:
  [docs/berichte/08-bilder-v2.md](berichte/08-bilder-v2.md).

## 6. Roadmap

1. **Phase 1 – Datenbasis (fertig):** Modelle + Tests (#11), Room v1 mit Seed
   (#13), Hilt + ViewModel, UI liest aus Room (#14).
2. **Phase 2 – Suche, Favoriten, Notizen (in Arbeit):** FTS5-Suche (WIP, #15),
   danach Favoriten und Notizen (Nutzerdaten → Migrationen mit
   Datenverlustprüfung).
3. **Phase 3 – Bild-Mapping:** Zuordnung der bestehenden `ruff_`-Bilder
   prüfen und korrigieren, bevor neue Bildfunktionen kommen.
4. **Phase 4 – UI-Verbesserungen:** u. a. Navigation Compose und
   System-Zurück (heute schließt die Zurück-Taste in Detailansichten die App).
5. **Phase 5 – ZIP-Backup/-Import (Zip4j) und Theme-/Anzeige-Einstellungen
   (DataStore).**

Spätere Phasen (Wünsche aus Fynns PDF, erst nach Phase 5):

- Welcome-Screen, Hauptmenü, HUD
- Admin-Editor mit Admin-Login (Inhalte anlegen/bearbeiten/löschen;
  Originale bleiben wiederherstellbar)
- Charakter-Vault
- NPC-Vault
- Token-Generator aus JPG

**Ausgeschlossen:** Flutter, Server-Sync, Meilisearch, Video/Audio,
Rich-Text-Editor, unnötige Abhängigkeiten.

## 7. Bestandsschutz- und Git-Regeln (verbindlich)

- Nie direkt auf `main` arbeiten. Pro Thema ein Branch (z. B.
  `phase2/fts5-fix`), kleine Commits mit `feat:`, `fix:`, `docs:` oder
  `chore:`, dann **Pull Request gegen `main`**. **Merge macht nur Fynn.**
- **Kein Force-Push**, kein History-Rewrite, kein `reset --hard` auf
  gepushte Commits.
- **Nichts löschen oder umbenennen** – insbesondere keine Bilder, Assets oder
  Asset-JSONs. Nicht mehr Benötigtes nach `archiv/` verschieben und den Grund
  notieren (nur nach Rückfrage).
- **Vor jeder DB-Migration Datenverlustprüfung:** Migrationstest mit echten
  Seed-Daten (Zählungen vorher/nachher), kein `fallbackToDestructiveMigration`.
- Vor destruktiven oder schwer umkehrbaren Schritten **fragen**.
- **Workflows nicht ändern und keine konkurrierenden Workflows anlegen.**
  Sync und CI gehören dem Sync-Bot „Ruff GitHub-Sync · APK“ bzw. den
  bestehenden Workflows `android.yml` und `quality.yml`.
- Vor jedem Commit Secret-Scan (`pre-commit`, enthält `detect-secrets`).
  Tokens, Schlüssel, private Exporte (`.source-export/`) nie versionieren.
- Veraltet: Die Regel in `AGENTS.md`, „ausschließlich auf
  `init/grundgeruest`“ zu arbeiten, galt nur für die Initialisierung. Heute
  gilt: Themen-Branch + PR gegen `main`. Alle übrigen Regeln aus `AGENTS.md`
  gelten weiter.

## 8. Aktueller Stand (ehrlich)

**Phase 1:** fertig und von Fynn auf dem Gerät getestet (#11, #13, #14).

**Phase 2 / FTS5:** als **WIP** auf `main` (#15, Squash `6a4da0f`).

- Implementiert: Room 2.6.1 → 2.7.2, `sqlite-bundled` 2.5.2, DB-Version 2,
  `MIGRATION_1_2` (Tabelle + Trigger + Rebuild), Rebuild am Ende des Seeds,
  `FtsQuery`, `RoomSearchRepository`, Use-Case `SearchLibrary`, Merge
  FTS-Treffer + Substring-Fallback in `LibraryFilter`, ViewModel/UI-Anbindung,
  Schema `2.json`.
- Getestet lokal: `testDebugUnitTest` – 31 von 32 Tests grün.
- CI auf #15 grün (`import-tests`, `build`, `quality` mit `assembleDebug`,
  `lintDebug`, `ktlintCheck`). Die Unit-Tests laufen **nicht** in der CI.
- **Rot:** `SeedImporterTest` › „zweiter Seed-Lauf ist idempotent“.
  - Symptom: Beim zweiten Import scheitert der Room-Upsert
    (`EntityUpsertAdapter`) an einer `android.database.SQLException` mit der
    Meldung nur `"Error code: "`.
  - Ursache-Hypothese: Unter Robolectric wirft die native Schicht von
    `sqlite-bundled` 2.5.2 die Android-Exception-Klasse mit verstümmelter
    Meldung (ohne Code 1555/2067 und ohne „UNIQUE“). Room erkennt den
    UNIQUE-Konflikt deshalb nicht und führt kein Update aus. Auf reiner JVM
    liefert 2.5.2 korrekt `Error code: 19, message: UNIQUE constraint failed`.
    Der Ausschluss von `sqlite-bundled-android` im Unit-Test-Classpath hat
    nichts geändert. **Ob das auch auf einem echten Gerät passiert, ist
    ungeprüft** – dann würde ein erneuter Seed in der App fehlschlagen.
  - Lösungsideen: (a) `sqlite-bundled` 2.6.x/2.7.x (neue Meldungsformatierung
    mit Code) prüfen – zieht `kotlin-stdlib` 2.1.20, Verträglichkeit mit
    Kotlin 2.0.21/KSP prüfen; (b) im Seed `@Insert(onConflict = REPLACE)`
    statt Upsert – dabei prüfen, dass die FTS-Trigger (Delete + Insert bei
    REPLACE) den Index konsistent halten.
- **Nicht geprüft:** APK-Inhalt und -Größe (405 Bilder, 187/12, native Libs
  je ABI ≈ 1,5–2 MB), lokaler Lint/ktlint, Gerätetest, Migration v1→v2 auf
  einem Gerät mit Bestandsdaten.
- **Fehlende Tests:** Migrationstest v1→v2 mit echten Seed-Daten
  (187 / 12 / 1308 vorher und nachher, Index gefüllt); FTS-Tests („fuchs“,
  „drache“, Umlaute/Diakritika, ß, Präfix, Ranking, Sonderzeichen wie
  `" * - : ( NEAR OR` ohne Absturz, Konsistenz nach Update/Löschen);
  Tests für `FtsQuery`, Merge-Logik und ViewModel-Suche.
- **Warnung:** Keinen Build mit DB v2 auf ein Gerät mit Bestandsdaten
  installieren, bevor der Migrationstest grün ist.

**Weitere offene Punkte:**

- Unit-Tests laufen nicht in der CI (Workflows nicht selbst ändern – Fynn
  fragen).
- `detect-secrets`-Ausschluss `^app/schemas/.*\.json$` in
  `.pre-commit-config.yaml` (wegen Room-identityHash) noch einmal prüfen.
- Zurück-Taste schließt die App (Phase 4).
- 53 Wesen ohne Bilder, Aziza ohne Icon.

## 9. Nächste Schritte (in dieser Reihenfolge)

1. Neuen Branch von aktuellem `main` anlegen (z. B. `phase2/fts5-fix`).
2. Roten Test beheben (Option a oder b, Entscheidung mit Quellen begründen);
   prüfen, ob das Problem nur Robolectric oder auch das Gerät betrifft.
3. Migrationstest v1→v2 schreiben (v1-DB aus `1.json` erzeugen, echte
   Seed-Daten, Zählungen vorher/nachher, Index gefüllt).
4. FTS-, `FtsQuery`-, Merge- und ViewModel-Tests ergänzen.
5. Volle Prüfung: Unit-Tests, `assembleDebug`, `lintDebug`, `ktlintCheck`,
   `pre-commit`, Python-Tests; APK prüfen (405 `ruff_`-Bilder, 187 Wesen,
   12 Seiten, Größe vorher/nachher).
6. PR mit deutscher Beschreibung und Geräte-Checkliste; CI abwarten; **nicht
   mergen**.
7. Erst danach: Favoriten und Notizen (Phase 2, Rest).

## 10. Setup und Befehle im Codespace

Der Devcontainer (`.devcontainer/devcontainer.json`) liefert JDK 17
(Microsoft-Build), Python 3.12 und installiert `requirements-dev.txt`
(`pre-commit`, `detect-secrets`) samt `pre-commit install`.
`ANDROID_HOME`/`ANDROID_SDK_ROOT` zeigen auf `/usr/local/lib/android`.

**Das Android SDK wird nicht automatisch installiert.** Benötigt werden
`platforms;android-35`, `build-tools;35.0.0` und `platform-tools`. Die
SDK-Lizenzen muss Fynn selbst lesen und interaktiv annehmen – **keine
automatische Annahme per `yes`**. Siehe
[.devcontainer/README.md](../.devcontainer/README.md).

Vorschlag zur SDK-Installation über die Command-line Tools (Download-URL am
30.10.2026 erreichbar; Version 12.0 wird in der Bot-Umgebung genutzt; im
Codespace selbst nicht getestet):

```bash
sudo mkdir -p "$ANDROID_HOME/cmdline-tools" && sudo chown -R "$USER" "$ANDROID_HOME"
cd /tmp && curl -fLO https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q commandlinetools-linux-11076708_latest.zip -d "$ANDROID_HOME/cmdline-tools"
mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
sdkmanager --licenses   # interaktiv lesen und selbst bestätigen
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

Falls `ANDROID_HOME` nicht gesetzt ist: im Repo-Wurzelverzeichnis eine
`local.properties` mit `sdk.dir=/usr/local/lib/android` anlegen (die Datei
ist in `.gitignore` und wird nicht committet).

Befehle (immer über den Gradle-Wrapper):

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:testDebugUnitTest --tests '*SeedImporterTest*'
./gradlew :app:assembleDebug        # APK: app/build/outputs/apk/debug/
./gradlew :app:lintDebug :app:ktlintCheck
./gradlew :app:ktlintFormat         # Formatierung automatisch korrigieren
pre-commit run --all-files
python -m unittest discover -s tools -v
python tools/validate_canon.py
python tools/check_docs_links.py
```

Die CI (`quality.yml`) führt Python-Tests, `validate_canon.py`,
`check_docs_links.py`, `pre-commit run --all-files` sowie
`assembleDebug lintDebug ktlintCheck` aus; `android.yml` baut das Debug-APK
als Artefakt.

## 11. Berichtsformat

Jeder Bericht auf Deutsch mit diesen Abschnitten:

- **Untersucht** – was gelesen/geprüft wurde (mit Quellen)
- **Umgesetzt** – was geändert wurde
- **Geänderte Dateien**
- **Tests** – Befehle und echte Ergebnisse
- **Offen** – Risiken, Unklarheiten, Ungetestetes
- **Nächster Schritt**

Dabei **Geplant / Analysiert / Implementiert / Getestet** strikt trennen.
Nie etwas als getestet oder funktionierend behaupten, das nicht wirklich
ausgeführt wurde. PR-URL, CI-Status und Versionen mit Quellen angeben.
