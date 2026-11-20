from __future__ import annotations

import uuid
from datetime import datetime, timezone

from app.models import SourceJob
from app.normalizers.jobs import normalize_job
from app.normalizers.salary import parse_salary_text

RUN_ID = "20000000-0000-4000-8000-000000000001"
SOURCE_ID = "30000000-0000-4000-8000-000000000001"


def command() -> dict:
    return {
        "commandId": "10000000-0000-4000-8000-000000000001",
        "commandType": "ScrapeSourceRequested",
        "schemaVersion": 1,
        "runId": RUN_ID,
        "source": {
            "sourceId": SOURCE_ID,
            "sourceName": "Greenhouse Zeta",
            "sourceType": "GREENHOUSE",
            "baseUrl": "https://boards-api.greenhouse.io/v1/boards/zetasoft/jobs",
        },
    }


def greenhouse_job() -> SourceJob:
    return SourceJob(
        external_job_id="4123456",
        title="  Backend   Engineer ",
        source_url="https://boards.greenhouse.io/zetasoft/jobs/4123456",
        application_url="https://boards.greenhouse.io/zetasoft/jobs/4123456",
        posted_at=datetime(2026, 11, 10, 6, 30, tzinfo=timezone.utc),
        description_html="&lt;p&gt;Build &lt;strong&gt;Java&lt;/strong&gt; services with Spring Boot and PostgreSQL.&lt;/p&gt;",
        location_text="Pune, Maharashtra",
        company_name="Greenhouse Zeta",
        company_website_url="https://zetasoft.example",
        employment_type="Full-time",
        work_mode="Hybrid",
        experience_min_years=2,
        experience_max_years=4,
        skills=("Java", "Kotlin"),
        raw={"id": 4123456, "salaryText": "₹12,00,000 - ₹18,00,000 per year"},
    )


def test_normalizes_job_event_and_sanitizes_optional_fields() -> None:
    occurred_at = datetime(2026, 11, 13, 8, 0, tzinfo=timezone.utc)
    event = normalize_job(greenhouse_job(), command(), occurred_at=occurred_at)

    assert event["eventType"] == "JobDiscovered"
    assert event["runId"] == RUN_ID
    assert event["sourceId"] == SOURCE_ID
    assert event["occurredAt"] == "2026-11-13T08:00:00Z"
    assert event["postedAt"] == "2026-11-10T06:30:00Z"
    assert event["job"]["title"] == "Backend Engineer"
    assert event["job"]["description"].startswith("Build Java services")
    assert event["job"]["employmentType"] == "FULL_TIME"
    assert event["job"]["workMode"] == "HYBRID"
    assert event["job"]["salaryMin"] == 1_200_000
    assert event["job"]["salaryMax"] == 1_800_000
    assert event["job"]["salaryCurrency"] == "INR"
    assert event["job"]["salaryPeriod"] == "YEAR"
    assert set(event["job"]["skills"]) >= {"Java", "Kotlin", "Spring Boot", "PostgreSQL"}
    assert event["company"]["name"] == "Greenhouse Zeta"


def test_event_id_is_deterministic_per_run_and_job() -> None:
    first = normalize_job(greenhouse_job(), command())
    second = normalize_job(greenhouse_job(), command())
    assert first["eventId"] == second["eventId"]
    assert uuid.UUID(first["eventId"])


def test_parses_lpa_and_monthly_salary_text() -> None:
    annual = parse_salary_text("12-18 LPA")
    monthly = parse_salary_text("₹50,000 - ₹70,000 per month")

    assert annual is not None
    assert annual.minimum == 1_200_000
    assert annual.maximum == 1_800_000
    assert annual.currency == "INR"
    assert annual.period == "YEAR"
    assert monthly is not None
    assert monthly.minimum == 50_000
    assert monthly.maximum == 70_000
    assert monthly.period == "MONTH"


def test_discards_incomplete_salary_instead_of_failing_the_job() -> None:
    job = SourceJob(
        title="Analyst",
        source_url="https://example.com/jobs/1",
        employment_type="Full-time",
        raw={"salaryText": "₹12,00,000 per year"},
    )
    event = normalize_job(job, command())
    assert event["job"]["salaryMin"] is None
    assert event["job"]["salaryCurrency"] is None
