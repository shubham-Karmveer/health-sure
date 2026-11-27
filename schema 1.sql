
CREATE TABLE IF NOT EXISTS customers (
    customer_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    customer_name VARCHAR(100) NOT NULL,

    date_of_birth DATE NOT NULL,

    email VARCHAR(150) UNIQUE NOT NULL,

    phone_number VARCHAR(15) NOT NULL,

    address VARCHAR(250),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_customer_dob
        CHECK (date_of_birth <= CURRENT_DATE)
);

CREATE TABLE IF NOT EXISTS insurance_products (
    product_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    product_code VARCHAR(30) UNIQUE NOT NULL,

    policy_name VARCHAR(150) NOT NULL,

    policy_type VARCHAR(50) NOT NULL,

    policy_term_years INTEGER NOT NULL,

    sum_insured NUMERIC(15,2) NOT NULL,

    premium_frequency VARCHAR(20) NOT NULL,

    premium_amount NUMERIC(15,2) NOT NULL,

    min_entry_age INTEGER DEFAULT 18,

    max_entry_age INTEGER DEFAULT 65,

    active BOOLEAN DEFAULT TRUE,

    CONSTRAINT chk_policy_term
        CHECK (policy_term_years > 0),

    CONSTRAINT chk_sum_insured
        CHECK (sum_insured > 0),

    CONSTRAINT chk_premium_amount
        CHECK (premium_amount > 0),

    CONSTRAINT chk_premium_frequency
        CHECK (premium_frequency IN
        ('MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY')),

    CONSTRAINT chk_entry_age
        CHECK (
            min_entry_age >= 0
            AND max_entry_age >= min_entry_age
        )
);

CREATE TABLE IF NOT EXISTS customer_policies (
    policy_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    policy_number VARCHAR(40) UNIQUE NOT NULL,

    customer_id BIGINT NOT NULL,

    product_id BIGINT NOT NULL,

    policy_start_date DATE NOT NULL,

    policy_maturity_date DATE NOT NULL,

    policy_term_years INTEGER NOT NULL,

    premium_frequency VARCHAR(20) NOT NULL,

    premium_amount NUMERIC(15,2) NOT NULL,

    sum_insured NUMERIC(15,2) NOT NULL,

    total_premium_payable NUMERIC(15,2) NOT NULL,

    policy_status VARCHAR(20) DEFAULT 'ACTIVE',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_policy_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id),

    CONSTRAINT fk_policy_product
        FOREIGN KEY (product_id)
        REFERENCES insurance_products(product_id),

    CONSTRAINT uk_customer_product
        UNIQUE (customer_id, product_id),

    CONSTRAINT chk_policy_dates
        CHECK (policy_maturity_date > policy_start_date),

    CONSTRAINT chk_policy_term
        CHECK (policy_term_years > 0),

    CONSTRAINT chk_policy_premium
        CHECK (premium_amount > 0),

    CONSTRAINT chk_policy_sum_insured
        CHECK (sum_insured > 0),

    CONSTRAINT chk_total_premium
        CHECK (total_premium_payable > 0),

    CONSTRAINT chk_policy_frequency
        CHECK (premium_frequency IN
        ('MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY')),

    CONSTRAINT chk_policy_status
        CHECK (policy_status IN
        ('ACTIVE', 'LAPSED', 'MATURED'))
);
CREATE TABLE IF NOT EXISTS premium_installments (
    installment_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    policy_id BIGINT NOT NULL,

    installment_number INTEGER NOT NULL,

    due_date DATE NOT NULL,

    grace_period_end_date DATE NOT NULL,

    premium_amount NUMERIC(15,2) NOT NULL,

    penalty_amount NUMERIC(15,2) DEFAULT 0,

    amount_paid NUMERIC(15,2) DEFAULT 0,

    payment_status VARCHAR(20) DEFAULT 'PENDING',

    CONSTRAINT fk_installment_policy
        FOREIGN KEY (policy_id)
        REFERENCES customer_policies(policy_id),

    CONSTRAINT uk_policy_installment
        UNIQUE (policy_id, installment_number),

    CONSTRAINT chk_installment_number
        CHECK (installment_number > 0),

    CONSTRAINT chk_installment_dates
        CHECK (grace_period_end_date >= due_date),

    CONSTRAINT chk_installment_premium
        CHECK (premium_amount > 0),

    CONSTRAINT chk_installment_penalty
        CHECK (penalty_amount >= 0),

    CONSTRAINT chk_installment_paid
        CHECK (amount_paid >= 0),

    CONSTRAINT chk_installment_status
        CHECK (payment_status IN
        ('PENDING', 'PARTIALLY_PAID', 'PAID', 'OVERDUE'))
);
CREATE TABLE IF NOT EXISTS premium_payments (
    payment_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    installment_id BIGINT NOT NULL,

    receipt_number VARCHAR(50) NOT NULL,

    payment_date DATE NOT NULL,

    premium_paid NUMERIC(15,2) NOT NULL,

    penalty_paid NUMERIC(15,2) DEFAULT 0,

    payment_method VARCHAR(30),

    transaction_reference VARCHAR(100) UNIQUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_installment
        FOREIGN KEY (installment_id)
        REFERENCES premium_installments(installment_id),

    CONSTRAINT chk_payment_premium
        CHECK (premium_paid > 0),

    CONSTRAINT chk_payment_penalty
        CHECK (penalty_paid >= 0)
);
CREATE TABLE IF NOT EXISTS policy_recommendations (
    recommendation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    customer_id BIGINT NOT NULL,

    product_id BIGINT NOT NULL,

    customer_goal VARCHAR(500),

    customer_age INTEGER NOT NULL,

    recommendation_reason TEXT NOT NULL,

    suitability_description TEXT,

    ai_model VARCHAR(100),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_recommendation_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id),

    CONSTRAINT fk_recommendation_product
        FOREIGN KEY (product_id)
        REFERENCES insurance_products(product_id),

    CONSTRAINT chk_recommendation_age
        CHECK (customer_age >= 0)
);
CREATE TABLE IF NOT EXISTS insurance_claims (
    claim_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    claim_number VARCHAR(50) UNIQUE NOT NULL,

    policy_id BIGINT NOT NULL,

    customer_id BIGINT NOT NULL,

    claim_date DATE NOT NULL,

    claim_amount NUMERIC(15,2) NOT NULL,

    claim_description TEXT NOT NULL,

    documents_attached BOOLEAN DEFAULT FALSE,

    document_status VARCHAR(30) DEFAULT 'PENDING',

    premium_compliance_status VARCHAR(30) DEFAULT 'PENDING',

    ai_assessment_summary TEXT,

    claim_status VARCHAR(30) DEFAULT 'SUBMITTED',

    decision_reason TEXT,

    approver_id VARCHAR(100),

    decision_timestamp TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_claim_policy
        FOREIGN KEY (policy_id)
        REFERENCES customer_policies(policy_id),

    CONSTRAINT fk_claim_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id),

    CONSTRAINT chk_claim_amount
        CHECK (claim_amount > 0),

    CONSTRAINT chk_document_status
        CHECK (document_status IN
        ('PENDING', 'VERIFIED', 'REJECTED')),

    CONSTRAINT chk_premium_compliance
        CHECK (premium_compliance_status IN
        ('PENDING', 'COMPLIANT', 'NON_COMPLIANT')),

    CONSTRAINT chk_claim_status
        CHECK (claim_status IN
        ('SUBMITTED', 'PENDING_DOCUMENTS',
         'PENDING_REVIEW', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED'))
);

ALTER TABLE insurance_claims DROP CONSTRAINT IF EXISTS chk_claim_status;
ALTER TABLE insurance_claims ADD CONSTRAINT chk_claim_status
    CHECK (claim_status IN
    ('SUBMITTED', 'PENDING_DOCUMENTS', 'PENDING_REVIEW',
     'PENDING_APPROVAL', 'APPROVED', 'REJECTED'));