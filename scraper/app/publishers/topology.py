EXCHANGE = "pune.ingestion"
DEAD_LETTER_EXCHANGE = "pune.ingestion.dlx"
DEAD_LETTER_QUEUE = "pune.ingestion.dead-letter"
DEAD_LETTER_ROUTING_KEY = "dead-letter"

SCRAPE_REQUESTED_ROUTING_KEY = "scrape.source.requested"
JOB_DISCOVERED_ROUTING_KEY = "job.discovered"
RUN_COMPLETED_ROUTING_KEY = "scrape.run.completed"

SCRAPE_REQUESTED_QUEUE = "pune.ingestion.scrape-requested"
JOB_DISCOVERED_QUEUE = "pune.ingestion.job-discovered"
RUN_COMPLETED_QUEUE = "pune.ingestion.run-completed"
