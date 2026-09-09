package com.ab.orders.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/** Binds the country reference-data section from application.yml. */
@Component
@ConfigurationProperties(prefix = "reference-data")
public class CountryProperties {

    private Map<String, Country> countries = new HashMap<>();

    public Map<String, Country> getCountries() {
        return countries;
    }

    public void setCountries(Map<String, Country> countries) {
        this.countries = countries;
    }

    public static class Country {
        private String name;
        private String currency;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }
}
