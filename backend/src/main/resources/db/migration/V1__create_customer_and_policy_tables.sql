CREATE TABLE customer_account (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    date_of_birth DATE NOT NULL,
    phone VARCHAR(30),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE insurance_product (
    id UUID PRIMARY KEY,
    product_code VARCHAR(40) NOT NULL UNIQUE,
    policy_name VARCHAR(160) NOT NULL,
    policy_type VARCHAR(40) NOT NULL,
    term_months INTEGER NOT NULL CHECK (term_months > 0),
    premium_frequency VARCHAR(20) NOT NULL,
    premium_amount NUMERIC(19, 2) NOT NULL CHECK (premium_amount > 0),
    sum_insured NUMERIC(19, 2) NOT NULL CHECK (sum_insured > 0),
    min_entry_age INTEGER NOT NULL CHECK (min_entry_age >= 0),
    max_entry_age INTEGER NOT NULL CHECK (max_entry_age >= min_entry_age),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_insurance_product_active ON insurance_product (active);

CREATE TABLE customer_policy (
    id UUID PRIMARY KEY,
    policy_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES customer_account (id),
    product_id UUID NOT NULL REFERENCES insurance_product (id),
    term_months INTEGER NOT NULL CHECK (term_months > 0),
    start_date DATE NOT NULL,
    maturity_date DATE NOT NULL,
    premium_frequency VARCHAR(20) NOT NULL,
    premium_amount NUMERIC(19, 2) NOT NULL CHECK (premium_amount > 0),
    sum_insured NUMERIC(19, 2) NOT NULL CHECK (sum_insured > 0),
    total_premium_payable NUMERIC(19, 2) NOT NULL CHECK (total_premium_payable > 0),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customer_policy_product UNIQUE (customer_id, product_id)
);

CREATE INDEX idx_customer_policy_customer_created
    ON customer_policy (customer_id, created_at DESC);
