# HealthDekho Local Implementation Epic

## Epic goal

Build and verify the HealthDekho application locally in small, working increments, with each sprint aligned to the functional requirements (FRs) in [PRD.md](./PRD.md) and technical rules in [spec.md](./spec.md). AWS production deployment and production hardening are explicitly deferred to a separate follow-up epic.

The target code layout and eventual AWS services are described in [architecture-spine.md](./architecture-spine.md). During this epic, the application must run on a developer workstation using Next.js, Spring Boot, and PostgreSQL. Do not require AWS credentials or an AWS environment for normal development or automated tests.

## Delivery approach

- Suggested sprint length: 1–2 weeks; size work to team capacity.
- Each sprint is a vertical slice and ends with a local build, automated tests, and a working user flow.
- Keep both applications runnable from a clean checkout and keep `main` buildable.
- Use PostgreSQL locally and in CI. A local Docker Compose database is the default; AWS RDS is not required.
- Keep provider integrations behind interfaces. Automated tests use controlled stubs; local Bedrock access is optional and must not be needed to run tests.
- Use seeded development product data; product administration is not in this release.
- Record decisions before implementing behavior affected by unresolved questions in `spec.md`.
- Do not provision S3, CloudFront, Elastic Beanstalk, RDS, or Bedrock resources as part of these sprints.

## Sprint 0 — Local foundation and implementation decisions

**Goal:** Establish repeatable local frontend/backend builds and resolve decisions that would otherwise make feature behavior inconsistent.

**FR alignment:** Foundation for FR-1 through FR-6; no customer-facing FR is considered complete in this sprint.

**Tasks**

- [x] S0.1 Decide and document authentication/token approach and customer identity mapping.
- [x] S0.2 Decide which policy search modes customers may use and how search authorization is enforced; do not expose another customer's policy data.
- [x] S0.3 Confirm schedule dates, maturity-date convention, duplicate-registration scope, payment-recording scope, late-penalty/partial-payment rules, and idempotency contract.
- [x] S0.4 Confirm recommendation count and provider-unavailable behavior; retain local deterministic testing without AWS credentials.
- [x] S0.5 Create `frontend/` (Next.js App Router) and `backend/` projects according to `architecture-spine.md`, with build and test commands.
- [x] S0.6 Add Docker Compose PostgreSQL and non-secret local configuration examples.
- [x] S0.7 Add a minimal Next.js landing page, backend health endpoint, and documented local build/test commands.

**Exit verification**

- Frontend and backend build and test from a clean checkout.
- Next.js starts locally; Spring Boot starts locally and its health endpoint responds.
- PostgreSQL starts through Docker Compose and the backend can connect using local environment configuration.
- Accepted decisions are recorded in `spec.md` and used consistently by later sprint work.
- No AWS credential or AWS resource is needed for local startup or tests.

**Progress:** S0.1–S0.7 are implemented or documented. All current local behavior decisions are recorded in `spec.md`; production non-functional targets remain deferred.

## Sprint 1 — Authentication and customer dashboard

**Goal:** Let a customer sign in locally and see only their own profile and policies.

**FR alignment:** FR-1.

**Tasks**

- [x] S1.1 Add versioned schema migrations and customer persistence with BCrypt password hashes.
- [x] S1.2 Implement Spring Security JWT authentication using 30-minute HttpOnly cookies and derive customer identity from the authenticated principal.
- [x] S1.3 Implement `POST /api/auth/login` and the agreed logout/session behavior.
- [x] S1.4 Implement `GET /api/customers/me` and `GET /api/me/policies` with response DTOs.
- [x] S1.5 Build sign-in and dashboard screens with loading, error, and no-policy states.
- [x] S1.6 Add a safe local-only customer seed process for tests/demo; never commit real passwords.

**Exit verification**

- Unauthenticated requests cannot retrieve profile or dashboard data.
- Valid credentials show only that customer's profile and policy data.
- Invalid credentials return the standard error shape without leaking secrets.
- A customer with no policies sees an explicit empty state.
- Controller/security tests and frontend sign-in/dashboard tests pass against local PostgreSQL.

**Progress:** Complete. Sprint 1 implements the local sign-in and customer dashboard flow; customer identity is derived from the JWT subject, policy queries are scoped to that customer, and the optional demo seed stores a BCrypt hash without replacing existing accounts.

**Verification:** `mvn -f backend/pom.xml test` passed (9 tests); `npm --prefix frontend test`, `npm --prefix frontend run lint`, `npm --prefix frontend run typecheck`, and `npm --prefix frontend run build` passed (6 frontend tests). A local HTTP smoke test returned frontend HTTP 200 and verified seeded-customer login, profile retrieval, an empty policy list, and logout against PostgreSQL.

## Sprint 2 — Product discovery and policy registration

**Goal:** Let a signed-in customer discover eligible products and register a policy with a complete installment schedule.

**FR alignment:** FR-2 and FR-4.

**Tasks**

- [x] S2.1 Add product, customer-policy, and installment migrations, entities, and repositories.
- [x] S2.2 Add deterministic development catalog seed data with varied product terms and eligibility rules.
- [x] S2.3 Implement `GET /api/products/available`, excluding inactive and already-registered products.
- [x] S2.4 Implement `POST /api/policies` and service-level eligibility validation.
- [x] S2.5 Generate policy dates, totals, and installment schedules transactionally using `BigDecimal` and the approved calendar rules.
- [x] S2.6 Enforce unique policy number, customer/product registration, and installment sequence constraints.
- [x] S2.7 Build product listing and registration form with server validation feedback and success confirmation.

**Exit verification**

- Product listing contains only active, not-yet-registered products for the signed-in customer.
- Invalid dates, age ineligibility, and invalid product configuration are rejected.
- A successful registration stores one policy and its complete schedule atomically.
- Duplicate registration is rejected by service logic and the database constraint.
- Premium totals, maturity, and installment dates pass boundary tests using the approved rules.
- Local UI → API → PostgreSQL flow completes and the new policy appears on the dashboard.

**Progress:** Complete. The local PostgreSQL catalog seeds the three products and eligibility values from the source brief, plus an inactive sample excluded from discovery. Optional customer seeding provides the source brief's three sample customer profiles with BCrypt hashes using a local-only configured password. Authenticated customers can register an eligible product; policy and all installments persist together, and duplicate registration is blocked.

**Verification:** `mvn -f backend/pom.xml test` passed (16 tests), including PostgreSQL integration checks for catalog filtering, eligibility, duplicate registration, database schedule constraints, monthly/quarterly schedules, maturity, and grace dates. `npm --prefix frontend test` passed (9 tests), and frontend lint, typecheck, and production build passed.

## Sprint 3 — Policy details and payment management

**Goal:** Let a customer review a policy schedule and record payments with consistent balances and transaction history.

**FR alignment:** FR-5; depends on policy and installment data from Sprint 2.

**Tasks**

- [x] S3.1 Add payment and idempotency schema, including unique transaction references.
- [x] S3.2 Implement policy detail, installment, and payment-history endpoints with ownership enforcement.
- [x] S3.3 Implement payment validation, concurrency coordination, penalty calculation, status updates, and atomic transaction handling.
- [x] S3.4 Implement retry-safe idempotency using the agreed request key contract.
- [x] S3.5 Build installment selection, payment submission, updated totals, and payment history UI.

**Exit verification**

- Customers cannot view or pay another customer's policy.
- On-time, late, partial, fully paid, and repeated-payment cases match approved rules.
- Overpayments and invalid installment references are rejected.
- Retrying the same idempotency key cannot create duplicate payment records.
- A rejected multi-installment operation leaves no partial updates.
- Payment history, installment statuses, and policy totals reconcile against persisted records.
- The complete payment flow works locally using PostgreSQL.

**Progress:** Complete. Authenticated customers can review their policy totals and installment schedule, record one or more manual payments, and view transaction history. Late payments assess a one-time 10% penalty on remaining principal; collected amounts are allocated to penalty before premium. Payment requests use customer-scoped idempotency keys and row locks.

**Verification:** `mvn -f backend/pom.xml test` passed (20 tests), including PostgreSQL-backed checks for ownership, on-time full and partial payments, late-payment penalty-first allocation, idempotent retries, conflicting key reuse, overpayment rejection, and no partial writes on rejected batches. Frontend tests passed (10 tests); lint, typecheck, and production build passed.

## Sprint 4 — AI-assisted recommendations

**Goal:** Show persisted, explainable recommendations that reference only eligible catalog products.

**FR alignment:** FR-3.

**Prerequisites:** Local implementation and automated verification require no AWS account, API key, or Bedrock credentials because they use a deterministic provider stub. A manual Bedrock smoke test is optional and requires a selected model ID, a supported AWS region with model access enabled, and an AWS IAM identity with `bedrock:InvokeModel`. Use a local AWS profile/SSO session during development and an instance role after deployment; Bedrock does not use a separate API key. Add the Spring AI dependency and Bedrock adapter during S4.3; they are not yet present in the backend.

**Tasks**

- [x] S4.1 Add recommendation request/item migrations, entities, and repositories.
- [ ] S4.2 Implement customer preference validation, age calculation, and eligible-candidate selection.
- [ ] S4.3 Implement a recommendation provider interface, deterministic local test stub, and Bedrock adapter.
- [ ] S4.4 Parse provider output into typed data and reject unknown, inactive, or ineligible product references.
- [ ] S4.5 Implement `POST /api/recommendations` and `GET /api/recommendations/{requestId}` with metadata persistence.
- [ ] S4.6 Build recommendation form/results and a handoff to the existing registration flow.

**Exit verification**

- Model candidates contain only eligible products from the database.
- Invalid product references are not displayed or persisted as recommendation items.
- Request inputs, calculated age, model/prompt metadata, and validated items persist and can be retrieved.
- Automated recommendation tests pass with a deterministic local stub and no AWS credentials.
- Provider timeout/failure returns an explicit error and never fabricates a successful response.
- When explicitly configured with valid local AWS credentials, a manual Bedrock smoke test can be run; it is not a CI or local startup requirement.

**Progress:** S4.1 data-model foundation was implemented during pre-Sprint-4 requirement alignment: PostgreSQL tables, audit and lookup indexes, JPA entities, and repositories are present. Recommendation generation, provider integration, endpoints, and UI remain pending.

## Sprint 5 — Policy search and local end-to-end verification

**Goal:** Complete supported customer policy search and verify all current-release user journeys locally.

**FR alignment:** FR-6 plus regression verification for FR-1 through FR-5.

**Tasks**

- [ ] S5.1 Implement marketplace product-offer search and authenticated-customer-owned policy search with database-backed queries, validation, and pagination. Own-policy criteria must support policy identifier, customer name, and policy identifier plus date of birth, without exposing other customers' records.
- [ ] S5.2 Build search form, paginated results, error display, and no-results state.
- [ ] S5.3 Add tests for each supported search mode and confirm unauthorized records are never returned.
- [ ] S5.4 Run backend unit/service/controller tests, PostgreSQL integration tests, and frontend tests.
- [ ] S5.5 Run local end-to-end flows for sign-in, dashboard, discovery, registration, recommendations, payments, and search.
- [ ] S5.6 Fix local integration issues and document the commands required to run the application and tests.

**Exit verification**

- Each supported search mode returns only matching, authorized policies.
- Invalid/insufficient criteria are rejected and large results are bounded or paginated.
- All current-release user journeys pass locally against PostgreSQL.
- Frontend build, backend build, automated tests, and clean-start smoke checks pass.
- No AWS deployment or production readiness claim is made by this sprint.

## Definition of Done — Every Sprint

- Sprint acceptance criteria pass.
- Relevant automated tests are added and pass; database behavior is tested with PostgreSQL.
- Both frontend and backend build from a clean checkout.
- Database changes use versioned migrations.
- Authorization, validation, error responses, and UI loading/empty/error states are tested where relevant.
- No real credentials or sensitive customer data are committed or written to logs.
- Updated local setup/run instructions are documented.
- The integrated feature is runnable locally before the next sprint begins.

## Requirement Traceability

| PRD requirement | Sprint | Completion evidence |
|---|---:|---|
| FR-1 Authentication and customer dashboard | 1 | Completed: JWT-cookie auth, own-customer profile/policy APIs, seed, and UI/API verification |
| FR-2 Product discovery and registration entry point | 2 | Completed: seeded catalog filtering, registration entry point, and UI/API tests |
| FR-3 AI-assisted recommendations | 4 | Candidate validation, metadata persistence, provider failure, and UI tests |
| FR-4 Policy registration | 2 | Completed: age/date validation, transactional schedule generation, uniqueness, and UI/API tests |
| FR-5 Premium schedule and payment management | 2–3 | Schedule, penalty, partial payment, idempotency, ownership, and transaction tests |
| FR-6 Marketplace product and own-policy search | 5 | Active offer filters, own-policy authorization, pagination, and UI tests |
| Local build, testing, and developer setup | 0–5 | Clean local start/build/test verification |

## Deferred: AWS Production Readiness

This is a separate follow-up epic after local feature implementation and verification. It will cover S3/CloudFront, Elastic Beanstalk, RDS, Bedrock runtime access, deployment automation, production secrets, network/security configuration, monitoring, backups/recovery, and production acceptance targets. Do not block local sprints on AWS account access.

## Scope Exclusions

Claim registration/assessment, document upload/analysis, an operations/agent portal, product-administration UI, and payment gateway integration are not part of this epic. Add them only through an approved scope change.
