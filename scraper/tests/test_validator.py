from __future__ import annotations

from app.models import SourceJob
from app.normalizers.jobs import normalize_job
from app.validators.jobs import validate_job_event

COMMAND = {
    "runId": "20000000-0000-4000-8000-000000000001",
    "source": {
        "sourceId": "30000000-0000-4000-8000-000000000001",
        "sourceName": "Greenhouse Zeta",
        "sourceType": "GREENHOUSE",
    },
}


def valid_event() -> dict:
    return normalize_job(
        SourceJob(
            external_job_id="1",
            title="Backend Engineer",
            source_url="https://example.com/jobs/1",
            employment_type="Full-time",
            work_mode="remote",
        ),
        COMMAND,
    )


def test_accepts_valid_event() -> None:
    assert validate_job_event(valid_event()) == []


def test_rejects_bad_url_and_title() -> None:
    event = valid_event()
    event["sourceUrl"] = "not-a-url"
    event["job"]["title"] = " "
    errors = validate_job_event(event)
    assert "sourceUrl must be an absolute http(s) URL" in errors
    assert "job.title is required" in errors


def test_rejects_salary_with_one_bound() -> None:
    event = valid_event()
    event["job"]["salaryMin"] = 100
    event["job"]["salaryMax"] = None
    event["job"]["salaryCurrency"] = "INR"
    event["job"]["salaryPeriod"] = "YEAR"
    assert "salary requires both minimum and maximum bounds" in validate_job_event(event)


def test_rejects_inverted_experience_range() -> None:
    event = valid_event()
    event["job"]["experienceMinYears"] = 5
    event["job"]["experienceMaxYears"] = 2
    assert "experience minimum must not exceed maximum" in validate_job_event(event)


def test_rejects_unknown_employment_type() -> None:
    event = valid_event()
    event["job"]["employmentType"] = "WHATEVER"
    assert "job.employmentType is invalid" in validate_job_event(event)
