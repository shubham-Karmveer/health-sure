# HealthDekho local development

## Prerequisites

- Node.js and npm
- Java 21 and Maven
- Docker with Docker Compose

## Start PostgreSQL

From the repository root:

```powershell
docker compose up -d postgres
```

PostgreSQL is available locally on port `5432`. The credentials in `compose.yaml` are for local development only.

## Run the backend

```powershell
mvn -f backend/pom.xml spring-boot:run
```

The API health endpoint is `http://localhost:8080/actuator/health`.

Run backend tests with PostgreSQL running:

```powershell
mvn -f backend/pom.xml test
```

To create a local demo customer, set these values in the PowerShell session before starting the backend:

```powershell
$env:SEED_CUSTOMER_ENABLED = "true"
$env:SEED_CUSTOMER_EMAIL = "demo@example.com"
$env:SEED_CUSTOMER_PASSWORD = "LocalDemoPassword123!"
mvn -f backend/pom.xml spring-boot:run
```

The backend hashes the demo password before storing it. Use only a local test password. The JWT signing secret has a local-only default; replace it through `JWT_SECRET` outside local development.

## Run the frontend

```powershell
npm --prefix frontend install
npm --prefix frontend run dev
```

Open `http://localhost:3000`. The frontend test and validation commands are:

```powershell
npm --prefix frontend test
npm --prefix frontend run lint
npm --prefix frontend run typecheck
npm --prefix frontend run build
```

No AWS account or credentials are required for local development. Do not use the local database credentials outside a developer workstation.
