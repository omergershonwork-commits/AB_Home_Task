package com.ab.orders.domain;

/** Resolved country name and currency for a source country code. */
public record CountryInfo(
        String name,
        String currency
) {
}
