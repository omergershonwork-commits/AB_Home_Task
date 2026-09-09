package com.ab.orders.processing;

import com.ab.orders.config.CountryProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurationCountryResolverTest {

    @Test
    void resolvesConfiguredCountries() {
        ConfigurationCountryResolver resolver = resolver();

        assertThat(resolver.resolve("US").name()).isEqualTo("United States");
        assertThat(resolver.resolve("US").currency()).isEqualTo("USD");
        assertThat(resolver.resolve("GB").currency()).isEqualTo("GBP");
        assertThat(resolver.resolve("DE").currency()).isEqualTo("EUR");
    }

    @Test
    void rejectsUnknownCountry() {
        assertThatThrownBy(() -> resolver().resolve("FR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FR");
    }

    private ConfigurationCountryResolver resolver() {
        CountryProperties.Country us = country("United States", "USD");
        CountryProperties.Country gb = country("United Kingdom", "GBP");
        CountryProperties.Country de = country("Germany", "EUR");

        CountryProperties properties = new CountryProperties();
        properties.setCountries(Map.of("US", us, "GB", gb, "DE", de));
        return new ConfigurationCountryResolver(properties);
    }

    private CountryProperties.Country country(String name, String currency) {
        CountryProperties.Country country = new CountryProperties.Country();
        country.setName(name);
        country.setCurrency(currency);
        return country;
    }
}
