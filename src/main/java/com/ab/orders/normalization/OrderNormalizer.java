package com.ab.orders.normalization;

import com.ab.orders.domain.CanonicalOrder;

/** Converts a source-specific order DTO into the shared canonical order model. */
public interface OrderNormalizer<T> {
    CanonicalOrder normalize(T order);
}
