from __future__ import annotations

import json
from pathlib import Path

from app.sources.greenhouse.adapter import GreenhouseAdapter

FIXTURES = Path(__file__).parent / "fixtures"


def test_fetches_and_normalizes_greenhouse_jobs() -> None:
    captured: dict[str, str] = {}

    def fake_fetch(url: str, **_kwargs: object) -> dict:
        captured["url"] = url
        return json.loads((FIXTURES / "greenhouse_jobs.json").read_text(encoding="utf-8"))

    adapter = GreenhouseAdapter(fetcher=fake_fetch)
    jobs = adapter.fetch_jobs(
        {
            "sourceId": "30000000-0000-4000-8000-000000000001",
            "sourceName": "Greenhouse Zeta",
            "sourceType": "GREENHOUSE",
            "baseUrl": "https://boards.greenhouse.io/zetasoft",
        }
    )

    assert captured["url"] == "https://boards-api.greenhouse.io/v1/boards/zetasoft/jobs?content=true"
    assert len(jobs) == 1
    job = jobs[0]
    assert job.external_job_id == "4123456"
    assert job.title == "Backend Engineer"
    assert job.company_name == "Greenhouse Zeta"
    assert job.location_text == "Pune, Maharashtra"
    assert job.employment_type == "Full-time"
    assert job.work_mode == "Hybrid"
    assert job.experience_min_years == 2
    assert job.experience_max_years == 4
    assert job.salary_min is None
    assert job.raw["salaryText"] == "₹12,00,000 - ₹18,00,000 per year"
    assert "Java" in job.skills


def test_uses_existing_api_url_and_content_query() -> None:
    captured: dict[str, str] = {}

    def fake_fetch(url: str, **_kwargs: object) -> dict:
        captured["url"] = url
        return {"jobs": []}

    GreenhouseAdapter(fetcher=fake_fetch).fetch_jobs(
        {"sourceType": "GREENHOUSE", "baseUrl": "https://boards-api.greenhouse.io/v1/boards/acme/jobs"}
    )

    assert captured["url"] == "https://boards-api.greenhouse.io/v1/boards/acme/jobs?content=true"
