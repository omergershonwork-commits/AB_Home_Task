package com.ab.orders.processing;

import com.ab.orders.domain.CanonicalOrder;
import com.ab.orders.domain.CountryInfo;
import com.ab.orders.domain.ProcessedOrder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Validates a canonical order, enriches it, and calculates its final total value. */
@Component
public class OrderProcessor {

    private final CountryResolver countryResolver;

    public OrderProcessor(CountryResolver countryResolver) {
        this.countryResolver = countryResolver;
    }

    public ProcessedOrder process(CanonicalOrder order) {
        validate(order);

        CountryInfo country = countryResolver.resolve(order.countryCode());
        BigDecimal total = order.unitPrice().multiply(BigDecimal.valueOf(order.quantity()));

        return new ProcessedOrder(
                order.orderReference(),
                new ProcessedOrder.Customer(
                        order.customerReference(),
                        order.customerFullName(),
                        country.name()
                ),
                order.orderTimestamp(),
                new ProcessedOrder.Product(
                        order.productCode(),
                        order.quantity(),
                        order.unitPrice()
                ),
                total,
                country.currency()
        );
    }

    private void validate(CanonicalOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Order is required");
        }
        if (isBlank(order.orderReference())) {
            throw new IllegalArgumentException("Order reference is required");
        }
        if (isBlank(order.customerReference())) {
            throw new IllegalArgumentException("Customer reference is required");
        }
        if (isBlank(order.customerFullName())) {
            throw new IllegalArgumentException("Customer name is required");
        }
        if (isBlank(order.countryCode())) {
            throw new IllegalArgumentException("Country code is required");
        }
        if (isBlank(order.productCode())) {
            throw new IllegalArgumentException("Product code is required");
        }
        if (order.quantity() == null || order.quantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (order.unitPrice() == null || order.unitPrice().signum() < 0) {
            throw new IllegalArgumentException("Unit price must not be negative");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
