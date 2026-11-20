from __future__ import annotations

from app.deduplication.local import deduplicate_jobs
from app.models import SourceJob


def test_deduplicates_by_external_id_url_and_identity() -> None:
    jobs = [
        SourceJob(
            external_job_id="1",
            title="Backend Engineer",
            source_url="https://example.com/jobs/1",
            company_name="Acme",
        ),
        SourceJob(
            external_job_id="1",
            title="Backend Engineer",
            source_url="https://example.com/jobs/one",
            company_name="Acme",
        ),
        SourceJob(
            external_job_id="2",
            title="Backend Engineer",
            source_url="https://example.com/jobs/1",
            company_name="Acme",
        ),
        SourceJob(
            external_job_id="3",
            title="Backend Engineer",
            source_url="https://example.com/jobs/3",
            company_name="Acme",
        ),
        SourceJob(
            external_job_id="4",
            title="Data Engineer",
            source_url="https://example.com/jobs/4",
            company_name="Acme",
        ),
    ]

    unique = deduplicate_jobs(jobs)

    assert [job.external_job_id for job in unique] == ["1", "3", "4"]
