package com.weathermate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openmeteo")
public record OpenMeteoProperties(String forecastUrl, String geocodingUrl) {
}
