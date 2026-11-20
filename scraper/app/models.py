from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Any, Mapping


@dataclass(frozen=True)
class SourceJob:
    title: str
    source_url: str
    external_job_id: str | None = None
    application_url: str | None = None
    posted_at: datetime | None = None
    description_html: str | None = None
    location_text: str | None = None
    company_name: str | None = None
    company_website_url: str | None = None
    company_location_text: str | None = None
    employment_type: str | None = None
    work_mode: str | None = None
    experience_min_years: float | None = None
    experience_max_years: float | None = None
    salary_min: float | None = None
    salary_max: float | None = None
    salary_currency: str | None = None
    salary_period: str | None = None
    skills: tuple[str, ...] = ()
    raw: Mapping[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class RunResult:
    status: str
    records_fetched: int
    records_emitted: int
    errors: tuple[str, ...] = ()
    warnings: tuple[str, ...] = ()
