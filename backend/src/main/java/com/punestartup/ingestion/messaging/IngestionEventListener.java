package com.punestartup.ingestion.messaging;

import com.punestartup.ingestion.event.JobDiscoveredEvent;
import com.punestartup.ingestion.event.ScrapeRunCompletedEvent;
import com.punestartup.ingestion.service.JobIngestionService;
import com.punestartup.ingestion.service.ScrapingRunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class IngestionEventListener {

    private static final Logger log = LoggerFactory.getLogger(IngestionEventListener.class);

    private final JobIngestionService jobIngestionService;
    private final ScrapingRunService runService;

    public IngestionEventListener(JobIngestionService jobIngestionService, ScrapingRunService runService) {
        this.jobIngestionService = jobIngestionService;
        this.runService = runService;
    }

    @RabbitListener(queues = IngestionMessaging.JOB_DISCOVERED_QUEUE)
    public void onJobDiscovered(JobDiscoveredEvent event) {
        log.info("Received JobDiscovered event {} for run {}", event.eventId(), event.runId());
        jobIngestionService.ingest(event);
    }

    @RabbitListener(queues = IngestionMessaging.RUN_COMPLETED_QUEUE)
    public void onRunCompleted(ScrapeRunCompletedEvent event) {
        log.info("Received ScrapeRunCompleted event {} for run {}", event.eventId(), event.runId());
        runService.completeRun(event);
    }
}
