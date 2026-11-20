from __future__ import annotations

import json
import time
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from app.config import Settings


class SourceFetchError(RuntimeError):
    pass


def fetch_json(
    url: str,
    *,
    settings: Settings | None = None,
    headers: dict[str, str] | None = None,
) -> Any:
    config = settings or Settings()
    request_headers = {
        "Accept": "application/json",
        "User-Agent": config.user_agent,
    }
    if headers:
        request_headers.update(headers)

    last_error: Exception | None = None
    for attempt in range(config.http_retries + 1):
        request = Request(url, headers=request_headers)
        try:
            with urlopen(request, timeout=config.http_timeout_seconds) as response:
                return json.loads(response.read().decode("utf-8"))
        except HTTPError as exc:
            last_error = exc
            if exc.code not in {408, 425, 429, 500, 502, 503, 504} or attempt >= config.http_retries:
                raise SourceFetchError(f"GET {url} failed with HTTP {exc.code}") from exc
        except (URLError, TimeoutError, json.JSONDecodeError) as exc:
            last_error = exc
            if attempt >= config.http_retries:
                raise SourceFetchError(f"GET {url} failed: {exc}") from exc
        time.sleep(min(2**attempt, 8))

    raise SourceFetchError(f"GET {url} failed: {last_error}")
