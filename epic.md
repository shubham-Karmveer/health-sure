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

- [ ] S1.1 Add versioned schema migrations and customer persistence with BCrypt password hashes.
- [ ] S1.2 Implement Spring Security JWT authentication using 30-minute HttpOnly cookies and derive customer identity from the authenticated principal.
- [ ] S1.3 Implement `POST /api/auth/login` and the agreed logout/session behavior.
- [ ] S1.4 Implement `GET /api/customers/me` and `GET /api/me/policies` with response DTOs.
- [ ] S1.5 Build sign-in and dashboard screens with loading, error, and no-policy states.
- [ ] S1.6 Add a safe local-only customer seed process for tests/demo; never commit real passwords.

**Exit verification**

- Unauthenticated requests cannot retrieve profile or dashboard data.
- Valid credentials show only that customer's profile and policy data.
- Invalid credentials return the standard error shape without leaking secrets.
- A customer with no policies sees an explicit empty state.
- Controller/security tests and frontend sign-in/dashboard tests pass against local PostgreSQL.

## Sprint 2 — Product discovery and policy registration

**Goal:** Let a signed-in customer discover eligible products and register a policy with a complete installment schedule.

**FR alignment:** FR-2 and FR-4.

**Tasks**

- [ ] S2.1 Add product, customer-policy, and installment migrations, entities, and repositories.
- [ ] S2.2 Add deterministic development catalog seed data with varied product terms and eligibility rules.
- [ ] S2.3 Implement `GET /api/products/available`, excluding inactive and already-registered products.
- [ ] S2.4 Implement `POST /api/policies` and service-level eligibility validation.
- [ ] S2.5 Generate policy dates, totals, and installment schedules transactionally using `BigDecimal` and the approved calendar rules.
- [ ] S2.6 Enforce unique policy number, customer/product registration, and installment sequence constraints.
- [ ] S2.7 Build product listing and registration form with server validation feedback and success confirmation.

**Exit verification**

- Product listing contains only active, not-yet-registered products for the signed-in customer.
- Invalid dates, age ineligibility, and invalid product configuration are rejected.
- A successful registration stores one policy and its complete schedule atomically.
- Duplicate registration is rejected by service logic and the database constraint.
- Premium totals, maturity, and installment dates pass boundary tests using the approved rules.
- Local UI → API → PostgreSQL flow completes and the new policy appears on the dashboard.

## Sprint 3 — Policy details and payment management

**Goal:** Let a customer review a policy schedule and record payments with consistent balances and transaction history.

**FR alignment:** FR-5; depends on policy and installment data from Sprint 2.

**Tasks**

- [ ] S3.1 Add payment and idempotency schema, including unique transaction references.
- [ ] S3.2 Implement policy detail, installment, and payment-history endpoints with ownership enforcement.
- [ ] S3.3 Implement payment validation, concurrency coordination, penalty calculation, status updates, and atomic transaction handling.
- [ ] S3.4 Implement retry-safe idempotency using the agreed request key contract.
- [ ] S3.5 Build installment selection, payment submission, updated totals, and payment history UI.

**Exit verification**

- Customers cannot view or pay another customer's policy.
- On-time, late, partial, fully paid, and repeated-payment cases match approved rules.
- Overpayments and invalid installment references are rejected.
- Retrying the same idempotency key cannot create duplicate payment records.
- A forced error in a multi-installment operation leaves no partial updates.
- Payment history, installment statuses, and policy totals reconcile against persisted records.
- The complete payment flow works locally using PostgreSQL.

## Sprint 4 — AI-assisted recommendations

**Goal:** Show persisted, explainable recommendations that reference only eligible catalog products.

**FR alignment:** FR-3.

**Tasks**

- [ ] S4.1 Add recommendation request/item migrations, entities, and repositories.
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

## Sprint 5 — Policy search and local end-to-end verification

**Goal:** Complete supported customer policy search and verify all current-release user journeys locally.

**FR alignment:** FR-6 plus regression verification for FR-1 through FR-5.

**Tasks**

- [ ] S5.1 Implement marketplace product-offer search and authenticated-customer-owned policy search with database-backed queries, validation, and pagination.
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
| FR-1 Authentication and customer dashboard | 1 | Authentication/security, profile/dashboard API, and UI tests |
| FR-2 Product discovery and registration entry point | 2 | Product filtering, registration entry point, and UI/API tests |
| FR-3 AI-assisted recommendations | 4 | Candidate validation, metadata persistence, provider failure, and UI tests |
| FR-4 Policy registration | 2 | Eligibility, schedule calculation, uniqueness, atomicity, and UI tests |
| FR-5 Premium schedule and payment management | 2–3 | Schedule, penalty, partial payment, idempotency, ownership, and transaction tests |
| FR-6 Marketplace product and own-policy search | 5 | Active offer filters, own-policy authorization, pagination, and UI tests |
| Local build, testing, and developer setup | 0–5 | Clean local start/build/test verification |

## Deferred: AWS Production Readiness

This is a separate follow-up epic after local feature implementation and verification. It will cover S3/CloudFront, Elastic Beanstalk, RDS, Bedrock runtime access, deployment automation, production secrets, network/security configuration, monitoring, backups/recovery, and production acceptance targets. Do not block local sprints on AWS account access.

## Scope Exclusions

Claim registration/assessment, document upload/analysis, an operations/agent portal, product-administration UI, and payment gateway integration are not part of this epic. Add them only through an approved scope change.
