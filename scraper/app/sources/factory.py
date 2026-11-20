from __future__ import annotations

from typing import Mapping

from app.sources.base import SourceAdapter
from app.sources.greenhouse.adapter import GreenhouseAdapter
from app.sources.lever.adapter import LeverAdapter


def build_source(source: Mapping[str, object]) -> SourceAdapter:
    source_type = str(source.get("sourceType") or "").upper()
    if source_type == "GREENHOUSE":
        return GreenhouseAdapter()
    if source_type == "LEVER":
        return LeverAdapter()
    raise ValueError(f"Unsupported source type: {source_type or '<missing>'}")
