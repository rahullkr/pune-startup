from __future__ import annotations

from app.models import SourceJob
from app.normalizers.jobs import dedupe_key


def deduplicate_jobs(jobs: list[SourceJob]) -> list[SourceJob]:
    seen: set[str] = set()
    unique: list[SourceJob] = []
    for job in jobs:
        keys = dedupe_key(job)
        if any(key in seen for key in keys):
            continue
        seen.update(keys)
        unique.append(job)
    return unique
