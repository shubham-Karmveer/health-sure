# HealthDekho — Product Requirements Document

**Product:** HealthDekho Insurance Management System  
**Document status:** Draft  
**Source:** `FDE_HealthDekho_Capstone_Project_QP.md`

## 1. Product summary

HealthDekho is a web application for customers to discover insurance products, register policies, receive AI-assisted recommendations, and manage premium payments. It replaces manual insurance operations with a centralized experience backed by a relational database and a REST API.

The application must enforce eligibility, registration, and payment rules deterministically. AI may personalize and explain recommendations, but it may only recommend products in the active product catalog and may not override business rules.

## 2. Problem and opportunity

Manual customer, policy, premium, and search processes make it difficult to track payment compliance, understand customer needs, find suitable products, and process insurance operations efficiently. HealthDekho will centralize these workflows and provide a personalized customer experience.

## 3. Goals and success outcomes

- Give authenticated customers a clear view of their registered policies.
- Help customers discover eligible products and register for them.
- Provide explainable, catalog-grounded AI recommendations and preserve their history.
- Generate policy premium amounts and installment schedules consistently.
- Let customers review payment schedules, record payments, and see accurate balances.
- Support policy lookup and search with useful results and clear no-result feedback.
- Maintain data integrity, auditability, and separation of API, business, persistence, and AI responsibilities.

## 4. Scope

### 4.1 Current release

1. Customer authentication and customer-specific dashboard.
2. Active product discovery, excluding products already registered by the customer.
3. AI-assisted recommendations based on customer details and preferences.
4. Policy registration with eligibility validation and premium schedule generation.
5. Policy lookup, premium installment viewing, and payment recording.
6. Customer insurance policy search.

### 4.2 Out of scope

- Claim registration, assessment, and approval (future enhancement only).
- Actual supporting-document upload or document-content analysis.
- Any capabilities not described in the source brief, such as a payment gateway, agent portal, or administrative product-management UI.

## 5. Users

### Customer

An authenticated policyholder who can view their policies, discover and register for products, request recommendations, review premium schedules, record payments, and search for policies.

### Processing or operations user

No separate operations-user workflow is defined for the current release. Such a role appears only in the future claim-decision example.

## 6. User journeys

### 6.1 Sign in and review policies

1. Customer enters configured credentials.
2. On success, the application opens the customer dashboard.
3. The dashboard lists only the authenticated customer’s policies, or explains that no policies are registered.

### 6.2 Discover and register for a product

1. Customer views active products not already registered.
2. Customer selects **Register**.
3. The registration form is populated from the customer profile and selected product.
4. The system validates eligibility and input, calculates policy dates and premiums, creates a unique policy identifier, and generates the installment schedule.
5. Customer sees a confirmation and the new policy identifier.

### 6.3 Request recommendations

1. Customer provides one or more supported inputs: date of birth, insurance goals/preferences, preferred term, and insurance type. The date-of-birth field is prefilled from the authenticated customer profile.
2. The server verifies any submitted date of birth against the authenticated customer's profile, calculates age, loads active products, excludes products already registered, and applies configured product entry-age limits.
3. The AI generates recommendations grounded only in this eligible product set.
4. Recommendations and request/model metadata are persisted and displayed with rationale and suitability.
5. Customer can start registration for a recommended product.

### 6.4 Review a policy and record a payment

1. An authenticated customer searches for one of their policies by its unique identifier.
2. The application displays policy totals and its installment schedule only if the policy belongs to that customer.
3. Customer selects eligible installments and submits payment amounts.
4. The system validates amounts, applies any applicable late penalty, records transaction details, and updates installment and policy balances atomically.
5. The updated payment history and status are displayed.

### 6.5 Search marketplace products and customer policies

1. Customer searches active product offers by product code, policy name, or policy type.
2. Customer can separately search policy records belonging to their authenticated account by policy identifier, customer name, or policy identifier and date of birth, matching the source brief's search modes.
3. The application validates criteria and returns matching results in a bounded or paginated result set.
4. If there are no matches, the application shows a clear no-results message. Search never returns another customer's policy/payment data.

Marketplace product search is an agreed product-discovery addition; it is separate from the source brief's FR-5 customer policy search.

## 7. Functional requirements and acceptance criteria

### Source-brief requirement crosswalk

| Source brief requirement | PRD requirement |
|---|---|
| FR-1: Customer dashboard, available products, and registration entry point | FR-1 and FR-2 |
| FR-2: AI-assisted recommendations | FR-3 |
| FR-3: Policy registration | FR-4 |
| FR-4: Premium payment management, including lookup by unique policy identifier | FR-5 |
| FR-5: Customer insurance policy search | FR-6 |

### FR-1: Authentication and customer dashboard

- Authenticate customers using Spring Security and configured customer credentials.
- Store a 30-minute JWT in an HttpOnly cookie; do not expose it to browser JavaScript or local storage. After expiry, the customer signs in again.
- Invalid credentials produce an appropriate error; successful authentication routes to the dashboard.
- Dashboard data is scoped to the authenticated customer.
- Display the authenticated customer's policies in a table with identifier, name, type, term, start and maturity dates, premium frequency and amount, sum insured, and status.
- Show an empty state when the customer has no policies.

**Acceptance criteria**

- An unauthenticated user cannot access customer-specific dashboard data.
- The authentication cookie is HttpOnly, and logout clears it.
- Signing in with valid credentials shows only that customer’s policies.
- Invalid credentials do not authenticate the user and produce a useful error.
- A customer with no policies sees an empty state rather than a blank table.

### FR-2: Product discovery and registration entry point

- List active products and exclude products for which the customer already has a registered policy.
- Show available products in a table with product code, name, type, term, sum insured, premium frequency, premium amount, and a registration action.
- Selecting **Register** opens registration with the selected product pre-populated.
- The defined duplicate-registration rule is one policy per customer per product.

**Acceptance criteria**

- Inactive products and already-registered products are absent from the customer’s available-product list.
- Each displayed product has the correct catalog information and opens registration for that product.
- Duplicate registration is rejected by backend business validation and protected by a database uniqueness constraint.

### FR-3: AI-assisted recommendations

- Collect one or more supported inputs: date of birth, insurance goals/preferences, preferred policy term, and insurance type. Prefill date of birth from the authenticated customer profile; reject a submitted date of birth that differs from that profile. Calculate age with the Java Time API.
- Retrieve active products, exclude products already held by the customer, and exclude products outside their configured entry-age limits before requesting recommendations.
- Recommendations must reference only products in the retrieved eligible catalog.
- Show up to three recommendations in a table with product code, name, type, term, sum insured, frequency, premium, recommendation reason, suitability, and registration action.
- Persist each recommendation request, its inputs, calculated age, model/prompt metadata, and returned recommendation items.
- Selecting **Register** opens registration with the recommended product pre-populated.
- AI output does not replace deterministic eligibility, registration, or financial validation.

**Acceptance criteria**

- A recommendation cannot contain an unknown, inactive, or already-registered product.
- A submitted date of birth that does not match the authenticated customer's profile is rejected; product candidates respect configured entry-age limits.
- A recommendation request and its items remain available in persistent storage with the metadata described in the data model.
- Every displayed recommendation includes its rationale and suitability.
- Registration from a recommendation follows the same validation rules as registration from product discovery.

### FR-4: Policy registration

- Pre-populate customer name, date of birth, and contact information from the authenticated customer profile.
- Populate product-dependent values from the selected product; default the start date to the current date.
- Calculate maturity date, total premium payable, and the premium installment schedule.
- Validate required inputs, non-future date of birth, configured minimum and maximum entry age, logical dates, and positive term, premium, and sum insured.
- Generate a unique policy identifier.
- Persist the policy and all installments together.
- Return a confirmation including the policy identifier.

**Acceptance criteria**

- A customer outside the product’s configured entry-age range cannot register.
- Invalid dates or non-positive term/financial values are rejected with useful validation feedback.
- A successful registration creates one policy and its complete schedule; partial persistence is not allowed.
- A second registration for the same customer and product is rejected.
- The returned confirmation contains the generated policy identifier.

### FR-5: Premium schedule and payment management

- Provide a policy-number lookup in the payment interface. Search by the unique policy identifier and display details only when the policy belongs to the authenticated customer: customer name, policy name, term, dates, total payable, total paid, and remaining amount.
- Display installment number, scheduled amount, due date, grace-period end date, status, and selection for eligible pending installments.
- Support one or multiple selected installments and partial payments where applicable.
- Record payment date, unique transaction reference, amount, penalty, payment method, and transaction history.
- Apply a 10% penalty to the outstanding premium amount when payment is made after the one-calendar-month grace period.
- Prevent duplicate payments against an installment and amounts greater than the outstanding amount.
- Recalculate installment and policy outstanding balances. A policy is fully paid only when all applicable installments are paid.
- Process payment changes transactionally.

**Acceptance criteria**

- A valid payment updates the installment, policy totals, and payment history consistently.
- A payment cannot exceed the remaining amount due, including the applicable penalty.
- A payment after the one-calendar-month grace-period end date applies the specified 10% penalty to the outstanding premium amount; an on-time payment does not.
- Repeated submission cannot create a duplicate transaction for the same payment request.
- Partial payments are reflected as `PARTIALLY_PAID`; completed installments are `PAID`; unpaid installments retain the appropriate pending or overdue state.
- If payment processing fails, no partial set of payment or balance updates is committed.

### FR-6: Marketplace product and own-policy search

- Search active marketplace offers by product code, policy name, or policy type.
- Separately search policy records owned by the authenticated customer using the source-brief criteria: policy identifier, customer name, or policy identifier with date of birth. Supplied name and date of birth are match criteria, not identity selectors.
- Product search returns the product fields required by FR-2. Own-policy search returns policy dates and financial/payment totals required by FR-5.
- Display own-policy search results in a table with the fields specified by the source brief.
- Never return another customer's policy, payment, or personal data.
- Validate criteria, use efficient database queries, and bound or paginate results.
- Show a no-results message when no policies match.

**Acceptance criteria**

- Product search returns only active offers available to the customer; own-policy search returns only that customer's matching records and supports each source-brief search mode.
- Search results never include another customer's policy, payment, or personal data.
- Invalid or insufficient search criteria are rejected with clear feedback.
- Large result sets are bounded or paginated.
- No matching product or policy produces an explicit empty state.

## 8. Business rules

1. A customer’s date of birth cannot be in the future.
2. Product entry-age limits are enforced at registration.
3. A customer may register for a given product only once (`UNIQUE(customer_id, product_id)`).
4. Policy term, premium amount, and sum insured must be positive.
5. Policy maturity date must be consistent with start date and product term.
6. Premium arithmetic uses decimal-safe `BigDecimal`; money is not calculated with floating-point types.
7. Installments follow the selected premium frequency and policy term.
8. A payment must be positive and cannot exceed the outstanding amount.
9. A late payment after the one-calendar-month grace period incurs a 10% penalty on the outstanding premium amount. The calendar month is calculated from the installment due date, including month-end clamping.
10. Payment status and policy paid status are derived from recorded installments and payments, not from AI output.
11. AI recommendations may select only eligible products retrieved from the database.
12. Database constraints and transactional service logic enforce data integrity in addition to frontend validation.

## 9. Data and audit requirements

Use normalized PostgreSQL tables with explicit primary/foreign keys, unique constraints, and indexes for frequently searched fields. Keep product master data separate from customer policy transactions. Persist recommendation requests and items separately for auditability.

Current-scope logical entities:

- **Customer:** credentials hash, contact/profile data, status, and audit timestamps.
- **Insurance product:** product code, product attributes, term, frequency, premium, coverage, age bounds, and active state.
- **Customer policy:** policy number, customer/product references, policy dates and financial snapshots, and status.
- **Premium installment:** installment sequence, dates, scheduled and paid amounts, and status.
- **Premium payment:** immutable transaction reference, installment, date, paid amount, penalty, method, and creation timestamp.
- **AI recommendation request:** customer inputs, calculated age, model/version/prompt metadata, and request time.
- **AI recommendation item:** request/product references, rank, rationale, suitability, and registration selection state.

Storage guidance from the brief: money as `NUMERIC(19,2)`, dates as `DATE`, and audit timestamps as `TIMESTAMP WITH TIME ZONE`. Use UUID or BIGINT identifiers consistently. Store passwords as BCrypt hashes; sample password values in the source are placeholders only.

## 10. User experience requirements

- Use React functional components and hooks.
- Provide form validation, loading indicators, success messages, and actionable error messages.
- Present policies, products, recommendations, search results, and installments in readable tables.
- Provide clear empty states for no policies, no products/recommendations where applicable, and no search results.
- Keep server-side validation authoritative; frontend validation is for timely feedback, not a substitute.

## 11. Technical and quality requirements

- **Backend:** Java 21, Spring Boot 3.5.7, Spring Web, Spring Security, Spring Data JPA, Spring AI, Bean Validation, and PostgreSQL.
- **Frontend:** React.js, functional components/hooks, and Axios for REST integration.
- **Architecture:** layered Controller, Service, Repository, Entity/DTO, and AI integration layers. Business logic belongs in services; REST APIs use DTOs and do not expose JPA entities.
- **Java practices:** records for request/response DTOs where appropriate; `LocalDate`/Java Time API for dates; `YearMonth` for premium periods; `BigDecimal` for money; appropriate Optional, Stream, and collection operations.
- **Reliability:** use transactions for policy/schedule creation and payment processing; return appropriate HTTP status codes and handle validation, business-rule, and persistence failures explicitly.
- **Configuration:** keep environment-specific settings in configuration properties, including AI provider/model configuration and database credentials.
- **AI safety and correctness:** recommendation generation is constrained to database-sourced candidates; persisted metadata supports traceability; deterministic rules remain authoritative.
- **Security and privacy:** authenticate customers and restrict customer-specific dashboard information to the authenticated customer. Protect credentials using password hashes.

The source brief does not specify numeric availability, latency, throughput, retention, or accessibility targets; these require product/engineering decisions before production release.

## 12. Future release: AI-assisted claim assessment

Claim registration, policy/payment checks as of claim date, document availability and verification, AI assessment summary, deterministic approval rules, assessment persistence, and decision audit are explicitly future scope. They must not be required for the current assessment implementation.

Future approval must require all mandatory deterministic conditions: premiums due by the claim date are paid, no overdue/outstanding premium exists as of that date, the policy is valid on the claim date, and mandatory documents are available and verified. AI failure or AI output must never result in or override an approval. Suggested statuses include `APPROVED`, `REJECTED`, `PENDING_DOCUMENTS`, and `PENDING_REVIEW`.

## 13. Open questions

1. What production targets apply for performance, availability, accessibility, audit retention, backup/recovery, and personal-data retention?
