from __future__ import annotations

from app.config import Settings


def test_settings_reads_rabbitmq_ssl_flag(monkeypatch) -> None:
    monkeypatch.delenv("SCRAPER_REQUIRE_ENV", raising=False)
    monkeypatch.setenv("RABBITMQ_SSL_ENABLED", "true")
    assert Settings.from_env().rabbitmq_ssl is True

    monkeypatch.setenv("RABBITMQ_SSL_ENABLED", "false")
    assert Settings.from_env().rabbitmq_ssl is False
