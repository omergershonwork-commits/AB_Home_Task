package com.ab.orders.persistence;

import com.ab.orders.domain.ProcessedOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Stores processed orders in PostgreSQL. */
@Component
@RequiredArgsConstructor
public class PostgresOrderSink implements TargetOrderSink {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(String sourceSystem, ProcessedOrder order) {
        jdbcTemplate.update("""
                INSERT INTO processed_orders (
                    source_system,
                    order_reference,
                    customer_reference,
                    customer_full_name,
                    country,
                    currency,
                    order_timestamp,
                    product_code,
                    quantity,
                    unit_price,
                    total_order_value
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (source_system, order_reference) DO NOTHING
                """,
                sourceSystem,
                order.orderReference(),
                order.customer().customerReference(),
                order.customer().fullName(),
                order.customer().country(),
                order.currency(),
                order.orderTimestamp(),
                order.product().code(),
                order.product().quantity(),
                order.product().unitPrice(),
                order.totalOrderValue()
        );
    }
}
