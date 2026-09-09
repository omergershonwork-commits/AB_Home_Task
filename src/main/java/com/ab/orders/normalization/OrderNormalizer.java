package com.ab.orders.normalization;

import com.ab.orders.domain.CanonicalOrder;

public interface OrderNormalizer<T> {
    CanonicalOrder normalize(T order);
}
