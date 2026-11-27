BEGIN;

INSERT INTO customers
(customer_name, date_of_birth, email, phone_number, address)
VALUES
('Rahul Sharma', '1990-05-15', 'rahul@gmail.com', '9876543210', 'Delhi'),
('Priya Verma', '1985-08-20', 'priya@gmail.com', '9876543211', 'Noida'),
('Amit Kumar', '1995-02-10', 'amit@gmail.com', '9876543212', 'Gurgaon'),
('Neha Singh', '1992-11-25', 'neha@gmail.com', '9876543213', 'Lucknow'),
('Vikas Mehta', '1988-03-12', 'vikas@gmail.com', '9876543214', 'Jaipur')
ON CONFLICT (email) DO NOTHING;

INSERT INTO insurance_products
(product_code, policy_name, policy_type, policy_term_years,
 sum_insured, premium_frequency, premium_amount,
 min_entry_age, max_entry_age, active)
VALUES
('LIFE001', 'Secure Life Plan', 'LIFE', 10,
 1000000, 'MONTHLY', 850, 18, 60, TRUE),

('HEALTH001', 'Health Plus Plan', 'HEALTH', 5,
 500000, 'MONTHLY', 1200, 18, 65, TRUE),

('MOTOR001', 'Motor Shield Plan', 'MOTOR', 1,
 500000, 'MONTHLY', 1500, 18, 80, TRUE),

('HEALTH002', 'Family Health Plan', 'HEALTH', 5,
 1000000, 'MONTHLY', 2000, 18, 65, TRUE)
ON CONFLICT (product_code) DO NOTHING;

 INSERT INTO customer_policies
(policy_number, customer_id, product_id,
 policy_start_date, policy_maturity_date,
 policy_term_years, premium_frequency,
 premium_amount, sum_insured,
 total_premium_payable, policy_status)
SELECT
    'HDK-2026-1001',
    c.customer_id,
    p.product_id,
    DATE '2026-01-01',
    DATE '2031-01-01',
    5,
    'MONTHLY',
    1200,
    500000,
    72000,
    'ACTIVE'
FROM customers c
JOIN insurance_products p
    ON p.product_code = 'HEALTH001'
WHERE c.email = 'rahul@gmail.com'

UNION ALL

SELECT
    'HDK-2026-1002',
    c.customer_id,
    p.product_id,
    DATE '2026-03-01',
    DATE '2036-03-01',
    10,
    'MONTHLY',
    850,
    1000000,
    102000,
    'ACTIVE'
FROM customers c
JOIN insurance_products p
    ON p.product_code = 'LIFE001'
WHERE c.email = 'priya@gmail.com'

UNION ALL

SELECT
    'HDK-2026-1003',
    c.customer_id,
    p.product_id,
    DATE '2026-06-01',
    DATE '2027-06-01',
    1,
    'MONTHLY',
    1500,
    500000,
    18000,
    'ACTIVE'
FROM customers c
JOIN insurance_products p
    ON p.product_code = 'MOTOR001'
WHERE c.email = 'amit@gmail.com'
ON CONFLICT DO NOTHING;

INSERT INTO premium_installments
(policy_id, installment_number, due_date,
 grace_period_end_date, premium_amount,
 penalty_amount, amount_paid, payment_status)

SELECT
    cp.policy_id,
    gs.installment_number,

    cp.policy_start_date +
        ((gs.installment_number - 1) *
            CASE cp.premium_frequency
                WHEN 'MONTHLY' THEN INTERVAL '1 month'
                WHEN 'QUARTERLY' THEN INTERVAL '3 months'
                WHEN 'HALF_YEARLY' THEN INTERVAL '6 months'
                WHEN 'YEARLY' THEN INTERVAL '1 year'
            END
        ) AS due_date,

    (
        cp.policy_start_date +
        ((gs.installment_number - 1) *
            CASE cp.premium_frequency
                WHEN 'MONTHLY' THEN INTERVAL '1 month'
                WHEN 'QUARTERLY' THEN INTERVAL '3 months'
                WHEN 'HALF_YEARLY' THEN INTERVAL '6 months'
                WHEN 'YEARLY' THEN INTERVAL '1 year'
            END
        ) + INTERVAL '1 month'
    )::DATE AS grace_period_end_date,

    cp.premium_amount,
    0,
    0,

    CASE
        WHEN CURRENT_DATE >
            (
                cp.policy_start_date +
                ((gs.installment_number - 1) *
                    CASE cp.premium_frequency
                        WHEN 'MONTHLY' THEN INTERVAL '1 month'
                        WHEN 'QUARTERLY' THEN INTERVAL '3 months'
                        WHEN 'HALF_YEARLY' THEN INTERVAL '6 months'
                        WHEN 'YEARLY' THEN INTERVAL '1 year'
                    END
                ) + INTERVAL '1 month'
            )::DATE
        THEN 'OVERDUE'
        ELSE 'PENDING'
    END

FROM customer_policies cp

CROSS JOIN LATERAL generate_series(
    1,
    cp.policy_term_years *
        CASE cp.premium_frequency
            WHEN 'MONTHLY' THEN 12
            WHEN 'QUARTERLY' THEN 4
            WHEN 'HALF_YEARLY' THEN 2
            WHEN 'YEARLY' THEN 1
        END
    ) AS gs(installment_number)
ON CONFLICT (policy_id, installment_number) DO NOTHING;

INSERT INTO premium_payments
(installment_id, receipt_number, payment_date,
 premium_paid, penalty_paid,
 payment_method, transaction_reference)

SELECT
    pi.installment_id,
    'REC-1001',
    DATE '2026-01-01',
    1200,
    0,
    'UPI',
    'TXN-1001'
FROM premium_installments pi
JOIN customer_policies cp
    ON cp.policy_id = pi.policy_id
WHERE cp.policy_number = 'HDK-2026-1001'
AND pi.installment_number = 1

UNION ALL

SELECT
    pi.installment_id,
    'REC-1002',
    DATE '2026-02-01',
    1200,
    0,
    'CARD',
    'TXN-1002'
FROM premium_installments pi
JOIN customer_policies cp
    ON cp.policy_id = pi.policy_id
WHERE cp.policy_number = 'HDK-2026-1001'
AND pi.installment_number = 2

UNION ALL

SELECT
    pi.installment_id,
    'REC-1003',
    DATE '2026-04-10',
    850,
    85,
    'UPI',
    'TXN-1003'
FROM premium_installments pi
JOIN customer_policies cp
    ON cp.policy_id = pi.policy_id
WHERE cp.policy_number = 'HDK-2026-1002'
AND pi.installment_number = 1
ON CONFLICT (transaction_reference) DO NOTHING;

-- Generated by GitHub Copilot - Nov-03-2026
UPDATE premium_installments pi
SET amount_paid = totals.total_paid,
    penalty_amount = totals.total_penalty,
    payment_status = CASE
        WHEN totals.total_paid >= pi.premium_amount + totals.total_penalty THEN 'PAID'
        ELSE 'PARTIALLY_PAID'
    END
FROM (
    SELECT installment_id, SUM(premium_paid + penalty_paid) AS total_paid,
           SUM(penalty_paid) AS total_penalty
    FROM premium_payments
    GROUP BY installment_id
) totals
WHERE pi.installment_id = totals.installment_id;

INSERT INTO policy_recommendations
(customer_id, product_id, customer_goal,
 customer_age, recommendation_reason,
 suitability_description, ai_model)

SELECT
    c.customer_id,
    p.product_id,
    'Looking for additional family health coverage',
    36,
    'Family health coverage is suitable based on the stated goal.',
    'Recommended based on the customer goal and available product information.',
    'Spring AI - GPT'

FROM customers c
JOIN insurance_products p
    ON p.product_code = 'HEALTH002'
WHERE c.email = 'rahul@gmail.com'
AND NOT EXISTS (
    SELECT 1 FROM policy_recommendations r
    WHERE r.customer_id = c.customer_id AND r.product_id = p.product_id
        AND r.customer_goal = 'Looking for additional family health coverage'
)

UNION ALL

SELECT
    c.customer_id,
    p.product_id,
    'Looking for long-term financial protection',
    41,
    'Life insurance provides financial protection over the selected policy term.',
    'Recommended based on the customer goal and available product information.',
    'Spring AI - GPT'

FROM customers c
JOIN insurance_products p
    ON p.product_code = 'LIFE001'
WHERE c.email = 'priya@gmail.com'
AND NOT EXISTS (
    SELECT 1 FROM policy_recommendations r
    WHERE r.customer_id = c.customer_id AND r.product_id = p.product_id
        AND r.customer_goal = 'Looking for long-term financial protection'
)

UNION ALL

SELECT
    c.customer_id,
    p.product_id,
    'Looking for financial security for dependents',
    33,
    'Life insurance may support the stated financial protection goal.',
    'Recommended based on the customer goal and available product information.',
    'Spring AI - GPT'

FROM customers c
JOIN insurance_products p
    ON p.product_code = 'LIFE001'
WHERE c.email = 'neha@gmail.com'
AND NOT EXISTS (
    SELECT 1 FROM policy_recommendations r
    WHERE r.customer_id = c.customer_id AND r.product_id = p.product_id
        AND r.customer_goal = 'Looking for financial security for dependents'
);

INSERT INTO insurance_claims
(claim_number, policy_id, customer_id,
 claim_date, claim_amount, claim_description,
 documents_attached, document_status,
 premium_compliance_status, ai_assessment_summary,
 claim_status, decision_reason,
 approver_id, decision_timestamp)

SELECT
    'CLM-1001',
    cp.policy_id,
    c.customer_id,
    DATE '2026-02-15',
    25000,
    'Medical hospitalization claim for treatment expenses.',
    TRUE,
    'VERIFIED',
    'COMPLIANT',
    'AI assessment indicates that the policy was active and premium payments were compliant at the time of claim.',
    'APPROVED',
    'Claim approved after policy validity, premium compliance and document checks.',
    'ADMIN001',
    TIMESTAMP '2026-02-20 11:30:00'
FROM customers c
JOIN customer_policies cp
    ON cp.customer_id = c.customer_id
WHERE c.email = 'rahul@gmail.com'
AND cp.policy_number = 'HDK-2026-1001'

UNION ALL

SELECT
    'CLM-1002',
    cp.policy_id,
    c.customer_id,
    DATE '2026-04-15',
    50000,
    'Medical claim submitted for hospitalization expenses.',
    FALSE,
    'PENDING',
    'NON_COMPLIANT',
    'AI assessment identified premium compliance concerns. The claim requires review according to the defined business rules.',
    'REJECTED',
    'Claim rejected due to non-compliance with premium payment requirements.',
    'ADMIN001',
    TIMESTAMP '2026-04-18 14:30:00'
FROM customers c
JOIN customer_policies cp
    ON cp.customer_id = c.customer_id
WHERE c.email = 'priya@gmail.com'
AND cp.policy_number = 'HDK-2026-1002'
ON CONFLICT (claim_number) DO NOTHING;

COMMIT;