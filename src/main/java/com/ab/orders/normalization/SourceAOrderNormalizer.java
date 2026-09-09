package com.ab.orders.normalization;

import com.ab.orders.domain.CanonicalOrder;
import com.ab.orders.dtos.a.SourceAOrderDto;
import org.springframework.stereotype.Component;

@Component
public class SourceAOrderNormalizer implements OrderNormalizer<SourceAOrderDto> {

    @Override
    public CanonicalOrder normalize(SourceAOrderDto order) {
        return new CanonicalOrder(
                "SOURCE_A",
                order.orderId(),
                order.customerId(),
                order.customerName(),
                order.country(),
                order.orderDate(),
                order.productCode(),
                order.quantity(),
                order.unitPrice()
        );
    }
}
