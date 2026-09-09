package com.ab.orders.processing;

import com.ab.orders.config.CountryProperties;
import com.ab.orders.domain.CountryInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Resolves country data from the configured in-memory reference map. */
@Component
@RequiredArgsConstructor
public class ConfigurationCountryResolver implements CountryResolver {

    private final CountryProperties properties;

    @Override
    public CountryInfo resolve(String countryCode) {
        CountryInfo country = properties.getCountries().get(countryCode);
        if (country == null) {
            throw new IllegalArgumentException("Unsupported country code: " + countryCode);
        }
        return country;
    }
}
