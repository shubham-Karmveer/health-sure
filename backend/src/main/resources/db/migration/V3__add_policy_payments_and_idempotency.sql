ALTER TABLE premium_installment
    ADD COLUMN penalty_amount NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (penalty_amount >= 0),
    ADD COLUMN paid_penalty_amount NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (paid_penalty_amount >= 0),
    ADD CONSTRAINT ck_installment_paid_within_scheduled CHECK (paid_amount <= scheduled_amount),
    ADD CONSTRAINT ck_installment_penalty_paid_within_assessed
        CHECK (paid_penalty_amount <= penalty_amount);

CREATE TABLE payment_idempotency (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer_account (id),
    policy_id UUID NOT NULL REFERENCES customer_policy (id),
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_payment_idempotency_customer_key UNIQUE (customer_id, idempotency_key)
);

CREATE TABLE premium_payment (
    id UUID PRIMARY KEY,
    installment_id UUID NOT NULL REFERENCES premium_installment (id),
    idempotency_id UUID NOT NULL REFERENCES payment_idempotency (id),
    transaction_reference VARCHAR(60) NOT NULL UNIQUE,
    amount_paid NUMERIC(19, 2) NOT NULL CHECK (amount_paid > 0),
    premium_amount NUMERIC(19, 2) NOT NULL CHECK (premium_amount >= 0),
    penalty_amount NUMERIC(19, 2) NOT NULL CHECK (penalty_amount >= 0),
    payment_method VARCHAR(40) NOT NULL,
    payment_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_payment_amount_allocation
        CHECK (amount_paid = premium_amount + penalty_amount),
    CONSTRAINT uq_payment_batch_installment UNIQUE (idempotency_id, installment_id)
);

CREATE INDEX idx_premium_payment_installment_date
    ON premium_payment (installment_id, payment_date DESC);
