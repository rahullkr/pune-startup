package com.punestartup.ingestion.messaging;

public final class IngestionMessaging {

    public static final String EXCHANGE = "pune.ingestion";
    public static final String DEAD_LETTER_EXCHANGE = "pune.ingestion.dlx";
    public static final String DEAD_LETTER_QUEUE = "pune.ingestion.dead-letter";
    public static final String DEAD_LETTER_ROUTING_KEY = "dead-letter";

    public static final String SCRAPE_REQUESTED_ROUTING_KEY = "scrape.source.requested";
    public static final String JOB_DISCOVERED_ROUTING_KEY = "job.discovered";
    public static final String RUN_COMPLETED_ROUTING_KEY = "scrape.run.completed";

    public static final String SCRAPE_REQUESTED_QUEUE = "pune.ingestion.scrape-requested";
    public static final String JOB_DISCOVERED_QUEUE = "pune.ingestion.job-discovered";
    public static final String RUN_COMPLETED_QUEUE = "pune.ingestion.run-completed";

    private IngestionMessaging() {
    }
}
