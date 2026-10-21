
ALTER TABLE customers
    ADD COLUMN event_uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE;

ALTER TABLE customer_interactions
    ADD COLUMN customer_uuid UUID;

UPDATE customer_interactions ci
SET customer_uuid = c.event_uuid
    FROM customers c
WHERE ci.customer_id = c.id;

ALTER TABLE customer_interactions
    ALTER COLUMN customer_uuid SET NOT NULL;

ALTER TABLE customer_interactions
    ADD CONSTRAINT fk_interactions_customer_uuid
        FOREIGN KEY (customer_uuid) REFERENCES customers(event_uuid);
