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

On backend startup, the local catalog seeder inserts the three active insurance products from the source brief and one inactive product used to verify catalog filtering. Seeding is idempotent and can be disabled by setting `SEED_CATALOG_ENABLED=false`.

## Run the backend

```powershell
mvn -f backend/pom.xml spring-boot:run
```

The API health endpoint is `http://localhost:8080/actuator/health`.

Run backend tests with PostgreSQL running:

```powershell
mvn -f backend/pom.xml test
```

To seed the three sample customer profiles from the source brief plus an optional local demo account, set these values in the PowerShell session before starting the backend:

```powershell
$env:SEED_CUSTOMER_ENABLED = "true"
$env:SEED_CUSTOMER_EMAIL = "aarav.sharma@example.com"
$env:SEED_CUSTOMER_PASSWORD = "LocalDemoPassword123!"
mvn -f backend/pom.xml spring-boot:run
```

The configured password is BCrypt-hashed for any newly created source sample accounts (Aarav, Meera, and Rohan); an email outside those profiles also creates a local demo account. Existing accounts are not overwritten. The sample accounts are seeded only when `SEED_CUSTOMER_ENABLED=true`; use only a local test password. The source brief's password hashes are placeholders and are not used. The JWT signing secret has a local-only default; replace it through `JWT_SECRET` outside local development.

The sample catalog is intended only for local development. Disable it explicitly in any non-local environment until production catalog management is configured.

## Sprint 4 Bedrock access

The recommendation interface, validation, and automated tests can be developed with a deterministic local provider stub; they do not require AWS credentials or an API key. The backend does not yet include Spring AI or a Bedrock adapter; that dependency is added as part of Sprint 4.

For an optional live Bedrock smoke test, prepare:

- An AWS account and region where the selected Bedrock model is available, with model access enabled.
- The selected Bedrock model ID.
- An AWS identity allowed to invoke that model (`bedrock:InvokeModel`).
- Local AWS SSO/profile credentials, or an Elastic Beanstalk instance role for a deployed environment.

Amazon Bedrock uses AWS IAM credentials rather than a separate Bedrock API key. Do not commit access keys or place them in frontend configuration. The production deployment remains deferred.

Signed-in customers can open a policy's payment view to review installments, record manual payments, and see payment history. Payment recording is local-only accounting; it does not connect to a payment gateway.

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

No AWS account or credentials are required for local development or automated tests. Do not use the local database credentials outside a developer workstation.
