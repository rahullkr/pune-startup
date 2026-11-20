from __future__ import annotations

import json
import logging
import time
from typing import Any, Callable, Mapping

import pika

from app.config import Settings
from app.publishers import topology

LOGGER = logging.getLogger(__name__)


def _parameters(settings: Settings) -> pika.ConnectionParameters:
    credentials = pika.PlainCredentials(settings.rabbitmq_user, settings.rabbitmq_password)
    return pika.ConnectionParameters(
        host=settings.rabbitmq_host,
        port=settings.rabbitmq_port,
        virtual_host=settings.rabbitmq_vhost,
        credentials=credentials,
        heartbeat=60,
        blocked_connection_timeout=30,
    )


def declare_topology(channel: pika.adapters.blocking_connection.BlockingChannel) -> None:
    channel.exchange_declare(topology.EXCHANGE, exchange_type="topic", durable=True)
    channel.exchange_declare(topology.DEAD_LETTER_EXCHANGE, exchange_type="direct", durable=True)
    dead_letter_args = {
        "x-dead-letter-exchange": topology.DEAD_LETTER_EXCHANGE,
        "x-dead-letter-routing-key": topology.DEAD_LETTER_ROUTING_KEY,
    }
    channel.queue_declare(topology.SCRAPE_REQUESTED_QUEUE, durable=True, arguments=dead_letter_args)
    channel.queue_declare(topology.JOB_DISCOVERED_QUEUE, durable=True, arguments=dead_letter_args)
    channel.queue_declare(topology.RUN_COMPLETED_QUEUE, durable=True, arguments=dead_letter_args)
    channel.queue_declare(topology.DEAD_LETTER_QUEUE, durable=True)
    channel.queue_bind(
        topology.SCRAPE_REQUESTED_QUEUE, topology.EXCHANGE, topology.SCRAPE_REQUESTED_ROUTING_KEY
    )
    channel.queue_bind(
        topology.JOB_DISCOVERED_QUEUE, topology.EXCHANGE, topology.JOB_DISCOVERED_ROUTING_KEY
    )
    channel.queue_bind(topology.RUN_COMPLETED_QUEUE, topology.EXCHANGE, topology.RUN_COMPLETED_ROUTING_KEY)
    channel.queue_bind(
        topology.DEAD_LETTER_QUEUE, topology.DEAD_LETTER_EXCHANGE, topology.DEAD_LETTER_ROUTING_KEY
    )


class RabbitPublisher:
    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._connection: pika.BlockingConnection | None = None
        self._channel: pika.adapters.blocking_connection.BlockingChannel | None = None

    def publish(self, routing_key: str, payload: Mapping[str, Any]) -> None:
        channel = self._ensure_channel()
        message_id = payload.get("eventId") or payload.get("commandId")
        message_type = payload.get("eventType") or payload.get("commandType")
        channel.basic_publish(
            exchange=topology.EXCHANGE,
            routing_key=routing_key,
            body=json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8"),
            properties=pika.BasicProperties(
                content_type="application/json",
                content_encoding="utf-8",
                delivery_mode=2,
                message_id=str(message_id) if message_id else None,
                type=str(message_type) if message_type else None,
            ),
            mandatory=False,
        )

    def _ensure_channel(self) -> pika.adapters.blocking_connection.BlockingChannel:
        if self._connection and self._connection.is_open and self._channel and self._channel.is_open:
            return self._channel
        self.close()
        self._connection = pika.BlockingConnection(_parameters(self._settings))
        self._channel = self._connection.channel()
        self._channel.confirm_delivery()
        declare_topology(self._channel)
        return self._channel

    def close(self) -> None:
        if self._connection and self._connection.is_open:
            try:
                self._connection.close()
            except pika.exceptions.AMQPError:
                LOGGER.debug("RabbitMQ publisher close failed", exc_info=True)
        self._connection = None
        self._channel = None


class RabbitCommandConsumer:
    def __init__(self, settings: Settings, handler: Callable[[dict[str, Any]], Any]) -> None:
        self._settings = settings
        self._handler = handler

    def run_forever(self) -> None:
        while True:
            connection = None
            try:
                connection = pika.BlockingConnection(_parameters(self._settings))
                channel = connection.channel()
                declare_topology(channel)
                channel.basic_qos(prefetch_count=self._settings.rabbitmq_prefetch)
                channel.basic_consume(
                    queue=topology.SCRAPE_REQUESTED_QUEUE,
                    on_message_callback=self._on_message,
                )
                LOGGER.info("Waiting for scrape commands on %s", topology.SCRAPE_REQUESTED_QUEUE)
                channel.start_consuming()
            except KeyboardInterrupt:
                raise
            except pika.exceptions.AMQPError:
                LOGGER.exception("RabbitMQ consumer connection failed; retrying in 5 seconds")
                time.sleep(5)
            finally:
                if connection and connection.is_open:
                    connection.close()

    def _on_message(
        self,
        channel: pika.adapters.blocking_connection.BlockingChannel,
        method: pika.spec.Basic.Deliver,
        properties: pika.spec.BasicProperties,
        body: bytes,
    ) -> None:
        try:
            command = json.loads(body.decode("utf-8"))
            if not isinstance(command, dict):
                raise ValueError("Command payload must be a JSON object")
            self._handler(command)
            channel.basic_ack(delivery_tag=method.delivery_tag)
        except Exception:
            LOGGER.exception("Scrape command failed; routing message to the dead-letter queue")
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
