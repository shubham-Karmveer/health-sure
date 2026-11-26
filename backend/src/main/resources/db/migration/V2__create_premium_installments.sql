ALTER TABLE insurance_product
    ADD CONSTRAINT ck_insurance_product_premium_frequency
        CHECK (premium_frequency IN ('MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY')),
    ADD CONSTRAINT ck_insurance_product_term_matches_frequency
        CHECK (
            (premium_frequency = 'MONTHLY' AND term_months % 1 = 0)
            OR (premium_frequency = 'QUARTERLY' AND term_months % 3 = 0)
            OR (premium_frequency = 'HALF_YEARLY' AND term_months % 6 = 0)
            OR (premium_frequency = 'YEARLY' AND term_months % 12 = 0)
        );

CREATE TABLE premium_installment (
    id UUID PRIMARY KEY,
    policy_id UUID NOT NULL REFERENCES customer_policy (id) ON DELETE CASCADE,
    installment_number INTEGER NOT NULL CHECK (installment_number > 0),
    due_date DATE NOT NULL,
    grace_period_end_date DATE NOT NULL,
    scheduled_amount NUMERIC(19, 2) NOT NULL CHECK (scheduled_amount > 0),
    paid_amount NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (payment_status IN ('PENDING', 'PARTIALLY_PAID', 'PAID', 'OVERDUE')),
    CONSTRAINT ck_installment_grace_period_after_due_date
        CHECK (grace_period_end_date >= due_date),
    CONSTRAINT uq_installment_policy_number UNIQUE (policy_id, installment_number)
);

CREATE INDEX idx_premium_installment_policy_due_date
    ON premium_installment (policy_id, due_date);
