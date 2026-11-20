from __future__ import annotations

import re
from dataclasses import dataclass

RANGE_PATTERN = re.compile(
    r"(?:[₹$€£]\s*)?(?P<min>\d[\d,]*(?:\.\d+)?)\s*(?P<min_unit>k|lpa|lakh|lakhs)?"
    r"\s*(?:-|–|—|to)\s*"
    r"(?:[₹$€£]\s*)?(?P<max>\d[\d,]*(?:\.\d+)?)\s*(?P<max_unit>k|lpa|lakh|lakhs)?",
    re.IGNORECASE,
)

CURRENCY_MARKERS = (
    ("INR", ("inr", "₹", "rs.", "rs ", "rupee")),
    ("USD", ("usd", "$")),
    ("EUR", ("eur", "€")),
    ("GBP", ("gbp", "£")),
)

PERIOD_MARKERS = (
    ("YEAR", ("per year", "per annum", "annual", "yearly", "lpa", "lakh per annum", "a year")),
    ("MONTH", ("per month", "monthly", "a month", "pm")),
    ("HOUR", ("per hour", "hourly", "an hour")),
)


@dataclass(frozen=True)
class Salary:
    minimum: float
    maximum: float
    currency: str
    period: str


def _multiplier(unit: str | None, text: str) -> float:
    lowered = f"{unit or ''} {text}".casefold()
    if "lpa" in lowered or "lakh" in lowered:
        return 100_000
    if (unit or "").casefold() == "k":
        return 1_000
    return 1


def _currency(text: str) -> str | None:
    lowered = text.casefold()
    for code, markers in CURRENCY_MARKERS:
        if any(marker in lowered for marker in markers):
            return code
    if "lpa" in lowered or "lakh" in lowered:
        return "INR"
    return None


def _period(text: str) -> str | None:
    lowered = text.casefold()
    for period, markers in PERIOD_MARKERS:
        if any(marker in lowered for marker in markers):
            return period
    return None


def parse_salary_text(value: str | None) -> Salary | None:
    if not value:
        return None
    match = RANGE_PATTERN.search(value)
    if not match:
        return None
    minimum = float(match.group("min").replace(",", "")) * _multiplier(match.group("min_unit"), value)
    maximum = float(match.group("max").replace(",", "")) * _multiplier(match.group("max_unit"), value)
    currency = _currency(value)
    period = _period(value)
    return build_salary(minimum, maximum, currency, period)


def parse_salary_values(
    minimum: object,
    maximum: object,
    currency: str | None,
    interval: str | None,
) -> Salary | None:
    try:
        minimum_value = float(minimum)  # type: ignore[arg-type]
        maximum_value = float(maximum)  # type: ignore[arg-type]
    except (TypeError, ValueError):
        return None
    return build_salary(minimum_value, maximum_value, currency, interval_period(interval))


def interval_period(value: str | None) -> str | None:
    if not value:
        return None
    lowered = value.casefold()
    if "year" in lowered or "annual" in lowered:
        return "YEAR"
    if "month" in lowered:
        return "MONTH"
    if "hour" in lowered:
        return "HOUR"
    if "day" in lowered:
        return "DAY"
    return None


def build_salary(minimum: float, maximum: float, currency: str | None, period: str | None) -> Salary | None:
    if minimum < 0 or maximum < 0 or minimum > maximum:
        return None
    if not currency or len(currency) != 3 or not period:
        return None
    return Salary(minimum, maximum, currency.upper(), period.upper())
