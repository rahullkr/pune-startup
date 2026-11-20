from __future__ import annotations

import uuid
from typing import Any, Mapping
from urllib.parse import urlparse

EMPLOYMENT_TYPES = {"FULL_TIME", "PART_TIME", "CONTRACT", "INTERNSHIP", "TEMPORARY"}
WORK_MODES = {"ONSITE", "HYBRID", "REMOTE", "UNKNOWN"}
SOURCE_TYPES = {"GREENHOUSE", "LEVER", "ASHBY", "COMPANY_CAREERS", "PARTNER_FEED", "OTHER"}


def _valid_url(value: object) -> bool:
    if not isinstance(value, str) or not value:
        return False
    parsed = urlparse(value)
    return parsed.scheme in {"http", "https"} and bool(parsed.netloc)


def _valid_uuid(value: object) -> bool:
    try:
        uuid.UUID(str(value))
        return True
    except (ValueError, TypeError, AttributeError):
        return False


def _number(value: object) -> float | None:
    if value is None:
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def validate_job_event(event: Mapping[str, Any]) -> list[str]:
    errors: list[str] = []
    if event.get("eventType") != "JobDiscovered":
        errors.append("eventType must be JobDiscovered")
    if event.get("schemaVersion") != 1:
        errors.append("schemaVersion must be 1")
    if not _valid_uuid(event.get("eventId")):
        errors.append("eventId must be a UUID")
    if not _valid_uuid(event.get("runId")):
        errors.append("runId must be a UUID")
    if not _valid_uuid(event.get("sourceId")):
        errors.append("sourceId must be a UUID")
    if not event.get("sourceName"):
        errors.append("sourceName is required")
    if event.get("sourceType") not in SOURCE_TYPES:
        errors.append("sourceType is invalid")
    if not _valid_url(event.get("sourceUrl")):
        errors.append("sourceUrl must be an absolute http(s) URL")
    if event.get("applicationUrl") and not _valid_url(event.get("applicationUrl")):
        errors.append("applicationUrl must be an absolute http(s) URL")

    job = event.get("job")
    if not isinstance(job, Mapping):
        errors.append("job is required")
        return errors
    title = job.get("title")
    if not isinstance(title, str) or not title.strip():
        errors.append("job.title is required")
    elif len(title) > 250:
        errors.append("job.title exceeds 250 characters")
    if job.get("employmentType") not in EMPLOYMENT_TYPES:
        errors.append("job.employmentType is invalid")
    if job.get("workMode") not in WORK_MODES:
        errors.append("job.workMode is invalid")

    for field in ("experienceMinYears", "experienceMaxYears", "salaryMin", "salaryMax"):
        value = _number(job.get(field))
        if value is not None and value < 0:
            errors.append(f"job.{field} must be non-negative")

    experience_min = _number(job.get("experienceMinYears"))
    experience_max = _number(job.get("experienceMaxYears"))
    if experience_min is not None and experience_max is not None and experience_min > experience_max:
        errors.append("experience minimum must not exceed maximum")

    salary_min = _number(job.get("salaryMin"))
    salary_max = _number(job.get("salaryMax"))
    if salary_min is not None or salary_max is not None:
        if salary_min is None or salary_max is None:
            errors.append("salary requires both minimum and maximum bounds")
        elif salary_min > salary_max:
            errors.append("salary minimum must not exceed maximum")
        currency = job.get("salaryCurrency")
        if not isinstance(currency, str) or len(currency) != 3 or not currency.isalpha():
            errors.append("salary currency must be a three-letter code")
        if not job.get("salaryPeriod"):
            errors.append("salary period is required when salary is present")

    skills = job.get("skills")
    if not isinstance(skills, list) or any(not isinstance(skill, str) for skill in skills):
        errors.append("job.skills must be a list of strings")
    return errors
