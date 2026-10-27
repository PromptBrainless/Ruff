import tempfile
import unittest
from pathlib import Path

from import_export import parse_creature, parse_page


class ImportExportTests(unittest.TestCase):
    def test_parse_page_extracts_title_fields_and_content_blocks(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            page = root / "Wesen.html"
            page.write_text(
                """
                <h1>Wesen &amp; Held</h1>
                <table><tr><th>Machtstufe</th><td>IV – Monströs (Vorschlag)</td></tr></table>
                <h2>Ursprung</h2><p>Eine alte <strong>Sage</strong>.</p>
                """,
                encoding="utf-8",
            )

            parsed = parse_page(page, page.relative_to(root))

        self.assertEqual(parsed["title"], "Wesen & Held")
        self.assertEqual(parsed["fields"]["Machtstufe"], "IV – Monströs (Vorschlag)")
        self.assertEqual(
            parsed["blocks"],
            [
                {"type": "row", "text": "Machtstufe · IV – Monströs (Vorschlag)"},
                {"type": "h2", "text": "Ursprung"},
                {"type": "p", "text": "Eine alte Sage."},
            ],
        )

    def test_parse_creature_reads_numeric_power_level(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            page = root / "Anansi.html"
            page.write_text(
                """
                <h1>Anansi</h1>
                <table>
                  <tr><th>Machtstufe</th><td>IV – Monströs (Vorschlag Bot)</td></tr>
                  <tr><th>Archetyp</th><td>Trickster</td></tr>
                  <tr><th>Ursprung</th><td>Spinnen-Trickster.</td></tr>
                </table>
                """,
                encoding="utf-8",
            )

            creature = parse_creature(page, root)

        self.assertEqual(creature["powerLevel"], 4)
        self.assertEqual(creature["archetype"], "Trickster")
        self.assertEqual(creature["description"], "Spinnen-Trickster.")


if __name__ == "__main__":
    unittest.main()
