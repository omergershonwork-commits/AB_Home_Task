package com.ab.orders.persistence;

import com.ab.orders.domain.ProcessedOrder;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class PostgresOrderSinkTest {

    @Test
    void bisectsDeterministicBatchFailureUntilBadOrderIsIsolated() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate("ORD-2", false);
        PostgresOrderSink sink = new PostgresOrderSink(jdbcTemplate, new TestTransactionManager());
        ReflectionTestUtils.setField(sink, "batchSize", 4);

        FailedOrderPersistenceException exception = catchThrowableOfType(
                () -> sink.saveAll(List.of(
                        targetOrder("ORD-0"),
                        targetOrder("ORD-1"),
                        targetOrder("ORD-2"),
                        targetOrder("ORD-3")
                )),
                FailedOrderPersistenceException.class
        );

        assertThat(exception.failedIndex()).isEqualTo(2);
        assertThat(jdbcTemplate.attempts()).containsExactly(
                List.of("ORD-0", "ORD-1", "ORD-2", "ORD-3"),
                List.of("ORD-0", "ORD-1"),
                List.of("ORD-2", "ORD-3"),
                List.of("ORD-2")
        );
    }

    @Test
    void doesNotBisectInfrastructureFailure() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate(null, true);
        PostgresOrderSink sink = new PostgresOrderSink(jdbcTemplate, new TestTransactionManager());
        ReflectionTestUtils.setField(sink, "batchSize", 4);

        assertThatThrownBy(() -> sink.saveAll(List.of(
                targetOrder("ORD-0"),
                targetOrder("ORD-1")
        )))
                .isInstanceOf(DataAccessResourceFailureException.class);

        assertThat(jdbcTemplate.attempts()).containsExactly(
                List.of("ORD-0", "ORD-1")
        );
    }

    private TargetOrder targetOrder(String orderReference) {
        ProcessedOrder order = new ProcessedOrder(
                orderReference,
                new ProcessedOrder.Customer("CUST-1", "John Smith", "United States"),
                LocalDateTime.parse("2026-09-01T10:30:00"),
                new ProcessedOrder.Product("P100", 1, new BigDecimal("10.00")),
                new BigDecimal("10.00"),
                "USD"
        );
        return new TargetOrder("SOURCE_A", order);
    }

    private static class RecordingJdbcTemplate extends JdbcTemplate {
        private final String rejectedOrderReference;
        private final boolean infrastructureFailure;
        private final List<List<String>> attempts = new ArrayList<>();

        private RecordingJdbcTemplate(String rejectedOrderReference, boolean infrastructureFailure) {
            this.rejectedOrderReference = rejectedOrderReference;
            this.infrastructureFailure = infrastructureFailure;
        }

        @Override
        public <T> int[][] batchUpdate(
                String sql,
                Collection<T> batchArgs,
                int batchSize,
                ParameterizedPreparedStatementSetter<T> pss
        ) throws DataAccessException {
            List<String> references = batchArgs.stream()
                    .map(TargetOrder.class::cast)
                    .map(targetOrder -> targetOrder.order().orderReference())
                    .toList();
            attempts.add(references);

            if (infrastructureFailure) {
                throw new DataAccessResourceFailureException("database unavailable");
            }
            if (rejectedOrderReference != null && references.contains(rejectedOrderReference)) {
                throw new DataIntegrityViolationException("rejected row");
            }

            return new int[][]{new int[batchArgs.size()]};
        }

        private List<List<String>> attempts() {
            return attempts;
        }
    }

    private static class TestTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
