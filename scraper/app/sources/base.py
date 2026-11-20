from __future__ import annotations

from typing import Any, Mapping, Protocol

from app.models import SourceJob


class SourceAdapter(Protocol):
    def fetch_jobs(self, source: Mapping[str, Any]) -> list[SourceJob]:
        ...
