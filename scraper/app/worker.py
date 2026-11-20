from __future__ import annotations

import logging
import uuid
from typing import Any, Callable, Mapping

from app.deduplication.local import deduplicate_jobs
from app.models import RunResult
from app.normalizers.jobs import EVENT_NAMESPACE, normalize_job
from app.publishers.base import Publisher
from app.publishers.topology import JOB_DISCOVERED_ROUTING_KEY, RUN_COMPLETED_ROUTING_KEY
from app.sources.base import SourceAdapter
from app.sources.factory import build_source
from app.text import utc_now_iso
from app.validators.jobs import validate_job_event

LOGGER = logging.getLogger(__name__)

SourceFactory = Callable[[Mapping[str, Any]], SourceAdapter]


class IngestionWorker:
    def __init__(self, publisher: Publisher, source_factory: SourceFactory = build_source) -> None:
        self._publisher = publisher
        self._source_factory = source_factory

    def handle(self, command: Mapping[str, Any]) -> RunResult:
        if command.get("commandType") != "ScrapeSourceRequested" or command.get("schemaVersion") != 1:
            raise ValueError("Unsupported scrape command")
        run_id = str(command.get("runId") or "")
        source = command.get("source")
        if not run_id or not isinstance(source, Mapping) or not source.get("sourceId"):
            raise ValueError("Scrape command is missing runId or source")

        LOGGER.info("Starting scrape run %s for source %s", run_id, source.get("sourceName"))
        try:
            adapter = self._source_factory(source)
            jobs = adapter.fetch_jobs(source)
        except Exception as exc:
            LOGGER.exception("Source fetch failed for run %s", run_id)
            result = RunResult("FAILED", 0, 0, (str(exc),))
            self._publish_completion(command, result)
            return result

        unique_jobs = deduplicate_jobs(jobs)
        errors: list[str] = []
        warnings: list[str] = []
        emitted = 0
        if len(unique_jobs) != len(jobs):
            warnings.append(f"Skipped {len(jobs) - len(unique_jobs)} duplicate source records")

        for source_job in unique_jobs:
            event = normalize_job(source_job, command)
            problems = validate_job_event(event)
            if problems:
                warnings.append(
                    f"{source_job.external_job_id or source_job.source_url}: {'; '.join(problems)}"
                )
            try:
                self._publisher.publish(JOB_DISCOVERED_ROUTING_KEY, event)
                emitted += 1
            except Exception as exc:
                LOGGER.exception(
                    "Publishing job %s from run %s failed",
                    source_job.external_job_id or source_job.source_url,
                    run_id,
                )
                errors.append(str(exc))

        status = "SUCCEEDED"
        if errors:
            status = "FAILED" if emitted == 0 else "PARTIAL"
        result = RunResult(status, len(unique_jobs), emitted, tuple(errors), tuple(warnings))
        self._publish_completion(command, result)
        LOGGER.info(
            "Finished scrape run %s: status=%s fetched=%s emitted=%s errors=%s",
            run_id,
            status,
            len(unique_jobs),
            emitted,
            len(errors),
        )
        return result

    def _publish_completion(self, command: Mapping[str, Any], result: RunResult) -> None:
        source = command.get("source") or {}
        run_id = str(command.get("runId") or "")
        source_id = str(source.get("sourceId") or "")
        summary_parts = [*result.errors, *result.warnings]
        summary = " | ".join(summary_parts)[:4000] or None
        event = {
            "eventId": str(uuid.uuid5(EVENT_NAMESPACE, f"{run_id}:{source_id}:completed")),
            "eventType": "ScrapeRunCompleted",
            "schemaVersion": 1,
            "occurredAt": utc_now_iso(),
            "producer": "pune-startup-scraper",
            "runId": run_id,
            "sourceId": source_id,
            "status": result.status,
            "recordsFetched": result.records_fetched,
            "recordsEmitted": result.records_emitted,
            "errorSummary": summary,
        }
        self._publisher.publish(RUN_COMPLETED_ROUTING_KEY, event)
