UPDATE insurance_product
SET policy_name = 'HealthDekho Secure Term',
    policy_type = 'TERM',
    term_months = 240,
    premium_frequency = 'MONTHLY',
    premium_amount = 1250.00,
    sum_insured = 5000000.00,
    min_entry_age = 18,
    max_entry_age = 60,
    active = TRUE
WHERE product_code = 'HD-TERM-001'
  AND policy_name = 'Family Term Cover';

UPDATE insurance_product
SET policy_name = 'HealthDekho Family Health',
    policy_type = 'HEALTH',
    term_months = 12,
    premium_frequency = 'YEARLY',
    premium_amount = 18000.00,
    sum_insured = 1000000.00,
    min_entry_age = 18,
    max_entry_age = 65,
    active = TRUE
WHERE product_code = 'HD-HEALTH-001'
  AND policy_name = 'Essential Health Shield';

UPDATE insurance_product
SET product_code = 'HD-SAV-001',
    policy_name = 'HealthDekho Smart Savings',
    policy_type = 'SAVINGS',
    term_months = 120,
    premium_frequency = 'QUARTERLY',
    premium_amount = 7500.00,
    sum_insured = 1500000.00,
    min_entry_age = 21,
    max_entry_age = 55,
    active = TRUE
WHERE product_code = 'HD-FAMILY-001'
  AND policy_name = 'Family Secure Plan';
