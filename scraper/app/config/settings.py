from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    rabbitmq_user: str = "app_user"
    rabbitmq_password: str = "change_me_locally"
    rabbitmq_vhost: str = "/"
    rabbitmq_prefetch: int = 1
    http_timeout_seconds: float = 20.0
    http_retries: int = 2
    user_agent: str = "pune-startup-scraper/1.0"

    @classmethod
    def from_env(cls) -> "Settings":
        if os.getenv("SCRAPER_REQUIRE_ENV", "false").lower() in {"1", "true", "yes"}:
            required = ("RABBITMQ_HOST", "RABBITMQ_USER", "RABBITMQ_PASSWORD")
            missing = [name for name in required if not os.getenv(name)]
            if missing:
                raise RuntimeError("Missing required scraper environment variables: " + ", ".join(missing))
        return cls(
            rabbitmq_host=os.getenv("RABBITMQ_HOST", cls.rabbitmq_host),
            rabbitmq_port=int(os.getenv("RABBITMQ_PORT", str(cls.rabbitmq_port))),
            rabbitmq_user=os.getenv("RABBITMQ_USER", cls.rabbitmq_user),
            rabbitmq_password=os.getenv("RABBITMQ_PASSWORD", cls.rabbitmq_password),
            rabbitmq_vhost=os.getenv("RABBITMQ_VHOST", cls.rabbitmq_vhost),
            rabbitmq_prefetch=int(os.getenv("SCRAPER_RABBITMQ_PREFETCH", str(cls.rabbitmq_prefetch))),
            http_timeout_seconds=float(os.getenv("SCRAPER_HTTP_TIMEOUT_SECONDS", str(cls.http_timeout_seconds))),
            http_retries=int(os.getenv("SCRAPER_HTTP_RETRIES", str(cls.http_retries))),
            user_agent=os.getenv("SCRAPER_USER_AGENT", cls.user_agent),
        )
