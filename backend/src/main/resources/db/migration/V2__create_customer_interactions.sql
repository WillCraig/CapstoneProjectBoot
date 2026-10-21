
CREATE TABLE customer_interactions (
                                       interaction_id UUID PRIMARY KEY,
                                       customer_id BIGINT NOT NULL,
                                       channel VARCHAR(20) NOT NULL,
                                       summary VARCHAR(2000) NOT NULL,
                                       created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       version BIGINT NOT NULL DEFAULT 0,

                                       CONSTRAINT fk_customer_interactions_customer
                                           FOREIGN KEY (customer_id)
                                               REFERENCES customers(id),

                                       CONSTRAINT ck_customer_interactions_channel
                                           CHECK (channel IN ('PHONE', 'EMAIL', 'CHAT'))
);

CREATE INDEX idx_customer_interactions_customer_created
    ON customer_interactions (customer_id, created_at DESC);

INSERT INTO customers (customer_id, full_name, email, status)
VALUES
    ('CUS-1001', 'Amina Khan', 'amina.khan@example.test', 'ACTIVE'),
    ('CUS-1002', 'Ravi Singh', 'ravi.singh@example.test', 'PROSPECT');
