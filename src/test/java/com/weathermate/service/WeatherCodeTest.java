package com.weathermate.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherCodeTest {

    @ParameterizedTest
    @CsvSource({
            "0, CLEAR", "2, PARTLY_CLOUDY", "3, OVERCAST", "45, FOG", "53, DRIZZLE",
            "63, RAIN", "66, FREEZING_RAIN", "75, SNOW", "81, SHOWERS", "86, SNOW_SHOWERS",
            "95, THUNDERSTORM", "12345, UNKNOWN"
    })
    void mapsWmoCodes(int code, WeatherCode expected) {
        assertThat(WeatherCode.of(code)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"0, false", "3, false", "61, true", "73, true", "95, true"})
    void detectsPrecipitation(int code, boolean precipitation) {
        assertThat(WeatherCode.of(code).isPrecipitation()).isEqualTo(precipitation);
    }
}
