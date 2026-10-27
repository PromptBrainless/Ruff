#!/usr/bin/env python3
"""Convert the exported Ruff Notion pages into bundled Android app data."""

import argparse
import json
import re
import shutil
from html.parser import HTMLParser
from pathlib import Path


STAGE_NUMBERS = {
    "I": 1,
    "II": 2,
    "III": 3,
    "IV": 4,
    "V": 5,
    "VI": 6,
    "VII": 7,
    "VIII": 8,
    "IX": 9,
    "X": 10,
}

ASSETS = {
    "Bilder & Design/Universum-Design (Theme)/glyph-symbol.png": "ruff_glyph.png",
    "Bilder & Design/Universum-Design (Theme)/article-background-cap.png": (
        "ruff_article_background.png"
    ),
    "Bilder & Design/Universum-Design (Theme)/default-npc-image.webp": (
        "ruff_default_npc.webp"
    ),
}


def clean_text(parts):
    return re.sub(r"\s+", " ", "".join(parts)).strip()


class ExportPageParser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.title = ""
        self.fields = {}
        self.blocks = []
        self.current_block = None
        self.current_row = None
        self.current_cell = None
        self.skip_depth = 0

    def handle_starttag(self, tag, attrs):
        if tag in {"script", "style"}:
            self.skip_depth += 1
            return
        if self.skip_depth:
            return
        if tag == "br":
            if self.current_cell is not None:
                self.current_cell.append(" ")
            elif self.current_block is not None:
                self.current_block[1].append(" ")
        elif tag == "tr":
            self.current_row = []
        elif tag in {"td", "th"} and self.current_row is not None:
            self.current_cell = []
        elif (
            self.current_row is None
            and self.current_block is None
            and (tag in {"p", "li"} or re.fullmatch(r"h[1-6]", tag))
        ):
            self.current_block = [tag, []]

    def handle_endtag(self, tag):
        if tag in {"script", "style"} and self.skip_depth:
            self.skip_depth -= 1
            return
        if self.skip_depth:
            return
        if tag in {"td", "th"} and self.current_cell is not None:
            self.current_row.append((tag, clean_text(self.current_cell)))
            self.current_cell = None
        elif tag == "tr" and self.current_row is not None:
            cells = self.current_row
            if len(cells) >= 2 and cells[0][0] == "th" and cells[1][0] == "td":
                key, value = cells[0][1], cells[1][1]
                if key and value:
                    self.fields[key] = value
            row_text = " · ".join(value for _, value in cells if value)
            if row_text:
                self.blocks.append(("row", row_text))
            self.current_row = None
        elif self.current_block is not None and tag == self.current_block[0]:
            block_tag, parts = self.current_block
            value = clean_text(parts)
            if value:
                self.blocks.append((block_tag, value))
                if block_tag == "h1" and not self.title:
                    self.title = value
            self.current_block = None

    def handle_data(self, data):
        if self.skip_depth:
            return
        if self.current_cell is not None:
            self.current_cell.append(data)
        elif self.current_block is not None:
            self.current_block[1].append(data)


def parse_page(path, relative_path):
    parser = ExportPageParser()
    parser.feed(path.read_text(encoding="utf-8", errors="replace"))
    title = parser.title or re.sub(r"\s+[0-9a-f]{24,}.*$", "", path.stem)
    blocks = [
        {"type": block_type, "text": text}
        for block_type, text in parser.blocks
        if not (block_type == "h1" and text == title)
    ]
    return {
        "title": title,
        "source": relative_path.as_posix(),
        "fields": parser.fields,
        "blocks": blocks,
    }


def parse_creature(path, root):
    record = parse_page(path, path.relative_to(root))
    stage = record["fields"].get("Machtstufe", "")
    match = re.match(r"\s*([IVX]+)\s*[–-]\s*(.+?)(?:\s*\(|$)", stage)
    stage_roman = match.group(1) if match else ""
    description = (
        record["fields"].get("Ursprung")
        or record["fields"].get("Aussehen")
        or record["fields"].get("Besonderheiten (Fähigkeiten, Schwächen)", "")
    )
    return {
        "name": record["title"],
        "source": record["source"],
        "powerLevel": STAGE_NUMBERS.get(stage_roman, 0),
        "powerLabel": stage,
        "description": description,
        "archetype": record["fields"].get("Archetyp", ""),
        "fields": record["fields"],
    }


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def main():
    argument_parser = argparse.ArgumentParser()
    argument_parser.add_argument("export_root", type=Path)
    argument_parser.add_argument(
        "--output",
        type=Path,
        default=Path("app/src/main/assets"),
    )
    args = argument_parser.parse_args()

    root = args.export_root.resolve()
    creature_root = root / "Wesen & Figuren" / "Wesen-Register"
    rules_root = root / "Regeln"
    if not creature_root.is_dir() or not rules_root.is_dir():
        raise SystemExit(
            "Expected an extracted Ruff export containing 'Wesen & Figuren' and 'Regeln'."
        )

    parsed_creatures = [
        parse_creature(path, root)
        for path in sorted(creature_root.glob("*.html"))
    ]
    creatures = [
        creature
        for creature in parsed_creatures
        if creature["fields"] or creature["description"]
    ]
    skipped_creatures = len(parsed_creatures) - len(creatures)
    rules = [
        parse_page(path, path.relative_to(root))
        for path in sorted(rules_root.rglob("*.html"))
    ]
    if not creatures or not rules:
        raise SystemExit("The export contains no creature records or rule pages.")

    output = args.output.resolve()
    write_json(output / "creatures.json", creatures)
    write_json(output / "rules.json", rules)

    drawable_dir = output.parent / "res" / "drawable-nodpi"
    drawable_dir.mkdir(parents=True, exist_ok=True)
    for source_relative, target_name in ASSETS.items():
        source = root / source_relative
        if not source.is_file():
            raise SystemExit(f"Required design asset not found: {source}")
        shutil.copyfile(source, drawable_dir / target_name)

    levels = sorted({item["powerLevel"] for item in creatures if item["powerLevel"]})
    print(
        f"Imported {len(creatures)} creatures and {len(rules)} rule pages; "
        f"power levels: {', '.join(map(str, levels))}; "
        f"skipped {skipped_creatures} empty creature placeholders."
    )


if __name__ == "__main__":
    main()
