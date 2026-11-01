# Adapted from https://github.com/bikram-agarwal/Remember/blob/main/font_subset/build_search_emojis.py

"""
Build app/src/main/res/raw/emojis.json from Unicode + CLDR upstream data.

Replaces the bundled JSON from `com.github.alexdametto:compose-emoji-picker`,
adding the CLDR `keywords` field that powers concept search
("spicy" -> chili, "lit" -> fire, "yoga" -> meditation, etc.).

Inputs (downloaded with stdlib urllib, cached under tools/emojis/cache/):
- emoji-test.txt  - canonical emoji list with group/subgroup categorization
- en.xml          - CLDR English annotations (tts label + keywords)

Output JSON shape:

    [
      {
        "key": "fire",
        "emoji": "🔥",
        "name": "fire",
        "slug": "fire",
        "category": "objects",
        "keywords": ["flame", "hot", "lit", "tool"]
      },
      ...
    ]

Run: `python tools/emojis/build_search_emojis.py` (no extra deps; stdlib only).
Pass `--force-refresh` to bypass the cache and re-download.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import unicodedata
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Iterable

# User-replaceable defaults (override with CLI flags below).
EMOJI_VERSION_DEFAULT = "16.0"
CLDR_BRANCH_DEFAULT = "release-47"
OUTPUT_PATH_DEFAULT = "app/src/main/res/raw/emojis.json"

# Internal constants (paths + URL templates).
REPO = Path(__file__).resolve().parents[2]
CACHE_DIR = Path(__file__).with_name("cache")

EMOJI_TEST_URL = "https://unicode.org/Public/emoji/{version}/emoji-test.txt"
CLDR_ANNOTATIONS_URL = (
    "https://raw.githubusercontent.com/unicode-org/cldr/{branch}"
    "/common/annotations/en.xml"
)
CLDR_DERIVED_URL = (
    "https://raw.githubusercontent.com/unicode-org/cldr/{branch}"
    "/common/annotationsDerived/en.xml"
)

# Map upstream Unicode group strings to the picker's category keys.
GROUP_TO_CATEGORY: dict[str, str] = {
    "Smileys & Emotion": "smileys_and_emotions",
    "People & Body": "people",
    "Animals & Nature": "animals_and_nature",
    "Food & Drink": "food_and_drink",
    "Travel & Places": "travel_and_places",
    "Activities": "activities_and_events",
    "Objects": "objects",
    "Symbols": "symbols",
    "Flags": "flags",
}

# Skip skin-tone modifiers and other componentry; they are not picker rows.
SKIPPED_GROUPS = {"Component"}

# Status values in emoji-test.txt: only "fully-qualified" entries are end-user
# selectable. "minimally-qualified" / "unqualified" / "component" are dropped.
ALLOWED_STATUS = {"fully-qualified"}

EMOJI_LINE = re.compile(
    r"^(?P<codepoints>[0-9A-F ]+)\s*;\s*"
    r"(?P<status>[a-z\-]+)\s*#\s*"
    r"(?P<emoji>\S+)\s+"
    r"E\d+\.\d+\s+"
    r"(?P<name>.+)$"
)

# Strip qualifying combiners that some platforms render but search shouldn't see.
VARIATION_SELECTOR_16 = "️"


def fetch(url: str, cache_path: Path, *, force_refresh: bool) -> str:
    if cache_path.exists() and not force_refresh:
        return cache_path.read_text(encoding="utf-8")
    cache_path.parent.mkdir(parents=True, exist_ok=True)
    print(f"  downloading {url}", file=sys.stderr)
    request = urllib.request.Request(url, headers={"User-Agent": "remember/build-emoji-data"})
    with urllib.request.urlopen(request, timeout=60) as response:
        text = response.read().decode("utf-8")
    cache_path.write_text(text, encoding="utf-8")
    return text


def parse_emoji_list(text: str) -> list[dict[str, str]]:
    """Walk emoji-test.txt and emit one entry per fully-qualified emoji."""
    rows: list[dict[str, str]] = []
    current_group = ""
    for line in text.splitlines():
        if line.startswith("# group:"):
            current_group = line[len("# group:") :].strip()
            continue
        if not line or line.startswith("#"):
            continue
        match = EMOJI_LINE.match(line)
        if not match:
            continue
        if current_group in SKIPPED_GROUPS:
            continue
        if match.group("status") not in ALLOWED_STATUS:
            continue
        category = GROUP_TO_CATEGORY.get(current_group)
        if category is None:
            continue
        rows.append(
            {
                "emoji": match.group("emoji"),
                "name": match.group("name").strip(),
                "category": category,
            }
        )
    return rows


def parse_cldr_annotations(text: str) -> dict[str, dict[str, str | list[str]]]:
    """Return {emoji_string: {"tts": "...", "keywords": [...]}} from one CLDR file."""
    root = ET.fromstring(text)
    result: dict[str, dict[str, str | list[str]]] = {}
    for annotation in root.iter("annotation"):
        cp = annotation.attrib.get("cp")
        if cp is None:
            continue
        bucket = result.setdefault(cp, {"tts": "", "keywords": []})
        if annotation.attrib.get("type") == "tts":
            bucket["tts"] = (annotation.text or "").strip()
        else:
            keywords = [
                kw.strip()
                for kw in (annotation.text or "").split("|")
                if kw.strip()
            ]
            bucket["keywords"] = keywords
    return result


def merge_cldr(
    primary: dict[str, dict[str, str | list[str]]],
    derived: dict[str, dict[str, str | list[str]]],
) -> dict[str, dict[str, str | list[str]]]:
    """`annotationsDerived/en.xml` provides keywords for sequences (skin tones,
    families) missing from the base file. Overlay it without overwriting non-empty
    primary entries."""
    merged = {emoji: dict(value) for emoji, value in primary.items()}
    for emoji, value in derived.items():
        bucket = merged.setdefault(emoji, {"tts": "", "keywords": []})
        if not bucket.get("tts"):
            bucket["tts"] = value.get("tts", "")
        existing = list(bucket.get("keywords") or [])
        for keyword in value.get("keywords") or []:
            if keyword not in existing:
                existing.append(keyword)
        bucket["keywords"] = existing
    return merged


def slugify(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value)
    ascii_only = normalized.encode("ascii", "ignore").decode("ascii")
    cleaned = re.sub(r"[^a-zA-Z0-9]+", "_", ascii_only).strip("_").lower()
    return cleaned or "emoji"


def cldr_lookup_keys(emoji: str) -> Iterable[str]:
    """CLDR sometimes keys entries without VS-16 (U+FE0F). Try both."""
    yield emoji
    if VARIATION_SELECTOR_16 in emoji:
        yield emoji.replace(VARIATION_SELECTOR_16, "")


def build_rows(
    emojis: list[dict[str, str]],
    annotations: dict[str, dict[str, str | list[str]]],
) -> list[dict[str, object]]:
    seen_keys: set[str] = set()
    rows: list[dict[str, object]] = []
    for entry in emojis:
        emoji = entry["emoji"]
        name = entry["name"]
        category = entry["category"]
        annotation: dict[str, str | list[str]] = {}
        for key in cldr_lookup_keys(emoji):
            if key in annotations:
                annotation = annotations[key]
                break
        tts = (annotation.get("tts") or name).strip() or name
        keywords_raw = list(annotation.get("keywords") or [])
        # Drop redundant keywords that just echo the name word-for-word.
        name_tokens = {token.lower() for token in re.split(r"\W+", tts) if token}
        keywords = [
            keyword
            for keyword in keywords_raw
            if keyword.lower() not in name_tokens
        ]
        slug = slugify(tts)
        # Keys must be unique across the bundle (LazyVerticalGrid contract).
        key = slug
        suffix = 2
        while key in seen_keys:
            key = f"{slug}_{suffix}"
            suffix += 1
        seen_keys.add(key)
        rows.append(
            {
                "key": key,
                "emoji": emoji,
                "name": tts,
                "slug": slug,
                "category": category,
                "keywords": keywords,
            }
        )
    return rows


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--emoji-version", default=EMOJI_VERSION_DEFAULT, help="Unicode emoji version (e.g. 16.0)")
    parser.add_argument("--cldr-branch", default=CLDR_BRANCH_DEFAULT, help="unicode-org/cldr branch or tag")
    parser.add_argument("--output", default=OUTPUT_PATH_DEFAULT, help="Path (relative to repo root) for the emitted JSON")
    parser.add_argument("--force-refresh", action="store_true", help="Bypass cached downloads")
    args = parser.parse_args(argv)

    print(f"emoji {args.emoji_version} + CLDR {args.cldr_branch}", file=sys.stderr)

    emoji_text = fetch(
        EMOJI_TEST_URL.format(version=args.emoji_version),
        CACHE_DIR / f"emoji-test-{args.emoji_version}.txt",
        force_refresh=args.force_refresh,
    )
    cldr_primary_text = fetch(
        CLDR_ANNOTATIONS_URL.format(branch=args.cldr_branch),
        CACHE_DIR / f"cldr-en-{args.cldr_branch}.xml",
        force_refresh=args.force_refresh,
    )
    cldr_derived_text = fetch(
        CLDR_DERIVED_URL.format(branch=args.cldr_branch),
        CACHE_DIR / f"cldr-en-derived-{args.cldr_branch}.xml",
        force_refresh=args.force_refresh,
    )

    emojis = parse_emoji_list(emoji_text)
    annotations_primary = parse_cldr_annotations(cldr_primary_text)
    annotations_derived = parse_cldr_annotations(cldr_derived_text)
    annotations = merge_cldr(annotations_primary, annotations_derived)
    rows = build_rows(emojis, annotations)

    output_path = REPO / args.output
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(
        json.dumps(rows, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )

    rows_with_keywords = sum(1 for row in rows if row["keywords"])
    print(
        f"  wrote {output_path} ({len(rows)} emojis, {rows_with_keywords} with CLDR keywords)",
        file=sys.stderr,
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
