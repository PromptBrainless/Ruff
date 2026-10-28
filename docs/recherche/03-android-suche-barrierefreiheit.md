# Recherche · Durchsuchbare Android-Regel-App

Prüfdatum: 27.10.2026. Alle Links führen zu offizieller Android-Dokumentation.
Es wurden keine Codebeispiele übernommen. Der vorhandene Ruff-MVP bleibt
offline und verwendet Ruff-eigene Exportdaten.

## Suchbare Inhalte

Die Android-Seite [Search](https://developer.android.com/develop/ui/views/search)
bezeichnet Suche als Kernfunktion und macht deutlich, dass das Suchframework
nicht selbst die Daten durchsucht: die App muss den jeweils passenden
Datenzugriff nutzen. Für umfangreiche lokale Daten beschreibt die Room-Doku
[Full-text search](https://developer.android.com/training/data-storage/room/defining-data#fts)
FTS-Tabellen mit FTS3/4/5.

Für Ruff folgt daraus:

- Regeltext, Titel, Wesenfelder und Filterwerte strukturiert getrennt halten;
- die derzeit überschaubaren Offline-Assets einfach durchsuchen;
- erst bei deutlich größerem Register lokale Volltextsuche und eine passende
  Datenbank prüfen, nicht vorsorglich eine neue Abhängigkeit einführen;
- Suchbegriffe lokal behandeln und weder an einen Server senden noch
  personenbezogene Suchverläufe protokollieren.

## Barrierefreiheit

Die Android-Übersicht [Accessibility](https://developer.android.com/guide/topics/ui/accessibility)
empfiehlt beschriftete UI-Elemente und zugängliche Nutzerflüsse. Die
Compose-Dokumentation zu
[Semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
erklärt, dass Rollen, Zustände und Beschreibungen für Accessibility-Dienste und
UI-Tests verfügbar sein müssen; Material- und Compose-Bausteine liefern dafür
bereits passende Standardsemantik.

Für Ruff folgt daraus: Suchfeld und Filter müssen verständlich beschriftet,
interaktive Elemente mit Screenreader nutzbar und bedeutungstragende Bilder
beschrieben sein. Rein dekorative Bilder sollen keine unnötigen Ansagen
erzeugen. Android Lint ist im CI aktiviert; ein erfolgreicher Lint-Lauf ersetzt
keinen manuellen TalkBack-Test oder eine Prüfung mit Accessibility Scanner.

## Lizenzstatus

Die geprüften Android-Seiten sind Dokumentation der Plattform. Auf den
abgerufenen Seiten wurde keine konkrete Inhaltslizenz verifiziert; deshalb
werden daraus weder Text noch Beispielcode übernommen. Für Android SDK
Komponenten gelten separate SDK-Lizenzbedingungen.
