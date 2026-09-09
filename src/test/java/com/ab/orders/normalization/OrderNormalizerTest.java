package com.ab.orders.normalization;

import com.ab.orders.domain.CanonicalOrder;
import com.ab.orders.dtos.a.SourceAOrderDto;
import com.ab.orders.dtos.b.SourceBOrderDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderNormalizerTest {

    @Test
    void normalizesSourceA() {
        SourceAOrderDto input = new SourceAOrderDto(
                "ORD-10001", "CUST-501", "John Smith", "US",
                LocalDateTime.parse("2026-09-01T10:30:00"),
                "P100", 2, new BigDecimal("125.50")
        );

        CanonicalOrder result = new SourceAOrderNormalizer().normalize(input);

        assertThat(result.sourceSystem()).isEqualTo("SOURCE_A");
        assertThat(result.orderReference()).isEqualTo("ORD-10001");
        assertThat(result.customerFullName()).isEqualTo("John Smith");
        assertThat(result.countryCode()).isEqualTo("US");
        assertThat(result.unitPrice()).isEqualByComparingTo("125.50");
    }

    @Test
    void normalizesSourceB() {
        SourceBOrderDto input = new SourceBOrderDto(
                "ORD-20001",
                new SourceBOrderDto.CustomerDto("CUST-842", "Jane", "Miller", "DE"),
                LocalDateTime.parse("2026-09-01T11:15:00"),
                new SourceBOrderDto.ItemDto("P200", 3, new BigDecimal("80.00"))
        );

        CanonicalOrder result = new SourceBOrderNormalizer().normalize(input);

        assertThat(result.sourceSystem()).isEqualTo("SOURCE_B");
        assertThat(result.orderReference()).isEqualTo("ORD-20001");
        assertThat(result.customerFullName()).isEqualTo("Jane Miller");
        assertThat(result.countryCode()).isEqualTo("DE");
        assertThat(result.quantity()).isEqualTo(3);
    }
}
