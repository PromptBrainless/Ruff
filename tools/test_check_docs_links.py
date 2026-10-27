import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from check_docs_links import check_docs, extract_links


class CheckDocsLinksTests(unittest.TestCase):
    def test_extract_links_ignores_fenced_code(self):
        markdown = (
            "[local](README.md)\n"
            "```md\n"
            "[not a link](missing.md)\n"
            "```\n"
        )

        self.assertEqual(extract_links(markdown), ["README.md"])

    def test_check_docs_reports_missing_local_target(self):
        with TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            (root / "guide.md").write_text(
                "[missing](absent.md)\n",
                encoding="utf-8",
            )

            errors = check_docs(root, check_external=False)

        self.assertEqual(len(errors), 1)
        self.assertIn("missing local target", errors[0])


if __name__ == "__main__":
    unittest.main()
