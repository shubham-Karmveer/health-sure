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

**FR alignment:** Source FR-1; PRD FR-1.

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

**FR alignment:** Source FR-1 and FR-3; PRD FR-2 and FR-4.

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

**FR alignment:** Source FR-4; PRD FR-5. Depends on policy and installment data from Sprint 2.

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

**Progress:** Payment schedule and processing are complete: authenticated customers can review policy totals and installments, record one or more manual payments, and view transaction history. Late payments assess a one-time 10% penalty on remaining principal; collected amounts are allocated to penalty before premium. Payment requests use customer-scoped idempotency keys and row locks. The source FR-4 policy-number entry/search form is a known UI gap and is scheduled as S4.8; policy access remains owner-scoped.

**Verification:** `mvn -f backend/pom.xml test` passed (20 tests), including PostgreSQL-backed checks for ownership, on-time full and partial payments, late-payment penalty-first allocation, idempotent retries, conflicting key reuse, overpayment rejection, and no partial writes on rejected batches. Frontend tests passed (10 tests); lint, typecheck, and production build passed.

## Sprint 4 — Recommendations and discovery completion

**Goal:** Deliver the recommendation vertical slice and close the agreed product-discovery and policy-number lookup gaps, with local verification.

**FR alignment:** Source FR-2 / PRD FR-3 (recommendations); source FR-1 / PRD FR-2 plus the agreed marketplace-search addition (product discovery); source FR-4 / PRD FR-5 (policy-number lookup for payments). The multi-criteria customer policy search in source FR-5 / PRD FR-6 remains Sprint 5.

**Prerequisites:** S4.1 recommendation storage is implemented. Local development and automated tests use a deterministic provider stub and require no AWS account or credentials. Optional Bedrock mode is configured with `AI_PROVIDER=bedrock`, `SPRING_AI_MODEL_CHAT=bedrock-converse`, `AWS_REGION`, and `BEDROCK_MODEL_ID`; use the AWS SDK credential chain (local profile/SSO for development and an Elastic Beanstalk instance role after deployment). Bedrock bearer API keys are not wired into this adapter. Keep all credentials out of source control and frontend configuration.

**Tasks**

- [x] S4.1 Add recommendation request/item migrations, entities, and repositories.
- [x] S4.2 Validate recommendation inputs in the backend; prefill date of birth from the authenticated profile, reject a submitted mismatch, calculate age using Java Time, and select active, unowned products within configured entry-age limits.
- [x] S4.3 Add the Spring AI dependency, provider configuration, a provider interface, and a deterministic local provider used by tests and default local development; implement the configurable Bedrock adapter.
- [x] S4.4 Parse provider output into typed data, cap results at three, and validate every product against the eligible candidate set before persisting or returning it. Invalid/untrusted output and provider failure must produce explicit errors, never fabricated results.
- [x] S4.5 Implement authenticated `POST /api/recommendations` and owner-scoped `GET /api/recommendations/{requestId}`; persist inputs, verified calculated age, model/prompt metadata, and validated recommendation items.
- [x] S4.6 Build the dashboard recommendation entry point, input form, and tabular results with rationale, suitability, loading/error/empty states, and a same-tab registration handoff to the selected product.
- [x] S4.7 Implement authenticated `GET /api/products/search` for active, not-already-owned offers by product code, policy name, or type; validate criteria and return bounded/paginated results. Present available products and search results in the source-required table, replacing the current card presentation.
- [x] S4.8 Add a policy-number input/search state to the payment flow and connect it to the existing owner-scoped policy detail endpoint. Display payment details only for the authenticated customer's policy.
- [x] S4.9 Add focused backend and frontend tests for DOB validation, candidate eligibility, provider errors/untrusted output, persistence/ownership, product search filters/pagination, policy lookup ownership, and registration handoff.
- [x] S4.10 Run backend tests and PostgreSQL integration tests, frontend tests/lint/typecheck/build, and local smoke flows for recommendations, product search, policy lookup, and registration.

**Execution order:** Complete S4.2 before candidate generation; complete S4.3–S4.5 for the recommendation service and API before S4.6 UI integration. S4.7 product search and S4.8 policy-number lookup are separate vertical slices and may proceed independently. Add focused tests with each slice, then finish with S4.9–S4.10.

**Exit verification**

- Model candidates contain only eligible products from the database.
- A submitted date of birth that differs from the authenticated customer's profile is rejected; candidates exclude inactive, already-owned, and age-ineligible products.
- Invalid product references are not displayed or persisted as recommendation items; at most three validated recommendations are returned.
- Request inputs, calculated age, model/prompt metadata, and validated items persist and can be retrieved.
- Automated recommendation tests pass with a deterministic local stub and no AWS credentials.
- Provider timeout/failure returns an explicit error and never fabricates a successful response.
- Product search supports code/name/type, returns only available active products, and is bounded or paginated; available products and results use the source-required tabular presentation.
- The payment flow supports policy-number lookup and never reveals another customer's policy.
- Backend, frontend, PostgreSQL integration, and local smoke checks pass. A live Bedrock smoke test is optional and is not a CI or local-startup requirement.

**Progress:** S4.1–S4.9 are implemented. PostgreSQL-backed backend tests (30 total), frontend tests (19 total), lint, typecheck, production build, and frontend HTTP smoke check passed. S4.10 remains open: the frontend smoke check passed, but the backend listener was unavailable for local end-to-end API flows; endpoint behavior is covered by the PostgreSQL-backed integration tests. A live Bedrock call was not attempted; it remains optional and requires model access and credentials.

## Sprint 5 — Customer policy search and end-to-end verification

**Goal:** Implement the source brief's multi-criteria customer-owned policy search and verify all current-release user journeys locally.

**FR alignment:** Source FR-5 / PRD FR-6 (own-policy search), plus regression verification for source FR-1 through FR-4. Marketplace product search is delivered in Sprint 4 and is not part of this search scope.

**Planning notes:** The search contract in `spec.md` defines the supported criteria but does not define whether customer-name matching is exact or partial/case-insensitive. Resolve and record this before implementing the search query. The latest full backend test run also exposed PostgreSQL integration assertions that count rows across a shared database; establish a repeatable, non-destructive test baseline before using the full suite as a release gate.

**Tasks**

- [ ] S5.1 Resolve and record customer-name matching semantics in `spec.md`; make PostgreSQL integration fixtures and assertions repeatable without truncating or deleting shared developer data.
- [ ] S5.2 Implement authenticated `GET /api/policies/search` with validated query parameters and page/size handling. Support only the source-brief modes: policy identifier; customer name; policy identifier plus date of birth. Reject blank or unsupported criteria combinations. Derive the customer ID exclusively from the authenticated principal.
- [ ] S5.3 Add efficient customer-scoped repository/service queries and response DTOs. Return `PageResponse` metadata and policy identifier, customer name, date of birth, policy name/type, start/maturity dates, total premium payable, payment status, total premium paid, and remaining premium. Keep totals consistent with the existing payment-detail calculations and avoid per-row follow-up queries.
- [ ] S5.4 Add the customer policy-search form to the signed-in experience, with policy identifier, customer name, and date-of-birth inputs; connect it to the API client and display paginated table results, validation errors, loading state, and an explicit no-results state.
- [ ] S5.5 Add focused repository/service/controller and frontend tests for all supported criteria modes, unsupported/empty criteria, normalization, pagination and page-size bounds, response fields, and cross-customer isolation.
- [ ] S5.6 Run local end-to-end flows for sign-in, dashboard, product search, registration, recommendations, policy-number lookup, payments, and multi-criteria policy search. Verify search results do not expose another customer's data.
- [ ] S5.7 Run the backend suite against an isolated/repeatable PostgreSQL test setup, frontend tests, lint, typecheck, and production build; fix integration issues and update local run/test instructions.

**Search acceptance matrix**

| Input criteria | Expected behavior |
|---|---|
| Policy identifier | Return exact matching policy records owned by the authenticated customer |
| Customer name | Apply the agreed name-matching rule and return only the authenticated customer's matching policies |
| Policy identifier and date of birth | Require both values to match, while still restricting results to the authenticated customer |
| Empty input, date of birth alone, or unsupported combinations | Return a clear validation error |
| No matching owned policy | Return an empty page and show a clear no-results message |

**Exit verification**

- Each supported search mode returns only matching, authorized policies.
- Invalid/insufficient criteria are rejected and large results are bounded or paginated.
- The result table includes all policy, customer, date, and payment fields required by source FR-5, with totals consistent with the payment view.
- Pagination is bounded consistently with the existing product-search API, and repeated test runs do not rely on or destroy shared database state.
- All current-release user journeys pass locally against PostgreSQL.
- Frontend build, backend build, automated tests, and clean-start smoke checks pass.
- No AWS deployment or production readiness claim is made by this sprint.

**Execution order:** Resolve the name-matching rule and stabilize the PostgreSQL test baseline in S5.1. Complete the authenticated API and owner-scoped query in S5.2–S5.3 before building the UI in S5.4. Add focused tests with each layer, then complete the search acceptance matrix and full regression flow in S5.5–S5.7.

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
| FR-1 Authentication and customer dashboard (source FR-1) | 1 | Completed: JWT-cookie auth, own-customer profile/policy APIs, seed, and UI/API verification |
| FR-2 Product discovery and registration entry point (source FR-1) | 2, 4 | Core catalog/registration implemented in S2; S4 aligns the offer table and adds active product search |
| FR-3 AI-assisted recommendations (source FR-2) | 4 | Candidate eligibility, verified DOB, metadata persistence, provider failure handling, and UI tests |
| FR-4 Policy registration (source FR-3) | 2 | Completed: age/date validation, transactional schedule generation, uniqueness, and UI/API tests |
| FR-5 Premium schedule and payment management (source FR-4) | 2–4 | Payment core implemented in S3; S4 adds the source-required policy-number lookup UI |
| FR-6 Customer insurance policy search (source FR-5) | 5 | Source search modes, own-customer authorization, pagination, result table, and UI tests |
| Agreed marketplace product search (addition to source FR-1 discovery) | 4 | Active, available-offer filters by code/name/type, bounded results, and table UI |
| Local build, testing, and developer setup | 0–5 | Clean local start/build/test verification |

## Deferred: AWS Production Readiness

This is a separate follow-up epic after local feature implementation and verification. It will cover S3/CloudFront, Elastic Beanstalk, RDS, Bedrock runtime access, deployment automation, production secrets, network/security configuration, monitoring, backups/recovery, and production acceptance targets. Do not block local sprints on AWS account access.

## Scope Exclusions

Claim registration/assessment, document upload/analysis, an operations/agent portal, product-administration UI, and payment gateway integration are not part of this epic. Add them only through an approved scope change.
