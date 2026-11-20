from __future__ import annotations

from typing import Any, Callable, Mapping
from urllib.parse import parse_qsl, urlencode, urlparse, urlunparse

from app.config import Settings
from app.http import fetch_json
from app.models import SourceJob
from app.normalizers.salary import interval_period
from app.text import first_non_empty, normalize_whitespace, parse_epoch_millis, strip_html

FetchJson = Callable[..., Any]


class LeverAdapter:
    def __init__(self, fetcher: FetchJson = fetch_json, settings: Settings | None = None) -> None:
        self._fetch_json = fetcher
        self._settings = settings or Settings()

    def fetch_jobs(self, source: Mapping[str, Any]) -> list[SourceJob]:
        base_url = normalize_whitespace(source.get("baseUrl"))
        if not base_url:
            raise ValueError("Lever source is missing baseUrl")
        payload = self._fetch_json(self._endpoint(base_url), settings=self._settings)
        if not isinstance(payload, list):
            raise ValueError("Lever response did not contain a postings list")
        return [self._parse_job(job, source) for job in payload if isinstance(job, dict)]

    def _endpoint(self, base_url: str) -> str:
        parsed = urlparse(base_url)
        if parsed.netloc == "jobs.lever.co":
            token = parsed.path.strip("/").split("/")[0]
            parsed = parsed._replace(netloc="api.lever.co", path=f"/v0/postings/{token}", query="")
        path = parsed.path.rstrip("/")
        if "/postings/" not in path:
            path = f"/v0/postings/{path.rsplit('/', 1)[-1]}"
        query = dict(parse_qsl(parsed.query))
        query["mode"] = "json"
        return urlunparse(parsed._replace(path=path, query=urlencode(query)))

    def _parse_job(self, job: dict[str, Any], source: Mapping[str, Any]) -> SourceJob:
        categories = job.get("categories") if isinstance(job.get("categories"), dict) else {}
        salary = job.get("salaryRange") if isinstance(job.get("salaryRange"), dict) else {}
        description = self._description(job)
        locations = categories.get("allLocations") if isinstance(categories.get("allLocations"), list) else []
        location = first_non_empty(categories.get("location"), ", ".join(str(item) for item in locations))
        tags = [str(tag) for tag in job.get("tags") or [] if isinstance(tag, (str, int))]
        return SourceJob(
            external_job_id=str(job["id"]) if job.get("id") is not None else None,
            title=first_non_empty(job.get("text")) or "",
            source_url=first_non_empty(job.get("hostedUrl")) or "",
            application_url=first_non_empty(job.get("applyUrl"), job.get("hostedUrl")),
            posted_at=parse_epoch_millis(job.get("createdAt")),
            description_html=description,
            location_text=location,
            company_name=normalize_whitespace(source.get("sourceName")),
            company_website_url=first_non_empty(job.get("companyUrl")),
            company_location_text=location,
            employment_type=first_non_empty(categories.get("commitment"), categories.get("team")),
            work_mode=first_non_empty(categories.get("workplaceType")),
            salary_min=self._number(salary.get("min")),
            salary_max=self._number(salary.get("max")),
            salary_currency=normalize_whitespace(salary.get("currency")),
            salary_period=interval_period(normalize_whitespace(salary.get("interval"))),
            skills=tuple(tags),
            raw=job,
        )

    def _description(self, job: dict[str, Any]) -> str | None:
        parts = [
            first_non_empty(job.get("descriptionPlain"), job.get("description")),
            first_non_empty(job.get("additionalPlain"), job.get("additional")),
        ]
        for item in job.get("lists") or []:
            if not isinstance(item, dict):
                continue
            text = strip_html(first_non_empty(item.get("text")))
            content = strip_html(first_non_empty(item.get("content")))
            if text or content:
                parts.append(" ".join(part for part in (text, content) if part))
        return " ".join(part for part in parts if part) or None

    def _number(self, value: object) -> float | None:
        try:
            return float(value)  # type: ignore[arg-type]
        except (TypeError, ValueError):
            return None
