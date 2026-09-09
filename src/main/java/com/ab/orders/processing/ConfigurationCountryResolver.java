package com.ab.orders.processing;

import com.ab.orders.config.CountryProperties;
import com.ab.orders.domain.CountryInfo;
import org.springframework.stereotype.Component;

/** Resolves country data from the configured in-memory reference map. */
@Component
public class ConfigurationCountryResolver implements CountryResolver {

    private final CountryProperties properties;

    public ConfigurationCountryResolver(CountryProperties properties) {
        this.properties = properties;
    }

    @Override
    public CountryInfo resolve(String countryCode) {
        CountryProperties.Country country = properties.getCountries().get(countryCode);
        if (country == null) {
            throw new IllegalArgumentException("Unsupported country code: " + countryCode);
        }
        return new CountryInfo(country.getName(), country.getCurrency());
    }
}
