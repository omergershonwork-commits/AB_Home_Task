CREATE TABLE IF NOT EXISTS processed_orders (
    id BIGSERIAL PRIMARY KEY,
    source_system VARCHAR(32) NOT NULL,
    order_reference VARCHAR(128) NOT NULL,
    customer_reference VARCHAR(128) NOT NULL,
    customer_full_name VARCHAR(255) NOT NULL,
    country VARCHAR(128) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    order_timestamp TIMESTAMP NOT NULL,
    product_code VARCHAR(128) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    total_order_value NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_processed_orders_source_reference
        UNIQUE (source_system, order_reference)
);
