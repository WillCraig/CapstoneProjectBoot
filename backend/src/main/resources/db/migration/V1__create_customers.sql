CREATE TABLE customers (
                           id BIGSERIAL PRIMARY KEY,
                           customer_id VARCHAR(50) NOT NULL UNIQUE,
                           full_name VARCHAR(150) NOT NULL,
                           email VARCHAR(255) NOT NULL UNIQUE,
                           status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                           created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);