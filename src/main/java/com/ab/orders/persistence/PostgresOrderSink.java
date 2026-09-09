package com.ab.orders.persistence;

import com.ab.orders.domain.ProcessedOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** Stores processed orders in PostgreSQL using configurable JDBC batches. */
@Component
@RequiredArgsConstructor
public class PostgresOrderSink implements TargetOrderSink {

    private static final String INSERT_SQL = """
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
            """;

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.persistence.batch-size:100}")
    private int batchSize;

    @Override
    public void saveAll(List<TargetOrder> orders) {
        if (orders.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                INSERT_SQL,
                orders,
                batchSize,
                (statement, targetOrder) -> {
                    ProcessedOrder order = targetOrder.order();

                    statement.setString(1, targetOrder.sourceSystem());
                    statement.setString(2, order.orderReference());
                    statement.setString(3, order.customer().customerReference());
                    statement.setString(4, order.customer().fullName());
                    statement.setString(5, order.customer().country());
                    statement.setString(6, order.currency());
                    statement.setObject(7, order.orderTimestamp());
                    statement.setString(8, order.product().code());
                    statement.setInt(9, order.product().quantity());
                    statement.setBigDecimal(10, order.product().unitPrice());
                    statement.setBigDecimal(11, order.totalOrderValue());
                }
        );
    }
}
