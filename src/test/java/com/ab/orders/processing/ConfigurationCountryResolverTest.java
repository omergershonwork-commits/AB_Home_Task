package com.ab.orders.processing;

import com.ab.orders.config.CountryProperties;
import com.ab.orders.domain.CountryInfo;
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
        CountryProperties properties = new CountryProperties();
        properties.setCountries(Map.of(
                "US", new CountryInfo("United States", "USD"),
                "GB", new CountryInfo("United Kingdom", "GBP"),
                "DE", new CountryInfo("Germany", "EUR")
        ));
        return new ConfigurationCountryResolver(properties);
    }
}
