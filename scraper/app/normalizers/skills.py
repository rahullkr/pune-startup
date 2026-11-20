from __future__ import annotations

import re

SKILL_ALIASES: dict[str, str] = {
    "python": "Python",
    "java": "Java",
    "kotlin": "Kotlin",
    "javascript": "JavaScript",
    "js": "JavaScript",
    "typescript": "TypeScript",
    "ts": "TypeScript",
    "react": "React",
    "react.js": "React",
    "reactjs": "React",
    "next.js": "Next.js",
    "nextjs": "Next.js",
    "node.js": "Node.js",
    "nodejs": "Node.js",
    "angular": "Angular",
    "vue": "Vue.js",
    "vue.js": "Vue.js",
    "spring": "Spring",
    "spring boot": "Spring Boot",
    "django": "Django",
    "flask": "Flask",
    "fastapi": "FastAPI",
    "postgresql": "PostgreSQL",
    "postgres": "PostgreSQL",
    "mysql": "MySQL",
    "mongodb": "MongoDB",
    "redis": "Redis",
    "elasticsearch": "Elasticsearch",
    "kafka": "Kafka",
    "rabbitmq": "RabbitMQ",
    "docker": "Docker",
    "kubernetes": "Kubernetes",
    "aws": "AWS",
    "azure": "Azure",
    "gcp": "GCP",
    "git": "Git",
    "linux": "Linux",
    "graphql": "GraphQL",
    "rest": "REST",
    "html": "HTML",
    "css": "CSS",
    "tailwind": "Tailwind CSS",
    "figma": "Figma",
    "sql": "SQL",
    "excel": "Excel",
    "machine learning": "Machine Learning",
    "data analysis": "Data Analysis",
    "product management": "Product Management",
    "digital marketing": "Digital Marketing",
    "seo": "SEO",
}

_CANONICAL_BY_KEY = {key.casefold(): value for key, value in SKILL_ALIASES.items()}
_PATTERNS = [
    (re.compile(r"(?<![a-z0-9])" + re.escape(alias) + r"(?![a-z0-9])", re.IGNORECASE), canonical)
    for alias, canonical in sorted(SKILL_ALIASES.items(), key=lambda item: len(item[0]), reverse=True)
]


def split_skills(value: str | None) -> list[str]:
    if not value:
        return []
    return [part.strip() for part in re.split(r"[,;|]", value) if part.strip()]


def canonical_skill(value: str) -> str:
    cleaned = value.strip()
    return _CANONICAL_BY_KEY.get(cleaned.casefold(), cleaned)


def extract_skills(explicit: list[str] | tuple[str, ...], text: str | None) -> list[str]:
    found: dict[str, str] = {}
    for value in explicit:
        canonical = canonical_skill(value)
        if canonical:
            found.setdefault(canonical.casefold(), canonical)

    haystack = text or ""
    for pattern, canonical in _PATTERNS:
        if pattern.search(haystack):
            found.setdefault(canonical.casefold(), canonical)
    return sorted(found.values(), key=str.casefold)
