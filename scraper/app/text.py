from __future__ import annotations

import html
import re
from datetime import datetime, timezone
from html.parser import HTMLParser

WHITESPACE = re.compile(r"\s+")
NUMERIC = re.compile(r"\d[\d,]*(?:\.\d+)?")


class _TextExtractor(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.parts: list[str] = []

    def handle_data(self, data: str) -> None:
        if data:
            self.parts.append(data)

    def handle_endtag(self, tag: str) -> None:
        if tag.lower() in {"p", "div", "br", "li", "h1", "h2", "h3", "h4", "tr"}:
            self.parts.append(" ")


def normalize_whitespace(value: str | None) -> str | None:
    if value is None:
        return None
    normalized = WHITESPACE.sub(" ", value).strip()
    return normalized or None


def strip_html(value: str | None) -> str | None:
    if not value:
        return None
    text = html.unescape(value)
    if "<" not in text:
        return normalize_whitespace(text)
    parser = _TextExtractor()
    parser.feed(text)
    return normalize_whitespace("".join(parser.parts))


def normalize_key(value: str | None) -> str | None:
    normalized = normalize_whitespace(value)
    return normalized.lower() if normalized else None


def parse_iso_datetime(value: str | None) -> datetime | None:
    if not value:
        return None
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return None
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=timezone.utc)
    return parsed.astimezone(timezone.utc)


def parse_epoch_millis(value: object) -> datetime | None:
    if value is None:
        return None
    try:
        millis = float(value)
    except (TypeError, ValueError):
        return None
    try:
        return datetime.fromtimestamp(millis / 1000, tz=timezone.utc)
    except (OverflowError, OSError, ValueError):
        return None


def to_iso(value: datetime | None) -> str | None:
    if value is None:
        return None
    return value.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")


def utc_now_iso() -> str:
    return to_iso(datetime.now(timezone.utc)) or ""


def parse_numbers(value: str | None) -> list[float]:
    if not value:
        return []
    numbers: list[float] = []
    for match in NUMERIC.findall(value):
        try:
            numbers.append(float(match.replace(",", "")))
        except ValueError:
            continue
    return numbers


def first_non_empty(*values: object) -> str | None:
    for value in values:
        if isinstance(value, str):
            normalized = normalize_whitespace(value)
            if normalized:
                return normalized
    return None
