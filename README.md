# Pune Startup Arena

Job board and startup discovery platform for Pune. The stack is a Spring Boot API, a React/Vite frontend, a Python job scraper, PostgreSQL with PostGIS, Redis, and RabbitMQ.

## Repository layout

| Path | Contents |
| --- | --- |
| `backend/` | Spring Boot 3.5 / Java 21 API, Flyway migrations, integration tests |
| `frontend/` | React 19 / TypeScript / Vite 6 single-page app |
| `scraper/` | Python 3.12 scraper/worker that publishes discovered jobs to RabbitMQ |
| `ai/` | Placeholder package for future matching/parsing work |
| `.github/workflows/ci.yml` | Build and test pipeline for all three components |
| `docker-compose.yml`, `docker-compose.prod.yml` | Local services and production stack |
| `.env.example` | Template for all required environment variables |

## Prerequisites

- Docker with Compose
- Java 21 and Maven
- Node.js 22 and npm
- Python 3.12

## Local development

```powershell
Copy-Item .env.example .env
docker compose up -d
```

Start the API from `backend/` (Flyway applies migrations on startup):

```powershell
cd backend
mvn spring-boot:run
```

Start the frontend in a second terminal from `frontend/`:

```powershell
cd frontend
npm ci
npm run dev
```

Start the scraper manually from `scraper/` when needed:

```powershell
cd scraper
python -m venv .venv
.\.venv\Scripts\python -m pip install -r requirements.txt
.\.venv\Scripts\python -m app.main
```

Use `python -m app.main`, not `python app/main.py`.

### Local endpoints

| Service | URL |
| --- | --- |
| Frontend | http://localhost:5173 |
| API base | http://localhost:8080/api/v1 |
| Health check | http://localhost:8080/actuator/health |
| RabbitMQ management | http://localhost:15672 |

The Vite dev server proxies `/api` to `http://localhost:8080`. Credentials for PostgreSQL, Redis, RabbitMQ, and the admin API key come from `.env`; never commit `.env`.

## API overview

The API exposes public endpoints for browsing jobs, companies, industries, skills, and map markers; authenticated candidate endpoints under `/api/v1/candidates/me` for profiles, saved jobs, followed companies, applications, job matches, and resumes; auth endpoints under `/api/v1/auth`; and admin endpoints under `/api/v1/admin` protected by `ADMIN_API_KEY`.

## Tests

```powershell
cd backend; mvn test
cd frontend; npm run typecheck; npm run build
cd scraper; python -m pytest -q
```

The same commands run in CI on pushes and pull requests to `main`.

## Production

Set the production environment variables, then from the repository root:

```powershell
docker compose -f docker-compose.prod.yml up -d --build
```

Rotate all placeholder secrets from `.env.example` before exposing the stack publicly.
