from __future__ import annotations

import logging

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
    try:
        consumer.run_forever()
    except KeyboardInterrupt:
        logging.getLogger(__name__).info("Scraper worker stopped")
    finally:
        publisher.close()


if __name__ == "__main__":
    main()
