from __future__ import annotations

import json
from pathlib import Path

from app.sources.lever.adapter import LeverAdapter

FIXTURES = Path(__file__).parent / "fixtures"


def test_fetches_and_normalizes_lever_jobs() -> None:
    captured: dict[str, str] = {}

    def fake_fetch(url: str, **_kwargs: object) -> list:
        captured["url"] = url
        return json.loads((FIXTURES / "lever_jobs.json").read_text(encoding="utf-8"))

    adapter = LeverAdapter(fetcher=fake_fetch)
    jobs = adapter.fetch_jobs(
        {
            "sourceId": "30000000-0000-4000-8000-000000000002",
            "sourceName": "Lever Zeta",
            "sourceType": "LEVER",
            "baseUrl": "https://jobs.lever.co/zetasoft",
        }
    )

    assert captured["url"] == "https://api.lever.co/v0/postings/zetasoft?mode=json"
    assert len(jobs) == 1
    job = jobs[0]
    assert job.external_job_id == "lever-1001"
    assert job.title == "Frontend Engineer"
    assert job.location_text == "Pune, India"
    assert job.employment_type == "Full-time"
    assert job.work_mode == "remote"
    assert job.salary_min == 1_200_000
    assert job.salary_max == 1_800_000
    assert job.salary_currency == "INR"
    assert job.salary_period == "YEAR"
    assert job.posted_at is not None
    assert "Requirements" in (job.description_html or "")
    assert "React" in job.skills
