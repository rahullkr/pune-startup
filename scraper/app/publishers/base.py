from __future__ import annotations

from typing import Any, Mapping, Protocol


class Publisher(Protocol):
    def publish(self, routing_key: str, payload: Mapping[str, Any]) -> None:
        ...
