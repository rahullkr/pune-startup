from __future__ import annotations

import logging
import os

from app.config import Settings
from app.publishers.rabbit import RabbitCommandConsumer, RabbitPublisher
from app.worker import IngestionWorker


def main() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    settings = Settings.from_env()
    publisher = RabbitPublisher(settings)
    worker = IngestionWorker(publisher)
    consumer = RabbitCommandConsumer(settings, worker.handle)
    max_messages = int(os.getenv("SCRAPER_MAX_MESSAGES", "0"))
    idle_timeout_seconds = float(os.getenv("SCRAPER_IDLE_TIMEOUT_SECONDS", "300"))
    try:
        if max_messages > 0:
            processed = consumer.run_batch(max_messages, idle_timeout_seconds)
            logging.getLogger(__name__).info("Processed %s scrape commands", processed)
        else:
            consumer.run_forever()
    except KeyboardInterrupt:
        logging.getLogger(__name__).info("Scraper worker stopped")
    finally:
        publisher.close()


if __name__ == "__main__":
    main()
