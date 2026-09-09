package com.ab.orders.normalization;

import com.ab.orders.domain.CanonicalOrder;
import com.ab.orders.dtos.b.SourceBOrderDto;
import org.springframework.stereotype.Component;

@Component
public class SourceBOrderNormalizer implements OrderNormalizer<SourceBOrderDto> {

    @Override
    public CanonicalOrder normalize(SourceBOrderDto order) {
        return new CanonicalOrder(
                "SOURCE_B",
                order.orderNumber(),
                order.customer().id(),
                order.customer().firstName() + " " + order.customer().lastName(),
                order.customer().countryCode(),
                order.createdAt(),
                order.item().sku(),
                order.item().units(),
                order.item().price()
        );
    }
}
