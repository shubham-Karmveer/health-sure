**Java FDE** **Capstone** **Assessment**

**Insurance Management System**

**Application Name**: HealthDekho – Insurance Management System **Assessment Type**: Java Full-Stack with AI - Capstone Assessment **Technology Stack**: Java 21 | Spring Boot 3.5.7 | Spring Data JPA | PostgreSQL | Spring AI | React **Assessment Objective**: Design and develop a full-stack, enterprise-oriented insurance management application.

**1. Business Scenario**

HealthDekho, an insurance service provider, intends to modernize its existing insurance management operations by developing a centralized, web-based Insurance Management System.

Currently, the organization relies on manual processes for managing customer information, insurance policy registration, premium collection, policy searches, insurance recommendations and claim processing. These processes have resulted in operational inefficiencies, difficulties in tracking premium payment compliance, limited customer insights and delays in insurance claim assessment.

To address these challenges, HealthDekho has decided to develop a modern full-stack application that automates its insurance operations and incorporates AI-assisted capabilities for personalized insurance policy recommendations.

You have been assigned the responsibility of designing and developing this application using the specified technology stack.

The application must provide the following capabilities:

- Customer authentication and secure access to the application.

- A personalized customer dashboard displaying registered insurance policies.

- Display of available insurance products that the customer has not already registered for.

- AI-based insurance policy recommendations based on customer information and preferences.

- Policy registration from both available-policy listings and AI-generated recommendations.

- Premium payment management and payment history tracking.

- Customer insurance policy search.

The solution must demonstrate enterprise-level application development practices, appropriate architectural separation, data integrity, business-rule enforcement and effective frontend-backend integration.

**2. Technology and Implementation Requirements**

**2.1 Backend Technologies**

- Java 21 (Mandatory)

- Spring Boot 3.5.7

- Spring Web for REST API development

- Spring Security for authentication and authorization

- Spring Data JPA for database operations

- PostgreSQL for relational data persistence

- Spring AI for AI-powered functionalities

- Bean Validation for request validation

- Appropriate exception handling and HTTP status codes

**2.2 Frontend Technologies**

- React.js

- Functional Components and React Hooks

- Form handling and validation

- REST API integration using Axios

- Search, filtering and tabular data presentation

- Appropriate loading indicators, success messages and error handling

**2.3 Application Architecture Guidelines**

Implement a layered architecture comprising Controller, Service, Repository, Entity/DTO and AI Integration layers.

- Use DTOs for communication between frontend and backend.

- Maintain appropriate entity relationships and database constraints.

- Implement business logic within the service layer.

- Avoid exposing JPA entities directly through REST APIs.

- Maintain environment-specific configurations using appropriate configuration properties.

- Implement transaction management wherever required.

- Persist AI-generated insurance recommendations wherever specified.

- AI must not replace deterministic business validations or financial calculations.

**3. Functional Requirements**

**FR-1: Customer Dashboard, Policy Discovery and Registration**

Business Objective: Provide customers with a personalized dashboard to view their existing insurance policies, explore available insurance products and initiate registration of suitable insurance policies.

**A. Customer Authentication**

- Develop a login interface using React.js.

- Implement customer authentication using Spring Security.

- Authenticate customers against the configured customer credentials.

- On successful authentication, redirect the customer to the dashboard.

- Display an appropriate authentication error message for invalid credentials.

- Restrict customer-specific dashboard information to the authenticated customer.

**B. Customer Dashboard**

- Develop a customer dashboard displaying all insurance policies registered by the logged-in customer.

- Retrieve the policy information associated with the authenticated customer.

- Display the following information in a tabular format:

| **Field** | **Description** |
| undefined | undefined |
| Policy Identifier | Unique policy number |
| Policy Name | Registered insurance product |
| Policy Type | Insurance category |
| Policy Term | Duration of the policy |
| Policy Start Date | Policy commencement date |
| Policy Maturity Date | Policy maturity date |
| Premium Frequency | Monthly, Quarterly, Half-Yearly or Yearly |
| Premium Amount | Premium payable per installment |
| Sum Insured | Insurance coverage amount |
| Policy Status | Current policy status |
- Display an appropriate message when the customer has not registered any insurance policy.

**C. View Available Insurance Policies**

- Retrieve all active insurance products from the PostgreSQL database.

- Retrieve the existing policies of the logged-in customer.

- Exclude products for which the customer has already registered a policy, according to the defined duplicate-registration business rule.

- Display the remaining available insurance products in a tabular format:

| **Field** | **Description** |
| undefined | undefined |
| Product Code | Unique insurance product code |
| Policy Name | Name of the insurance product |
| Policy Type | Insurance category |
| Policy Term | Duration of the policy |
| Sum Insured | Insurance coverage amount |
| Premium Frequency | Premium payment frequency |
| Premium Amount | Premium payable per installment |
| Action | Register button |
- Provide a Register button against every available insurance product.

- On clicking Register, navigate the customer to the policy registration interface with the selected product information pre-populated.

**FR-2: AI Insurance Recommendation**

Business Objective: Generate personalized, explainable insurance product recommendations using Spring AI while restricting recommendations to eligible products available in the application database.

**D. AI-Based Policy Recommendations**

- Provide an option on the customer dashboard to generate personalized insurance policy recommendations using Spring AI.

- Allow customers to provide one or more of the following inputs:

Date of Birth (DOB)

Insurance Goals and Preferences

Preferred Policy Term

Insurance Type, wherever applicable

- Automatically calculate the customer's age using the Java Time API.

- Retrieve active insurance products from the database.

- Retrieve the customer's existing registered policies.

- Exclude products already availed by the customer.

- Integrate Spring AI with a configured AI model to generate personalized recommendations based on customer inputs and available insurance products.

- Ensure that AI-generated recommendations reference only products retrieved from the database.

- Display the generated recommendations in a tabular format:

| **Field** | **Description** |
| undefined | undefined |
| Product Code | Unique insurance product code |
| Policy Name | Recommended insurance product |
| Policy Type | Insurance category |
| Policy Term | Duration of the policy |
| Sum Insured | Insurance coverage amount |
| Premium Frequency | Premium payment frequency |
| Premium Amount | Premium payable per installment |
| Recommendation Reason | AI-generated recommendation rationale |
| Suitability | Suitability for the customer's goals |
| Action | Register button |
- Provide a Register button against every recommended insurance product.

- On clicking Register, navigate the customer to the policy registration interface with the selected product information pre-populated.

- Save generated recommendations against the customer in the database.

**E. Java 21 Implementation Expectations**

Records for request and response DTOs.

LocalDate and Java Time API for date calculations.

Optional for repository lookups.

Stream API for filtering available products and excluding existing policies.

Collectors and suitable collection operations.

Pattern matching with instanceof wherever appropriate.

**FR-3: Insurance Policy Registration**

Business Objective: Enable customers to register new insurance policies and automatically calculate policy-related financial and date information.

**A. Policy Registration Interface**

Develop a React-based registration form that captures and displays the following information:

| **Field** | **Expected Behaviour** |
| undefined | undefined |
| Customer Name | Automatically populated from logged-in customer details |
| Date of Birth | Automatically populated from customer details |
| Contact Information | Automatically populated from customer details |
| Policy Type | Retrieved from the selected insurance product |
| Policy Name | Pre-populated based on selected product |
| Policy Term | Automatically populated based on selected product |
| Policy Start Date | Default to current date |
| Policy Maturity Date | Automatically calculated based on policy term |
| Premium Payment Frequency | Automatically populated based on selected product |
| Premium Amount | Retrieved from the selected insurance product |
| Sum Insured | Automatically populated based on selected product |
| Total Premium Payable | Calculated based on premium amount, frequency and policy term |
**B. Business and Validation Requirements**

- Validate all mandatory fields before submitting the registration request.

- Ensure that the customer's date of birth is not in the future.

- Validate customer eligibility against the minimum and maximum entry age configured for the insurance product.

- Validate the logical consistency of policy start and maturity dates.

- Ensure premium amount, sum insured and policy term are positive.

- Generate a unique policy identifier for every registered policy.

- Prevent duplicate policy registration for the same customer and insurance product according to the defined business rules.

- Calculate the total premium payable based on the selected premium frequency and policy term.

- Generate the premium installment schedule for the registered policy.

- Persist customer policy information and the associated installment schedule in PostgreSQL using Spring Data JPA.

- Display the generated policy identifier and a confirmation message after successful registration.

- Handle validation errors, database failures and business-rule violations appropriately.

**C. Java 21 Implementation Expectations**

- Records for immutable request and response DTOs.

- LocalDate and Java Time API for date calculations and validations.

- BigDecimal for financial calculations.

- Optional for handling repository lookups.

- Appropriate Stream API and collection operations.

**FR-4: Insurance Premium Payment Management**

Business Objective: Provide functionality to retrieve an existing policy, view its premium payment schedule and update premium payment information whenever a customer makes a payment.

**A. Policy Search**

Develop a React interface that allows users to search for an existing policy using its unique policy identifier. Display the following information:

Policy Identifier

Customer Name

Policy Name

Policy Term

Policy Start Date

Policy Maturity Date

Total Premium Payable

Total Premium Paid

Total Remaining Premium Amount

**B. Premium Payment Schedule**

Display all premium installments in a tabular view.

| **Field** | **Description** |
| undefined | undefined |
| Installment Number | Sequential installment number |
| Premium Amount | Scheduled premium amount |
| Due Date | Premium installment due date |
| Grace Period End Date | Last date of the grace period |
| Payment Status | PAID / PENDING / PARTIALLY_PAID / OVERDUE |
| Payment Selection | Checkbox for selecting eligible pending installments |
**C. Premium Payment Processing**

Allow users to select one or multiple pending premium installments and update their payment information. The application must:

- Capture the premium amount paid.

- Automatically update the payment status after successful payment.

- Automatically record the payment date and transaction details.

- Apply a 10% penalty on the outstanding premium amount if payment is made after the one-month grace period.

- Automatically recalculate the outstanding premium amount.

- Maintain an accurate premium payment history.

- Prevent duplicate payments against the same scheduled installment.

- Prevent payments exceeding the outstanding amount.

- Support partial payments, wherever applicable.

- Ensure that a policy is marked as fully paid only when all applicable premium installments have been paid.

**D. Java 21 Implementation Expectations**

Records for premium payment request and response DTOs.

YearMonth for representing premium payment periods.

BigDecimal for financial calculations.

Switch expressions or pattern matching with switch for payment-status processing.

Stream API and collection operations for processing payment records.

Transaction management for payment processing.

**FR-5: Customer Insurance Policy Search**

Business Objective: Enable users to retrieve and view insurance policies associated with a customer using different search criteria.

**A. Search Interface**

Develop a React-based search interface with the following fields:

Customer Name

Policy Identifier

Date of Birth

Allow users to perform searches using:

- Policy Identifier.

- Customer Name.

- Policy Identifier and Date of Birth.

**B. Search Results**

Retrieve all matching insurance policies and display the results in a tabular format.

| **Field** | **Description** |
| undefined | undefined |
| Policy Identifier | Unique policy reference |
| Customer Name | Customer's name |
| Date of Birth | Customer's date of birth |
| Policy Name | Registered insurance product |
| Policy Type | Insurance category |
| Policy Start Date | Policy commencement date |
| Policy Maturity Date | Policy maturity date |
| Total Premium Payable | Total policy premium |
| Premium Payment Status | Current payment status |
| Total Premium Paid | Cumulative paid amount |
| Remaining Premium | Outstanding premium amount |
**C. Additional Requirements**

- Retrieve all policies matching the supplied search criteria.

- Display an appropriate message when no matching records are found.

- Validate the search criteria before processing.

- Implement efficient database queries using Spring Data JPA.

- Support pagination or bounded result retrieval wherever appropriate.

**D. Java 21 Implementation Expectations**

- Records for search response DTOs.

- Stream API for transforming entity objects into DTOs.

- Collectors.groupingBy() or other suitable collectors wherever required.

- Optional for single-record lookups.

- Pagination for handling larger result sets.

**4. Future Enhancements**

**FE-1: AI-Assisted Claim Assessment and Approval**

Future Enhancement Objective: Introduce intelligent claim processing in a later release by combining deterministic business validations with AI-assisted claim assessment and a controlled approval workflow.

Scope Note: This feature is excluded from the current implementation and assessment scope. The requirements below are retained as a future-state specification for a subsequent release.

**A. Claim Registration Interface**

Develop a React-based claim registration form with the following fields:

| **Field** | **Description** |
| undefined | undefined |
| Claim Identifier | Unique claim reference |
| Policy Identifier | Associated insurance policy |
| Customer Identifier | Claimant's customer reference |
| Claim Date | Date of claim submission/event as defined by business rules |
| Claim Amount | Amount requested by the customer |
| Claim Description | Description of the claim |
| Supporting Documents Available | Checkbox indicating whether mandatory supporting documents are available |
Scope clarification: Actual document upload, file storage and document-content analysis are not required. The application shall capture document availability and verification status for the claim assessment workflow.

**B. AI-Assisted Assessment**

Integrate Spring AI with tool calling to facilitate access to the required business information. The assessment process must:

- Retrieve policy details from PostgreSQL.

- Retrieve the associated premium payment history.

- Verify whether all premiums are due on or before the claim date has been paid.

- Verify that no premium payment was overdue or outstanding as of the claim date.

- Check the supporting-document availability and verification status.

- Use Spring AI to generate an assessment summary based on the claim description and retrieved policy/payment information.

**C. Mandatory Claim Approval Rules**

The following business rules must be enforced before approving a claim:

- Premium Compliance: All premiums due on or before the claim date must have been paid.

- Payment Default: No outstanding or overdue premium must exist as of the claim date.

- Policy Validity: The associated insurance policy must be valid on the claim date.

- Document Verification: Mandatory supporting documents must be available and satisfy the defined verification requirements.

- Final Approval: A claim can be approved only when all mandatory business conditions are satisfied.

- If any mandatory condition fails, the application must not approve the claim.

- Use appropriate claim statuses, such as:

  - APPROVED

  - REJECTED

  - PENDING_DOCUMENTS

  - PENDING_REVIEW

Record the reasons associated with the assessment outcome.

**D. Claim Assessment Persistence**

Persist with the following information:

Claim Identifier

Policy Identifier

Customer Identifier

Claim Date

Claim Amount

Document Availability and Verification Status

Premium Compliance Status

AI Assessment Summary

Final Claim Status

Approval/Rejection Reasons

Decision Timestamp

Approver or Processing User Identifier, wherever applicable

**E. Additional Requirements**

- An AI service failure must never result in automatic claim approval.

- AI-generated assessments must not override mandatory business-rule failures.

- Maintain a clear separation between AI-generated assessment results and deterministic business validations.

- Provide appropriate error handling and meaningful responses.

- Persist the final assessment and claim decision.

**5. Database Design**

**5.1 Database Design Principles**

Use PostgreSQL with normalized relational tables, explicit primary and foreign keys, unique constraints and indexes for frequently searched fields.

Use UUID or BIGINT identifiers consistently. Store money values as NUMERIC(19,2), dates as DATE and audit timestamps as TIMESTAMP WITH TIME ZONE.

Keep insurance product master data separate from customer policy transactions. Do not duplicate premium, term or coverage rules across transactional tables.

Persist AI recommendation requests and recommendation items separately so that prompts, model metadata, generated rationale and selected products remain auditable.

Claim-related tables belong to the future enhancement scope and must not be required for the current assessment implementation.

**5.2 Current-Scope Database Tables**

The following logical tables support FR-1 through FR-5. Teams may adapt physical names while preserving the relationships, constraints and auditability described below.

| **Table** | **Purpose** | **Key Columns** | **Core Constraints / Indexes** |
| --- | --- | --- | --- |
| customer | Stores authenticated customer profile data. | customer_id PK; email UQ; password_hash; full_name; date_of_birth; phone; status; created_at; updated_at | date_of_birth <= CURRENT_DATE; status indexed |
| insurance_product | Master catalog of insurance products. | product_id PK; product_code UQ; policy_name; policy_type; term_months; premium_frequency; premium_amount; sum_insured; min_entry_age; max_entry_age; active | premium_amount > 0; sum_insured > 0; active indexed |
| customer_policy | Registered policy owned by a customer. | policy_id PK; policy_number UQ; customer_id FK; product_id FK; start_date; maturity_date; premium_frequency; premium_amount; sum_insured; total_premium_payable; status; created_at | UQ(customer_id, product_id) for the defined duplicate rule; customer_id and status indexed |
| premium_installment | Scheduled premium obligations for a policy. | installment_id PK; policy_id FK; installment_number; due_date; grace_period_end_date; scheduled_amount; paid_amount; payment_status | UQ(policy_id, installment_number); paid_amount between 0 and scheduled amount plus applicable penalty |
| premium_payment | Immutable premium payment transaction history. | payment_id PK; installment_id FK; transaction_reference UQ; payment_date; amount_paid; penalty_amount; payment_method; created_at | amount_paid > 0; installment_id and payment_date indexed |
| ai_recommendation_request | Captures each recommendation interaction. | recommendation_request_id PK; customer_id FK; insurance_goals; preferred_term; preferred_policy_type; calculated_age; model_name; model_version; prompt_version; requested_at | customer_id and requested_at indexed |
| ai_recommendation_item | Stores products returned for a recommendation request. | recommendation_item_id PK; recommendation_request_id FK; product_id FK; rank_order; recommendation_reason; suitability; selected_for_registration | UQ(recommendation_request_id, product_id); product must exist and be active at generation time |
**5.3 Entity Relationship Diagram**

The ER diagram shows the current implementation scope. One customer can own multiple policies and recommendation requests. A policy contains installments; each installment can have multiple payment transactions. Recommendations reference only products from the product master.

*Figure 1. HealthDekho current-scope database entity relationships*

**5.4 Future-Enhancement Database Tables**

The following tables are reserved for FE-1 and should be added only when AI-Assisted Claim Assessment and Approval is implemented.

| **Future Table** | **Purpose** | **Key Columns** |
| --- | --- | --- |
| insurance_claim | Claim header and submitted claim information. | claim_id PK; claim_number UQ; policy_id FK; customer_id FK; claim_date; claim_amount; description; document_status; claim_status; submitted_at |
| claim_assessment | Separates deterministic validations from AI output. | assessment_id PK; claim_id FK; premium_compliance; policy_validity; document_verified; ai_summary; model_name; model_version; assessed_at |
| claim_decision | Records controlled approval or rejection. | decision_id PK; claim_id FK; final_status; decision_reasons; processing_user_id; decision_timestamp |
**5.5 Sample Data**

The following illustrative records are internally consistent and may be used for database seeding, API demonstrations and functional testing. Password values are placeholders and must be replaced with BCrypt hashes in implementation.

**5.5.1 customer**

| **customer_id** | **email** | **password_hash** | **full_name** | **date_of_birth** | **phone** | **status** |
| --- | --- | --- | --- | --- | --- | --- |
| 1001 | aarav.sharma@example.com | {bcrypt}$2a$10$sample01 | Aarav Sharma | 1990-04-15 | 9876501001 | ACTIVE |
| 1002 | meera.iyer@example.com | {bcrypt}$2a$10$sample02 | Meera Iyer | 1985-09-22 | 9876501002 | ACTIVE |
| 1003 | rohan.verma@example.com | {bcrypt}$2a$10$sample03 | Rohan Verma | 2000-01-10 | 9876501003 | ACTIVE |
**5.5.2 insurance_product**

| **product_id** | **product_code** | **policy_name** | **policy_type** | **term_months** | **premium_frequency** | **premium_amount** | **sum_insured** | **age_range** | **active** |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 201 | HD-TERM-001 | HealthDekho Secure Term | TERM | 240 | MONTHLY | 1250.00 | 5000000.00 | 18-60 | TRUE |
| 202 | HD-HEALTH-001 | HealthDekho Family Health | HEALTH | 12 | YEARLY | 18000.00 | 1000000.00 | 18-65 | TRUE |
| 203 | HD-SAV-001 | HealthDekho Smart Savings | SAVINGS | 120 | QUARTERLY | 7500.00 | 1500000.00 | 21-55 | TRUE |
**5.5.3 customer_policy**

| **policy_id** | **policy_number** | **customer_id** | **product_id** | **start_date** | **maturity_date** | **frequency** | **premium_amount** | **sum_insured** | **total_premium** | **status** |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 3001 | HDP-2026-0001 | 1001 | 201 | 2026-11-20 | 2046-11-19 | MONTHLY | 1250.00 | 5000000.00 | 300000.00 | ACTIVE |
| 3002 | HDP-2026-0002 | 1002 | 202 | 2026-11-20 | 2027-11-19 | YEARLY | 18000.00 | 1000000.00 | 18000.00 | ACTIVE |
**5.5.4 premium_installment**

| **installment_id** | **policy_id** | **installment_no** | **due_date** | **grace_end_date** | **scheduled_amount** | **paid_amount** | **payment_status** |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 4001 | 3001 | 1 | 2026-11-20 | 2026-12-20 | 1250.00 | 1250.00 | PAID |
| 4002 | 3001 | 2 | 2026-12-20 | 2027-01-20 | 1250.00 | 500.00 | PARTIALLY_PAID |
| 4003 | 3001 | 3 | 2027-01-20 | 2027-02-20 | 1250.00 | 0.00 | PENDING |
| 4004 | 3002 | 1 | 2026-11-20 | 2026-12-20 | 18000.00 | 18000.00 | PAID |
**5.5.5 premium_payment**

| **payment_id** | **installment_id** | **transaction_reference** | **payment_date** | **amount_paid** | **penalty_amount** | **payment_method** |
| --- | --- | --- | --- | --- | --- | --- |
| 5001 | 4001 | TXN-HD-000001 | 2026-11-20 | 1250.00 | 0.00 | UPI |
| 5002 | 4002 | TXN-HD-000002 | 2026-12-20 | 500.00 | 0.00 | NET_BANKING |
| 5003 | 4004 | TXN-HD-000003 | 2026-11-20 | 18000.00 | 0.00 | CARD |
**5.5.6 ai_recommendation_request**

| **request_id** | **customer_id** | **insurance_goals** | **preferred_term** | **preferred_type** | **calculated_age** | **model_name** | **model_version** | **prompt_version** | **requested_at** |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 6001 | 1001 | Family income protection | 20 years | TERM | 36 | configured-chat-model | deployment-v1 | rec-v1 | 2026-11-20 10:15:00+05:30 |
| 6002 | 1003 | Health coverage for self and parents | 1 year | HEALTH | 26 | configured-chat-model | deployment-v1 | rec-v1 | 2026-11-20 10:30:00+05:30 |
**5.5.7 ai_recommendation_item**

| **item_id** | **request_id** | **product_id** | **rank_order** | **recommendation_reason** | **suitability** | **selected** |
| --- | --- | --- | --- | --- | --- | --- |
| 7001 | 6001 | 201 | 1 | Long-term protection aligned with income-replacement goal | HIGH | TRUE |
| 7002 | 6001 | 203 | 2 | Savings option supports a secondary wealth goal | MEDIUM | FALSE |
| 7003 | 6002 | 202 | 1 | Annual family health cover matches stated preference | HIGH | FALSE |
**5.5.8 Future-Enhancement Sample Data**

The following records are examples only for FE-1 and must not be loaded as part of the current assessment unless the future claim capability is implemented.

**insurance_claim**

| **claim_id** | **claim_number** | **policy_id** | **customer_id** | **claim_date** | **claim_amount** | **document_status** | **claim_status** |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 8001 | CLM-2027-0001 | 3002 | 1002 | 2027-03-15 | 85000.00 | AVAILABLE | PENDING_REVIEW |
**claim_assessment**

| **assessment_id** | **claim_id** | **premium_compliance** | **policy_validity** | **document_verified** | **ai_summary** | **model_version** |
| --- | --- | --- | --- | --- | --- | --- |
| 8101 | 8001 | COMPLIANT | VALID | TRUE | Policy and payment checks passed; manual review required. | deployment-v1 |
**claim_decision**

| **decision_id** | **claim_id** | **final_status** | **decision_reasons** | **processing_user_id** | **decision_timestamp** |
| --- | --- | --- | --- | --- | --- |
| 8201 | 8001 | APPROVED | All mandatory deterministic conditions satisfied. | OPS-USER-101 | 2027-03-16 14:30:00+05:30 |

