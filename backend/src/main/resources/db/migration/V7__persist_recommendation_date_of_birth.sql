ALTER TABLE ai_recommendation_request
    ADD COLUMN date_of_birth DATE;

UPDATE ai_recommendation_request request
SET date_of_birth = customer.date_of_birth
FROM customer_account customer
WHERE request.customer_id = customer.id;

ALTER TABLE ai_recommendation_request
    ALTER COLUMN date_of_birth SET NOT NULL;
