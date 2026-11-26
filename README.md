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

Local development and automated tests use a deterministic provider stub by default and do not require AWS credentials or model access. The Bedrock recommendation provider calls Converse with native JSON Schema structured output, while continuing to validate every returned product against the eligible catalog.

For an optional live Bedrock smoke test, set these backend environment variables in the backend process:

```powershell
$env:AI_PROVIDER = "bedrock"
$env:AWS_REGION = "us-east-1"
$env:BEDROCK_MODEL_ID = "us.anthropic.claude-haiku-4-5-20251001-v1:0"
$env:BEDROCK_MAX_TOKENS = "1024"
mvn -f backend/pom.xml spring-boot:run
```

The example model ID uses US cross-region inference routing. Confirm the inference profile is available in your source region, submit Anthropic's one-time use-case form if prompted, and grant `bedrock:InvokeModel` for both the inference profile and its foundation model ARN. The first call with the schema may take up to 90 seconds while Bedrock compiles it. The provider uses the AWS SDK credential chain: use a local AWS profile/SSO session for development and an Elastic Beanstalk instance role after deployment. It does not configure Bedrock bearer API keys. Never place credentials in source control or frontend configuration. The production deployment remains deferred.

Signed-in customers can search active available products by code, name, or type; request catalog-grounded recommendations; open their own policies by policy number; review installments; record manual payments; and see payment history. Payment recording is local-only accounting; it does not connect to a payment gateway.

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
