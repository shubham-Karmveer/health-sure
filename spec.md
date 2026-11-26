# HealthDekho — Technical Specification

## 1. Purpose

This document translates the HealthDekho product requirements into an implementation design for the web application. It describes the system architecture, data model, API contracts, validation and processing rules, user-interface behavior, and verification approach.

## 2. System architecture

### 2.1 Technology

- **Backend:** Java 21, Spring Boot 3.5.7, Spring Web, Spring Security, Spring Data JPA, Bean Validation, Spring AI.
- **Database:** PostgreSQL.
- **Frontend:** React functional components and hooks, with Axios for REST communication.
- **Money and dates:** `BigDecimal` for monetary values; `LocalDate` and Java Time API for calendar dates; `YearMonth` where a premium period is represented as a month.
- **Java practices:** records for request/response DTOs; `Optional` for repository lookups; Stream API and collectors for filtering and entity-to-DTO mapping; switch expressions for payment-status handling where suitable.

### 2.2 Backend layers

1. **Controller:** REST routing, request/response DTOs, authentication context access, and HTTP status mapping. Must not contain business calculations.
2. **Service:** business rules, validation coordination, transaction boundaries, and orchestration of repository and AI operations.
3. **Repository:** persistence and query operations using Spring Data JPA.
4. **Entity:** persistence model and relationships. JPA entities must not be serialized as API responses.
5. **DTO:** immutable request/response contracts, preferably Java records.
6. **AI integration:** prompt construction, model invocation, response parsing, candidate validation, and provider metadata capture.

### 2.3 Frontend areas

- Authentication form.
- Customer dashboard and policy list.
- Available-product listing and registration form.
- Recommendation request form and recommendation results.
- Policy detail and installment/payment view.
- Customer policy search and results.

Each screen should expose loading, empty, success, and error states appropriate to its operation. Server validation remains authoritative.

## 3. Data model

Use normalized relational tables, UUID identifiers consistently, explicit foreign keys, uniqueness constraints, and indexes for frequently searched columns.

### 3.1 Customer

| Field | Type/constraint | Notes |
|---|---|---|
| customer_id | PK | Identifier |
| email | unique, not null | Authentication identity |
| password_hash | not null | BCrypt hash; never return in API |
| full_name | not null | Customer display name |
| date_of_birth | date, not null | Must not be in the future |
| phone | nullable/configured | Contact information |
| status | not null, indexed | Account state |
| created_at, updated_at | timestamp with time zone | Audit timestamps |

### 3.2 Insurance product

| Field | Type/constraint | Notes |
|---|---|---|
| product_id | PK | Identifier |
| product_code | unique, not null | Public product code |
| policy_name, policy_type | not null | Display and category |
| term_months | positive integer | Product term |
| premium_frequency | enum/value | Monthly, quarterly, half-yearly, yearly |
| premium_amount | NUMERIC(19,2), positive | Amount per installment |
| sum_insured | NUMERIC(19,2), positive | Coverage amount |
| min_entry_age, max_entry_age | integer | Registration eligibility |
| active | boolean, indexed | Discovery eligibility |

### 3.3 Customer policy

| Field | Type/constraint | Notes |
|---|---|---|
| policy_id | PK | Identifier |
| policy_number | unique, not null | Generated policy identifier |
| customer_id | FK, indexed | Policy owner |
| product_id | FK | Registered product |
| term_months | positive integer | Registration-time policy term |
| start_date, maturity_date | date | Policy period |
| premium_frequency | enum/value | Registration-time product value |
| premium_amount, sum_insured | NUMERIC(19,2) | Registration-time financial values |
| total_premium_payable | NUMERIC(19,2) | Sum payable over the term |
| status | indexed | Policy state |
| created_at | timestamp with time zone | Audit timestamp |

Enforce `UNIQUE(customer_id, product_id)` as described in the product requirements. Preserve policy financial terms on the policy record so later catalog changes do not silently change an existing policy.

### 3.4 Premium installment

| Field | Type/constraint | Notes |
|---|---|---|
| installment_id | PK | Identifier |
| policy_id | FK | Parent policy |
| installment_number | positive integer | Sequence |
| due_date, grace_period_end_date | date | Payment timing |
| scheduled_amount, paid_amount | NUMERIC(19,2) | Scheduled amount and paid premium principal |
| penalty_amount, paid_penalty_amount | NUMERIC(19,2) | Assessed and paid penalty totals |
| payment_status | enum/value | `PENDING`, `PARTIALLY_PAID`, `PAID`, or `OVERDUE` |

Enforce `UNIQUE(policy_id, installment_number)`. Keep payment transactions separate from installment aggregates.

The database design in the assessment also calls for `paid_amount` to remain within zero and the scheduled amount plus any applicable penalty. Enforce the corresponding invariant in service logic and with a database constraint where the chosen representation permits it.

### 3.5 Premium payment

| Field | Type/constraint | Notes |
|---|---|---|
| payment_id | PK | Identifier |
| installment_id | FK, indexed | Paid installment |
| transaction_reference | unique, not null | Payment transaction identifier |
| payment_date | date/timestamp | Recorded payment time |
| amount_paid, premium_amount, penalty_amount | NUMERIC(19,2) | Total collected and its premium/penalty allocations |
| payment_method | value | Method recorded for the payment |
| created_at | timestamp with time zone | Audit timestamp |
| idempotency_id | FK | Idempotency record for the payment batch |

Payments are append-only transaction history. Balance and status updates must be consistent with persisted payment records.

Enforce `amount_paid > 0`, `amount_paid = premium_amount + penalty_amount`, and a unique transaction reference; index installment and payment date for history lookups. A payment batch is recorded in `payment_idempotency`, keyed by customer and idempotency key, with the request hash and original response for exact retries.

### 3.6 Payment idempotency

| Field | Type/constraint | Notes |
|---|---|---|
| idempotency_id | PK | Identifier |
| customer_id, policy_id | FK | Owner and policy for the request |
| idempotency_key | unique with customer_id | Client-supplied retry key |
| request_hash | not null | Detects incompatible key reuse |
| response_json | not null | Original response returned for an exact retry |
| created_at | timestamp with time zone | Audit timestamp |

### 3.7 Recommendation request

| Field | Type/constraint | Notes |
|---|---|---|
| recommendation_request_id | PK | Identifier |
| customer_id | FK, indexed with requested_at | Authenticated customer |
| insurance_goals | nullable text | Customer preference |
| preferred_term | nullable string | Customer preference |
| preferred_policy_type | nullable string | Customer preference |
| calculated_age | non-negative integer | Calculated from the customer's profile date of birth |
| model_name, model_version, prompt_version | not null strings | Reproducibility/audit metadata |
| requested_at | timestamp with time zone | Request timestamp |

### 3.8 Recommendation item

| Field | Type/constraint | Notes |
|---|---|---|
| recommendation_item_id | PK | Identifier |
| recommendation_request_id | FK | Parent recommendation request |
| product_id | FK | Catalog product, validated as eligible before persistence |
| rank_order | positive integer, unique per request | Display order |
| recommendation_reason | text, not null | Advisory explanation |
| suitability | not null string | Advisory suitability result |
| selected_for_registration | boolean, not null | Whether the customer selected the item |

Enforce `UNIQUE(recommendation_request_id, product_id)` and `UNIQUE(recommendation_request_id, rank_order)`.

## 4. API design

All endpoints use JSON DTOs. The paths below are a proposed REST contract; naming may be adjusted consistently during implementation. Customer identity for customer-owned operations must come from the authenticated principal, not a client-supplied customer identifier.

| Method and path | Purpose |
|---|---|
| `POST /api/auth/login` | Authenticate customer credentials |
| `POST /api/auth/logout` | Clear the HttpOnly authentication cookie |
| `GET /api/customers/me` | Return the authenticated customer profile, excluding credentials |
| `GET /api/me/policies` | List authenticated customer policies |
| `GET /api/products/available` | List active products not already registered by the customer |
| `GET /api/products/search` | Search active product offers available to the authenticated customer |
| `POST /api/recommendations` | Generate and persist a recommendation request and its items |
| `GET /api/recommendations/{requestId}` | Retrieve a saved recommendation request and result |
| `POST /api/policies` | Register a policy and create its installment schedule |
| `GET /api/policies/{policyNumber}` | Retrieve policy details and payment totals |
| `GET /api/policies/{policyNumber}/installments` | Retrieve the policy installment schedule |
| `GET /api/policies/{policyNumber}/payments` | Retrieve payment transaction history |
| `POST /api/policies/{policyNumber}/payments` | Record one or more installment payments |
| `GET /api/policies/search` | Search only the authenticated customer's own policies using supported criteria and pagination |

### 4.1 Request and response conventions

- Use request DTOs with Bean Validation constraints for required values, length, format, and positive amounts where applicable.
- Return response DTOs, never JPA entities.
- Use ISO-8601 dates (`yyyy-MM-dd`) and decimal JSON numbers for monetary values. Avoid floating-point arithmetic internally.
- Include pagination metadata for search results.
- The authenticated principal is the source of customer identity for dashboard, product availability, recommendations, and registration.

### 4.2 Suggested request shapes

**Recommendation request**

```json
{
  "dateOfBirth": "1990-04-15",
  "insuranceGoals": "Family income protection",
  "preferredTerm": "20 years",
  "preferredPolicyType": "TERM"
}
```

Accept one or more of date of birth, insurance goals/preferences, preferred term, and insurance type. Calculate age from the supplied date of birth using the Java Time API. If date of birth is omitted, use the authenticated customer's profile date of birth; do not allow a client-supplied customer identifier to select another customer's profile.

**Policy registration request**

```json
{
  "productCode": "HD-TERM-001",
  "startDate": "2026-11-26"
}
```

The customer and product-dependent financial values are resolved by the server; clients must not be trusted to set customer identity, price, coverage, or eligibility.

**Payment request**

```json
{
  "paymentMethod": "UPI",
  "payments": [
    {
      "installmentNumber": 1,
      "amountPaid": 1250.00
    }
  ]
}
```

The payment API requires an `Idempotency-Key` header and must not create duplicate transactions when a request is retried with the same key.
The key is required, limited to 128 characters, and scoped to the authenticated customer. An exact retry returns the original payment response; reuse with a different policy or payment payload returns `409 Conflict`.

### 4.3 Search criteria

Marketplace product search supports product code, policy name, and policy type over active products available to the authenticated customer. Customer policy search supports policy identifier, customer name, and policy identifier plus date of birth, matching the source brief's search modes. Every policy search is restricted to the authenticated customer's own records; supplied name or date of birth is an additional match criterion, never an identity selector. Both searches are paginated and validate that a supported criterion is provided.

### 4.4 Required UI response fields

Response DTOs must include the information needed by the interfaces described in the assessment:

- **Dashboard policy:** policy identifier, policy name, policy type, term, start date, maturity date, premium frequency, premium amount, sum insured, and policy status.
- **Available product:** product code, policy name, policy type, term, sum insured, premium frequency, premium amount, and registration action/product reference.
- **Recommendation:** product fields from the available-product response, recommendation reason, suitability, and registration action/product reference.
- **Registration form/result:** customer name, date of birth, contact information; product type/name/term; policy start and calculated maturity dates; premium frequency and amount; sum insured; total premium payable; and, on success, generated policy identifier and confirmation.
- **Policy payment view:** policy identifier, customer name, policy name, term, start and maturity dates, total premium payable, total premium paid, remaining premium, assessed/paid/remaining penalties, estimated late penalty and estimated full amount due, plus installment number, scheduled amount, due date, grace-period end date, payment status, and a checkbox for each installment with a balance.
- **Payment history:** transaction reference, payment date, installment, amount paid, penalty amount, and payment method.
- **Policy search result:** policy identifier, customer name, date of birth, policy name/type, start and maturity dates, total premium payable, premium payment status, total premium paid, and remaining premium.

## 5. Processing rules

### 5.1 Authentication and customer context

- Validate credentials against stored password hashes.
- Reject inactive or invalid customer accounts according to account status.
- Protect authenticated endpoints with Spring Security and a signed JWT access token stored in an HttpOnly cookie.
- Set access-token lifetime to 30 minutes with no refresh token; the customer must sign in again after expiry. Logout clears the cookie.
- Use `Secure` cookies in production, `SameSite=Lax`, and CSRF protection for state-changing requests. Configure local HTTP development without weakening production settings.
- Derive customer-specific queries from the authenticated principal.
- Never include password hashes or authentication secrets in responses or logs.

The concrete session/token mechanism and cross-origin configuration are environment/application decisions.

### 5.2 Product discovery

1. Load active catalog products.
2. Load product identifiers already registered by the authenticated customer.
3. Exclude registered products.
4. Return only the product fields needed by the UI.

The database uniqueness constraint remains authoritative in case concurrent requests attempt duplicate registration.

The available-product response includes product code, policy name/type, term, sum insured, premium frequency, and premium amount.

### 5.3 Recommendations

1. Load the authenticated customer. Accept the recommendation date of birth as an input; when it is omitted, use the authenticated customer's date of birth. Calculate age using Java Time API.
2. Load active products and exclude products already held by the customer.
3. Provide the AI integration only the eligible product candidates and relevant preferences.
4. Parse the model result into a typed internal response.
5. Validate every returned product identifier against the candidate set; reject or safely omit invalid references and surface an integration error if the result cannot be trusted.
6. Persist the request, model/prompt metadata, and validated recommendation items.
7. Return stored recommendation DTOs.

AI-generated rationale and suitability are advisory. Product eligibility and registration validations remain deterministic.

### 5.4 Policy registration and schedule creation

Within one database transaction:

1. Resolve the authenticated customer and active product.
2. Check date of birth and age against the product’s configured entry limits.
3. Validate positive term and financial values and require the policy start date to be today or later.
4. Calculate maturity date and total payable using the product term and payment frequency. Derive the number of installments from the policy term and frequency; product configuration must produce a valid schedule.
5. Generate a unique policy number.
6. Create the customer policy and all scheduled installments.
7. Commit only when the policy and complete schedule are persisted.

Use decimal arithmetic with explicit scale/rounding rules. Convert premium frequency to its interval in months (monthly: 1, quarterly: 3, half-yearly: 6, yearly: 12). Calculate installment count as `term_months / interval_months`, and total premium payable as `premium_amount × installment_count`; product configuration must yield a whole number of installments. The first due date is the policy start date; calculate subsequent due dates from the original start date and interval to preserve month-end anchoring. Each grace-period end date is one calendar month after its due date using `LocalDate.plusMonths(1)`; Java's month-end clamping applies (for example, January 31 plus one month is the last day of February). A payment is late only after the grace-period end date. Calculate maturity as `start_date + term_months - 1 day`.

### 5.5 Payment processing

Process a payment request transactionally:

1. Resolve the policy and confirm the caller is authorized to operate on it.
2. Lock or otherwise safely coordinate updates to the affected installments.
3. Validate each selected installment belongs to the policy, amount is positive, and amount does not exceed the allowed outstanding amount.
4. On the first valid payment after an installment's grace-period end date, assess a one-time penalty equal to 10% of its outstanding principal, rounded to two decimal places using half-up rounding. Do not compound or reassess it.
5. Treat each request amount as the total collected, including any penalty. Allocate it to unpaid penalty first and then to premium principal; track both allocations separately. Reject a payment greater than the combined outstanding amount.
6. Record a unique payment transaction and update paid amount/status.
7. Recalculate policy totals and overall payment state.
8. Commit all selected installment updates and transaction records together.

Repeated requests must be idempotent or rejected without adding duplicate payment records. Payment status is based on persisted principal and penalty balances: `PAID` only when both are satisfied; otherwise use `PARTIALLY_PAID`, `PENDING`, or `OVERDUE` as appropriate. Payment method is recorded as a non-empty value of at most 40 characters; no payment gateway is used in this release.

### 5.6 Policy search

- Search active product offers and authenticated-customer-owned policy records as distinct search operations.
- Build database queries from supported criteria rather than loading all records for in-memory filtering.
- Apply pagination or a configured result bound.
- Map results to response DTOs containing only specified policy and customer summary information.
- Enforce ownership before returning policy, customer, payment, or installment details. Never expose another customer's contract through search.

## 6. Error handling

Use a consistent error response, for example:

```json
{
  "timestamp": "2026-11-26T10:00:00Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "The request contains invalid values.",
  "fieldErrors": [
    {
      "field": "startDate",
      "message": "Must not be before the allowed policy start date."
    }
  ]
}
```

Suggested status mapping:

- `400 Bad Request`: malformed request or Bean Validation failure.
- `401 Unauthorized`: missing or invalid authentication.
- `403 Forbidden`: authenticated user lacks access.
- `404 Not Found`: requested policy/product/recommendation does not exist or is not visible to the caller.
- `409 Conflict`: duplicate registration, duplicate transaction, or incompatible state transition.
- `422 Unprocessable Entity`: valid request shape but violated business eligibility/payment rule, if adopted consistently.
- `500 Internal Server Error`: unexpected persistence or application failure.
- `503 Service Unavailable`: configured AI provider unavailable, where applicable.

Do not return success-shaped responses after failed operations. Do not expose stack traces, credentials, or internal provider details to clients. Log diagnostic context without sensitive personal or authentication data.

## 7. Frontend behavior

- Use React functional components and hooks.
- Use Axios through a shared API client that handles authentication context, JSON conversion, and common error mapping.
- Validate forms for immediate feedback and display server-side field errors.
- Disable duplicate submissions while requests are in progress.
- Show loading indicators during network operations and confirmation messages after successful registration/payment.
- Show explicit empty states for no registered policies, no available products, no recommendations, and no search results.
- Display financial totals and statuses from authoritative server responses.
- Use accessible labels and keyboard-operable controls for form fields, tables, and installment selection.

## 8. Configuration and operations

Keep environment-specific values in configuration properties or environment variables:

- PostgreSQL connection settings and credentials.
- Spring AI provider, model, and provider credentials.
- Authentication/session settings.
- CORS/origin settings where applicable.
- Logging level and application port.

Never commit secrets. Use schema migrations or another controlled schema-management mechanism for database changes. Use BCrypt for customer password hashes. The application must fail visibly and log useful non-sensitive diagnostics when required configuration is missing.

## 9. Verification strategy

### Backend tests

- Unit tests for age eligibility, maturity/term calculations, premium totals, installment generation, penalty calculation, amount limits, and payment status transitions.
- Service tests for duplicate registration, customer ownership, product filtering, recommendation candidate validation, and transaction behavior.
- Repository tests for uniqueness constraints, search criteria, and pagination.
- Controller tests for DTO validation, authentication/authorization, response shape, and HTTP error mapping.
- Integration tests against PostgreSQL for policy-plus-schedule persistence and atomic multi-installment payment processing.
- AI integration tests using a controlled model stub to verify catalog grounding, invalid product rejection, metadata persistence, and provider-failure handling.

### Frontend tests

- Component tests for forms, validation feedback, loading/error/empty states, and successful actions.
- API integration tests for authentication, product registration flow, recommendation display, policy search, and payment submission.

### Quality checks

- Confirm API responses never expose persistence entities or credentials.
- Confirm financial calculations use `BigDecimal` and are consistent with stored payment records.
- Confirm customer-specific queries are bound to authenticated identity.
- Confirm database constraints protect invariants under concurrent requests.

## 10. Decisions and remaining confirmations

### Confirmed implementation decisions

1. **Authentication:** Use Spring Security with 30-minute JWT access tokens stored in HttpOnly cookies; do not expose tokens to browser JavaScript or local storage. There is no refresh token; logout clears the cookie. Use `SameSite=Lax`, enable `Secure` in production, and protect state-changing requests against CSRF.
2. **Identifiers:** Use UUID consistently for primary and foreign keys.
3. **Payment scope:** Record externally completed/manual payments only. No payment gateway is in the current release.
4. **Duplicate registration:** Permanently allow only one policy per customer/product pair, enforced by `UNIQUE(customer_id, product_id)`.
5. **Schedule dates:** Anchor each due date to the original policy start date using the payment interval in months. Calculate maturity as `start_date + term_months - 1 day`, with Java `LocalDate` month-end behavior.
6. **Late penalty:** Apply a 10% penalty once per installment, on principal outstanding at the first late payment. Track the penalty separately and do not compound or reapply it.
7. **Payment idempotency:** Require an `Idempotency-Key` header. The server generates the transaction reference and must not create duplicate payment records for retries with the same key.
8. **Recommendations:** Return up to three eligible products. If the configured provider is unavailable, return an explicit `503`; do not fabricate results or use ungrounded fallback recommendations.
9. **Installment grace period:** Set each installment's grace-period end date to one calendar month after its due date using `LocalDate.plusMonths(1)`; a payment after that date is late.
10. **Policy start date:** A policy start date must be today or a future date.

### Remaining confirmations

1. **Production non-functional targets:** Confirm performance, availability, accessibility, audit/data retention, backup, and recovery targets in the separate production-readiness phase.
