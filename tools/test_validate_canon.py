import json
import tempfile
import unittest
from pathlib import Path

from validate_canon import validate_records


def valid_record(entry_id="duscher"):
    return {
        "id": entry_id,
        "names": ["Düster"],
        "origin": "Ein Vorschlag.",
        "behavior": "Ein Vorschlag.",
        "appearance": "Ein Vorschlag.",
        "special_features": "Ein Vorschlag.",
        "core_values": {"koerperkraft": 100},
        "power_level_source": {"value": "ungeklärt", "source": "offen"},
        "status": "Vorschlag",
        "source": "Entwurf",
        "related_entries": [],
    }


class ValidateCanonTests(unittest.TestCase):
    def write_record(self, directory, filename, record):
        (directory / filename).write_text(
            json.dumps(record, ensure_ascii=False),
            encoding="utf-8",
        )

    def test_accepts_valid_record(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            data_dir = Path(temporary_directory)
            self.write_record(data_dir, "duscher.json", valid_record())

            errors = validate_records(data_dir)

        self.assertEqual(errors, [])

    def test_reports_duplicate_names_and_ids(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            data_dir = Path(temporary_directory)
            first = valid_record("first")
            second = valid_record("first")
            second["names"] = ["DÜSTER"]
            self.write_record(data_dir, "first.json", first)
            self.write_record(data_dir, "second.json", second)

            errors = validate_records(data_dir)

        self.assertTrue(any("duplicate id" in error for error in errors))
        self.assertTrue(any("duplicate name" in error for error in errors))

    def test_reports_missing_fields_invalid_values_and_orphan_references(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            data_dir = Path(temporary_directory)
            record = valid_record()
            record.pop("appearance")
            record["core_values"]["koerperkraft"] = 1001
            record["related_entries"] = ["missing"]
            self.write_record(data_dir, "entry.json", record)

            errors = validate_records(data_dir)

        self.assertTrue(any("missing required fields" in error for error in errors))
        self.assertTrue(any("0 to 1000" in error for error in errors))
        self.assertTrue(any("orphan related_entries" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
