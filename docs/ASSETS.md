# Gestaltungs-Assets und Schriften

Stand: 30.10.2026, Branch `phase4/navigation-design`.

## Lizenz der Gestaltungselemente

Alle Gestaltungselemente mit dem Präfix `ruff_ui_` stammen von **Fynn Levin Hirt**
und werden von ihm **unter seinem Namen** für Ruff lizenziert (Freigabe im Auftrag
vom 30.10.2026: „Ich lizensiere mit meinem Namen“). Sie sind keine Fremdquellen.
Die Wesen-Bilder `ruff_*` (405 Dateien) bleiben unverändert und unbenannt; ihre
Herkunft steht in [generierte-assets.md](generierte-assets.md).

## Dateien in `app/src/main/res/drawable-nodpi/`

| Datei | Pixel | Bytes | Verwendung |
|---|---|---|---|
| `ruff_ui_bg_thronsaal.webp` | 1168 × 784 | 132 138 | Hintergrund Hauptmenü (unter Verlaufs-Scrim 55–85 % Schwarz) |
| `ruff_ui_frame_detail.webp` | 1168 × 784 | 150 782 | Titelrahmen im Hauptmenü (Fenster mit 82 % Schwarz hinterlegt) |
| `ruff_ui_button_klinge.webp` | 1089 × 331 | 76 448 | Klingen-Knöpfe „Wesen“, „Regelwerk“, „Suche“ (Textfeld mit Scrim) |
| `ruff_ui_bar_titel.webp` | 1095 × 226 | 76 764 | Titelleiste (3-Slice, Text auf 35 % Schwarz) und Abschnittsköpfe |
| `ruff_ui_divider_kette.webp` | 1129 × 445 | 66 970 | Ketten-Trenner |
| `ruff_ui_frame_portrait.webp` | 784 × 1021 | 132 090 | Porträtrahmen im Wesen-Detail |
| `ruff_ui_avatar_rahmen.webp` | 192 × 192 | 6 326 | Ring um Wesen-Icons in Listen (aus Fynns Notion-Theme „avatar-frame“) |
| **Summe Bilder** | | **641 518** | |

Aufbereitung: Die sechs großen Elemente lagen als JPG mit eingebranntem
Schachbrett-„Transparenz“-Muster vor. Das Muster wurde per Helligkeits-/
Sättigungsschwelle und Zusammenhangsanalyse (nur vom Rand bzw. von der Mitte
erreichbare Flächen) freigestellt und als WebP mit Alpha gespeichert (Farbe
verlustbehaftet, Alpha in voller Qualität). Der Thronsaal-Hintergrund hat kein Alpha.
Die Originale bleiben außerhalb des Repositorys erhalten.

Regel: Text liegt nie direkt auf unruhiger Grafik, sondern immer auf einem Scrim
(siehe `ui/common/Ornaments.kt`).

## Schriften (`app/src/main/res/font/`)

| Datei | Quelle | Bytes | Lizenz |
|---|---|---|---|
| `cinzel.ttf` | [google/fonts `ofl/cinzel/Cinzel[wght].ttf`](https://github.com/google/fonts/tree/main/ofl/cinzel), Commit `6157dfa` | 125 468 | SIL OFL 1.1, Text in `res/raw/ofl_cinzel.txt` |
| `alegreya.ttf` | [google/fonts `ofl/alegreya/Alegreya[wght].ttf`](https://github.com/google/fonts/tree/main/ofl/alegreya), Commit `6157dfa` | 425 288 | SIL OFL 1.1, Text in `res/raw/ofl_alegreya.txt` |

Die Dateien sind unverändert (nur umbenannt, weil Android-Ressourcennamen keine
Klammern erlauben). Die OFL-Texte liegen im Repository und werden in der App unter
„Lizenzen & Herkunft“ angezeigt. Cinzel: Überschriften, Titel, Knöpfe; Alegreya:
Fließtext und Beschriftungen.

Gesamt neu (Bilder, Schriften, Lizenztexte): **1 201 052 Bytes** (≈ 1,15 MiB,
Vorgabe < 2,5 MB).

## Farben

Dunkles Material-3-Schema `RuffDarkColors` (`ui/theme/Color.kt`). Alle Textrollen
erreichen mindestens WCAG AA 4,5:1, geprüft in `RuffColorsContrastTest`.
**Dynamic Color ist bewusst aus:** Ruff hat eine feste Dark-Fantasy-Identität
(Höllenrot, Glut, Pergament auf Ruß); Systemfarben aus dem Wallpaper würden diese
Identität und die geprüften Kontraste auf den Grafiken aufheben.
