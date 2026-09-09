package com.ab.orders.config;

import com.ab.orders.domain.CountryInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/** Binds the country reference-data section from the dedicated country config file. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "reference-data")
public class CountryProperties {

    private Map<String, CountryInfo> countries = new HashMap<>();
}
