from __future__ import annotations

from app.text import parse_numbers


def parse_experience_text(value: str | None) -> tuple[float | None, float | None]:
    numbers = parse_numbers(value)
    if not numbers:
        return None, None
    if len(numbers) == 1:
        return numbers[0], None
    minimum, maximum = numbers[0], numbers[1]
    if minimum > maximum:
        minimum, maximum = maximum, minimum
    return minimum, maximum
