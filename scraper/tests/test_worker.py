from __future__ import annotations

import uuid
from typing import Any

import pytest

from app.models import SourceJob
from app.publishers.topology import JOB_DISCOVERED_ROUTING_KEY, RUN_COMPLETED_ROUTING_KEY
from app.worker import IngestionWorker


def command() -> dict[str, Any]:
    return {
        "commandId": str(uuid.uuid4()),
        "commandType": "ScrapeSourceRequested",
        "schemaVersion": 1,
        "requestedAt": "2026-11-13T08:00:00Z",
        "runId": "20000000-0000-4000-8000-000000000001",
        "source": {
            "sourceId": "30000000-0000-4000-8000-000000000001",
            "sourceName": "Acme Careers",
            "sourceType": "GREENHOUSE",
            "baseUrl": "https://example.com/jobs",
        },
    }


class FakePublisher:
    def __init__(self, fail_jobs: int = 0) -> None:
        self.messages: list[tuple[str, dict[str, Any]]] = []
        self.fail_jobs = fail_jobs

    def publish(self, routing_key: str, payload: dict[str, Any]) -> None:
        if routing_key == JOB_DISCOVERED_ROUTING_KEY and self.fail_jobs > 0:
            self.fail_jobs -= 1
            raise RuntimeError("broker rejected job event")
        self.messages.append((routing_key, dict(payload)))


class FakeAdapter:
    def __init__(self, jobs: list[SourceJob] | None = None, error: Exception | None = None) -> None:
        self.jobs = jobs or []
        self.error = error

    def fetch_jobs(self, source: dict[str, Any]) -> list[SourceJob]:
        if self.error:
            raise self.error
        return self.jobs


def source_job(external_id: str, title: str = "Backend Engineer") -> SourceJob:
    return SourceJob(
        external_job_id=external_id,
        title=title,
        source_url=f"https://example.com/jobs/{external_id}",
        location_text="Pune",
        company_name="Acme",
        employment_type="Full-time",
        work_mode="remote",
    )


def test_successful_run_publishes_jobs_and_completion() -> None:
    publisher = FakePublisher()
    worker = IngestionWorker(publisher, lambda source: FakeAdapter([source_job("1"), source_job("2")]))

    result = worker.handle(command())

    assert result.status == "SUCCEEDED"
    assert result.records_fetched == 2
    assert result.records_emitted == 2
    routes = [route for route, _ in publisher.messages]
    assert routes == [JOB_DISCOVERED_ROUTING_KEY, JOB_DISCOVERED_ROUTING_KEY, RUN_COMPLETED_ROUTING_KEY]
    completion = publisher.messages[-1][1]
    assert completion["status"] == "SUCCEEDED"
    assert completion["recordsFetched"] == 2
    assert completion["recordsEmitted"] == 2


def test_retry_keeps_job_and_completion_event_ids_stable() -> None:
    publisher = FakePublisher()
    worker = IngestionWorker(publisher, lambda source: FakeAdapter([source_job("1")]))

    worker.handle(command())
    worker.handle(command())

    job_ids = [payload["eventId"] for route, payload in publisher.messages if route == JOB_DISCOVERED_ROUTING_KEY]
    completion_ids = [
        payload["eventId"] for route, payload in publisher.messages if route == RUN_COMPLETED_ROUTING_KEY
    ]
    assert job_ids[0] == job_ids[1]
    assert completion_ids[0] == completion_ids[1]


def test_fetch_failure_still_publishes_failed_completion() -> None:
    publisher = FakePublisher()
    worker = IngestionWorker(publisher, lambda source: FakeAdapter(error=RuntimeError("source offline")))

    result = worker.handle(command())

    assert result.status == "FAILED"
    assert result.records_emitted == 0
    assert len(publisher.messages) == 1
    route, completion = publisher.messages[0]
    assert route == RUN_COMPLETED_ROUTING_KEY
    assert completion["status"] == "FAILED"
    assert "source offline" in completion["errorSummary"]


def test_partial_publish_reports_partial_completion() -> None:
    publisher = FakePublisher(fail_jobs=1)
    worker = IngestionWorker(publisher, lambda source: FakeAdapter([source_job("1"), source_job("2")]))

    result = worker.handle(command())

    assert result.status == "PARTIAL"
    assert result.records_emitted == 1
    assert publisher.messages[-1][1]["status"] == "PARTIAL"
    assert "broker rejected job event" in publisher.messages[-1][1]["errorSummary"]


def test_rejects_unknown_command() -> None:
    worker = IngestionWorker(FakePublisher(), lambda source: FakeAdapter())
    with pytest.raises(ValueError):
        worker.handle({"commandType": "Unknown", "schemaVersion": 1})
