from __future__ import annotations

from typing import Any, Callable, Mapping
from urllib.parse import parse_qsl, urlencode, urlparse, urlunparse

from app.config import Settings
from app.http import fetch_json
from app.models import SourceJob
from app.normalizers.experience import parse_experience_text
from app.normalizers.skills import split_skills
from app.text import first_non_empty, normalize_whitespace, parse_iso_datetime

FetchJson = Callable[..., Any]


class GreenhouseAdapter:
    def __init__(self, fetcher: FetchJson = fetch_json, settings: Settings | None = None) -> None:
        self._fetch_json = fetcher
        self._settings = settings or Settings()

    def fetch_jobs(self, source: Mapping[str, Any]) -> list[SourceJob]:
        base_url = normalize_whitespace(source.get("baseUrl"))
        if not base_url:
            raise ValueError("Greenhouse source is missing baseUrl")
        payload = self._fetch_json(self._endpoint(base_url), settings=self._settings)
        jobs = payload.get("jobs") if isinstance(payload, dict) else None
        if not isinstance(jobs, list):
            raise ValueError("Greenhouse response did not contain a jobs list")
        return [self._parse_job(job, source) for job in jobs if isinstance(job, dict)]

    def _endpoint(self, base_url: str) -> str:
        parsed = urlparse(base_url)
        if parsed.netloc == "boards.greenhouse.io":
            token = parsed.path.strip("/").split("/")[0]
            parsed = parsed._replace(netloc="boards-api.greenhouse.io", path=f"/v1/boards/{token}/jobs", query="")
        path = parsed.path.rstrip("/")
        if not path.endswith("/jobs"):
            path = f"{path}/jobs"
        query = dict(parse_qsl(parsed.query))
        query["content"] = "true"
        return urlunparse(parsed._replace(path=path, query=urlencode(query)))

    def _parse_job(self, job: dict[str, Any], source: Mapping[str, Any]) -> SourceJob:
        metadata = self._metadata(job)
        title = first_non_empty(job.get("title")) or ""
        salary_text = self._metadata_value(metadata, "salary range", "compensation", "salary")
        experience_text = self._metadata_value(metadata, "experience", "years of experience", "experience level")
        experience_min, experience_max = parse_experience_text(experience_text)
        skills_text = self._metadata_value(metadata, "skills", "technologies", "tech stack")
        location = job.get("location") if isinstance(job.get("location"), dict) else {}
        return SourceJob(
            external_job_id=str(job["id"]) if job.get("id") is not None else None,
            title=title,
            source_url=first_non_empty(job.get("absolute_url")) or "",
            application_url=first_non_empty(job.get("absolute_url")),
            posted_at=parse_iso_datetime(first_non_empty(job.get("first_published"), job.get("created_at"))),
            description_html=first_non_empty(job.get("content"), job.get("description")),
            location_text=first_non_empty(location.get("name")),
            company_name=self._metadata_value(metadata, "company", "company name")
            or normalize_whitespace(source.get("sourceName")),
            company_website_url=self._metadata_value(metadata, "website", "company website"),
            company_location_text=first_non_empty(location.get("name")),
            employment_type=self._metadata_value(metadata, "employment type", "job type"),
            work_mode=self._metadata_value(
                metadata, "workplace type", "work type", "location type", "remote"
            ),
            experience_min_years=experience_min,
            experience_max_years=experience_max,
            skills=tuple(split_skills(skills_text)),
            raw={**job, "salaryText": salary_text},
        )

    def _metadata(self, job: dict[str, Any]) -> dict[str, str]:
        result: dict[str, str] = {}
        for item in job.get("metadata") or []:
            if not isinstance(item, dict):
                continue
            name = normalize_whitespace(item.get("name"))
            value = item.get("value")
            if name and value is not None:
                result[name.casefold()] = str(value)
        return result

    def _metadata_value(self, metadata: Mapping[str, str], *names: str) -> str | None:
        for name in names:
            value = normalize_whitespace(metadata.get(name.casefold()))
            if value:
                return value
        return None
