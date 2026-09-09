package com.ab.orders.processing;

import com.ab.orders.domain.CanonicalOrder;
import com.ab.orders.domain.CountryInfo;
import com.ab.orders.domain.ProcessedOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderProcessorTest {

    private final OrderProcessor processor = new OrderProcessor(
            code -> new CountryInfo("United States", "USD")
    );

    @Test
    void enrichesAndCalculatesTotal() {
        ProcessedOrder result = processor.process(order(2, "125.50"));

        assertThat(result.orderReference()).isEqualTo("ORD-10001");
        assertThat(result.customer().country()).isEqualTo("United States");
        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.totalOrderValue()).isEqualByComparingTo("251.00");
        assertThat(result.product().quantity()).isEqualTo(2);
    }

    @Test
    void rejectsInvalidQuantity() {
        assertThatThrownBy(() -> processor.process(order(0, "125.50")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity");
    }

    @Test
    void rejectsNegativePrice() {
        assertThatThrownBy(() -> processor.process(order(2, "-1.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit price");
    }

    private CanonicalOrder order(Integer quantity, String unitPrice) {
        return new CanonicalOrder(
                "SOURCE_A",
                "ORD-10001",
                "CUST-501",
                "John Smith",
                "US",
                LocalDateTime.parse("2026-09-01T10:30:00"),
                "P100",
                quantity,
                new BigDecimal(unitPrice)
        );
    }
}
