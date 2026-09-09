package com.ab.orders.processing;

import com.ab.orders.domain.CountryInfo;

/** Resolves a source country code to its full country name and currency. */
public interface CountryResolver {
    CountryInfo resolve(String countryCode);
}
