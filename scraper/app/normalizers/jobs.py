from __future__ import annotations

import uuid
from datetime import datetime
from typing import Any, Mapping

from app.models import SourceJob
from app.normalizers.salary import build_salary, parse_salary_text
from app.normalizers.skills import extract_skills
from app.text import normalize_key, normalize_whitespace, strip_html, to_iso, utc_now_iso

EVENT_NAMESPACE = uuid.UUID("8d6da57a-0c45-4e7d-948b-ade53b8f7a11")
EMPLOYMENT_TYPES = {"FULL_TIME", "PART_TIME", "CONTRACT", "INTERNSHIP", "TEMPORARY"}
WORK_MODES = {"ONSITE", "HYBRID", "REMOTE", "UNKNOWN"}


def _source_value(source: Mapping[str, Any], key: str) -> str | None:
    value = source.get(key)
    return normalize_whitespace(value) if isinstance(value, str) else None


def _company_name(source_job: SourceJob, source: Mapping[str, Any]) -> str:
    if source_job.company_name:
        return source_job.company_name
    name = _source_value(source, "sourceName") or "Unknown company"
    for prefix in ("Greenhouse ", "Lever "):
        if name.casefold().startswith(prefix.casefold()):
            return name[len(prefix) :].strip() or name
    return name


def _employment_type(hint: str | None, title: str) -> str:
    value = f"{hint or ''} {title}".casefold()
    if "intern" in value:
        return "INTERNSHIP"
    if "part-time" in value or "part time" in value:
        return "PART_TIME"
    if "contract" in value or "freelance" in value:
        return "CONTRACT"
    if "temporary" in value or "temp " in value:
        return "TEMPORARY"
    return "FULL_TIME"


def _work_mode(hint: str | None, location: str | None) -> str:
    value = f"{hint or ''} {location or ''}".casefold()
    if "hybrid" in value:
        return "HYBRID"
    if "remote" in value or "work from home" in value or "wfh" in value:
        return "REMOTE"
    if "on-site" in value or "onsite" in value or "in office" in value or "in-person" in value:
        return "ONSITE"
    return "UNKNOWN"


def _event_id(run_id: str, source_id: str, source_job: SourceJob) -> str:
    identity = source_job.external_job_id or source_job.source_url or (
        f"{source_job.title}|{source_job.location_text or ''}"
    )
    return str(uuid.uuid5(EVENT_NAMESPACE, f"{run_id}:{source_id}:job:{identity}"))


def normalize_job(
    source_job: SourceJob,
    command: Mapping[str, Any],
    *,
    occurred_at: datetime | None = None,
) -> dict[str, Any]:
    source = command.get("source") or {}
    run_id = str(command.get("runId") or "")
    source_id = str(source.get("sourceId") or "")
    source_name = _source_value(source, "sourceName") or ""
    source_type = _source_value(source, "sourceType") or ""
    title = normalize_whitespace(source_job.title) or ""
    description = strip_html(source_job.description_html)
    location = normalize_whitespace(source_job.location_text)
    company_name = _company_name(source_job, source)
    employment_type = _employment_type(source_job.employment_type, title)
    work_mode = _work_mode(source_job.work_mode, location)
    skills = extract_skills(source_job.skills, f"{title} {description or ''}")

    salary = None
    if source_job.salary_min is not None and source_job.salary_max is not None:
        salary = build_salary(
            source_job.salary_min,
            source_job.salary_max,
            source_job.salary_currency,
            source_job.salary_period,
        )
    if salary is None and source_job.salary_min is None and source_job.salary_max is None:
        salary = parse_salary_text(source_job.raw.get("salaryText")) if isinstance(source_job.raw, dict) else None

    return {
        "eventId": _event_id(run_id, source_id, source_job),
        "eventType": "JobDiscovered",
        "schemaVersion": 1,
        "occurredAt": to_iso(occurred_at) or utc_now_iso(),
        "producer": "pune-startup-scraper",
        "runId": run_id,
        "sourceId": source_id,
        "sourceName": source_name,
        "sourceType": source_type,
        "externalJobId": normalize_whitespace(source_job.external_job_id),
        "sourceUrl": normalize_whitespace(source_job.source_url),
        "applicationUrl": normalize_whitespace(source_job.application_url),
        "postedAt": to_iso(source_job.posted_at),
        "company": {
            "name": company_name,
            "websiteUrl": normalize_whitespace(source_job.company_website_url),
            "locationText": normalize_whitespace(source_job.company_location_text),
        },
        "job": {
            "title": title,
            "description": description[:50000] if description else None,
            "locationText": location,
            "employmentType": employment_type,
            "workMode": work_mode,
            "experienceMinYears": source_job.experience_min_years,
            "experienceMaxYears": source_job.experience_max_years,
            "salaryMin": salary.minimum if salary else None,
            "salaryMax": salary.maximum if salary else None,
            "salaryCurrency": salary.currency if salary else None,
            "salaryPeriod": salary.period if salary else None,
            "skills": skills,
        },
        "rawPayload": dict(source_job.raw),
    }


def dedupe_key(source_job: SourceJob) -> tuple[str, ...]:
    keys: list[str] = []
    if source_job.external_job_id:
        keys.append(f"external:{source_job.external_job_id}")
    for url in (source_job.application_url, source_job.source_url):
        if url:
            keys.append(f"url:{url}")
    title = normalize_key(source_job.title)
    if title and not source_job.external_job_id:
        keys.append(
            "identity:"
            f"{normalize_key(source_job.company_name) or ''}|{title}|{normalize_key(source_job.location_text) or ''}"
        )
    return tuple(keys)
