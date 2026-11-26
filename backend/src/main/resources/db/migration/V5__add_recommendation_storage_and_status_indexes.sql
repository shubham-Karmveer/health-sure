CREATE INDEX idx_customer_account_status
    ON customer_account (status);

CREATE INDEX idx_customer_policy_status
    ON customer_policy (status);

CREATE TABLE ai_recommendation_request (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer_account (id),
    insurance_goals TEXT,
    preferred_term VARCHAR(100),
    preferred_policy_type VARCHAR(40),
    calculated_age INTEGER NOT NULL CHECK (calculated_age >= 0),
    model_name VARCHAR(120) NOT NULL,
    model_version VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(100) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_recommendation_request_customer_requested
    ON ai_recommendation_request (customer_id, requested_at DESC);

CREATE TABLE ai_recommendation_item (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL REFERENCES ai_recommendation_request (id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES insurance_product (id),
    rank_order INTEGER NOT NULL CHECK (rank_order > 0),
    recommendation_reason TEXT NOT NULL,
    suitability VARCHAR(20) NOT NULL,
    selected_for_registration BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_recommendation_request_product UNIQUE (request_id, product_id),
    CONSTRAINT uq_recommendation_request_rank UNIQUE (request_id, rank_order)
);

CREATE INDEX idx_ai_recommendation_item_request_rank
    ON ai_recommendation_item (request_id, rank_order);
