package com.ab.orders.persistence;

import com.ab.orders.domain.ProcessedOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
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
    private final PlatformTransactionManager transactionManager;

    @Value("${app.persistence.batch-size:100}")
    private int batchSize;

    @Override
    public void saveAll(List<TargetOrder> orders) {
        if (orders.isEmpty()) {
            return;
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Persistence batch size must be greater than zero");
        }

        for (int start = 0; start < orders.size(); start += batchSize) {
            int end = Math.min(start + batchSize, orders.size());
            persistRangeOrIsolate(orders, start, end);
        }
    }

    /**
     * Attempts the full range first. If PostgreSQL rejects the range because of deterministic
     * row data, recursively split the range until the first rejected row is isolated.
     *
     * Infrastructure/transient failures are deliberately rethrown so Kafka retries the
     * uncommitted work instead of treating one order as a poison record.
     */
    private void persistRangeOrIsolate(List<TargetOrder> orders, int start, int end) {
        try {
            executeBatch(orders.subList(start, end));
        } catch (DataAccessException exception) {
            if (!isDeterministicDataFailure(exception)) {
                throw exception;
            }

            if (end - start == 1) {
                throw new FailedOrderPersistenceException(start, exception);
            }

            int middle = start + (end - start) / 2;

            // Persist the left half first. This keeps successful persistence aligned with the
            // contiguous Kafka prefix before the first failed record.
            persistRangeOrIsolate(orders, start, middle);
            persistRangeOrIsolate(orders, middle, end);
        }
    }

    /**
     * Every probe runs in its own transaction. PostgreSQL marks a transaction as failed after
     * a statement error, so a fresh transaction is required before testing a smaller sub-range.
     */
    private void executeBatch(List<TargetOrder> orders) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        transaction.executeWithoutResult(status -> jdbcTemplate.batchUpdate(
                INSERT_SQL,
                orders,
                orders.size(),
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
        ));
    }

    private boolean isDeterministicDataFailure(DataAccessException exception) {
        if (exception instanceof DataIntegrityViolationException) {
            return true;
        }

        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SQLException sqlException && hasDeterministicSqlState(sqlException)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private boolean hasDeterministicSqlState(SQLException exception) {
        SQLException current = exception;
        while (current != null) {
            String sqlState = current.getSQLState();
            if (sqlState != null && (sqlState.startsWith("22") || sqlState.startsWith("23"))) {
                // 22xxx = data exception, 23xxx = integrity constraint violation.
                return true;
            }
            current = current.getNextException();
        }
        return false;
    }
}
