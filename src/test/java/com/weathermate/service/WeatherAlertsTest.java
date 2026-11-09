package com.weathermate.service;

import com.weathermate.client.dto.ForecastResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherAlertsTest {

    private final WeatherAlerts alerts = new WeatherAlerts();

    @Test
    void niceWeatherGivesNoAlerts() {
        assertThat(alerts.forTomorrow(forecastTomorrow(0, 10, 2, 18, 5))).isEmpty();
    }

    @Test
    void rainWithHighChance() {
        assertThat(alerts.forTomorrow(forecastTomorrow(63, 80, 5, 10, 4)))
                .singleElement().asString().contains("дождь");
    }

    @Test
    void rainWithLowChanceIsIgnored() {
        assertThat(alerts.forTomorrow(forecastTomorrow(61, 30, 5, 10, 4))).isEmpty();
    }

    @Test
    void frostAndWindTogether() {
        assertThat(alerts.forTomorrow(forecastTomorrow(0, 0, -15, -8, 17))).hasSize(2);
    }

    @Test
    void thunderstorm() {
        assertThat(alerts.forTomorrow(forecastTomorrow(95, 90, 15, 25, 8)))
                .anyMatch(a -> a.contains("гроза"));
    }

    @Test
    void handlesMissingDailyData() {
        assertThat(alerts.forTomorrow(new ForecastResponse("UTC", null, null, null))).isEmpty();
    }

    /** Today is always calm; the parameters describe tomorrow. */
    private static ForecastResponse forecastTomorrow(int code, int precip, double min, double max, double wind) {
        var daily = new ForecastResponse.Daily(
                List.of("2026-11-09", "2026-11-10"),
                List.of(0, code),
                List.of(10.0, max),
                List.of(5.0, min),
                List.of(0, precip),
                List.of(3.0, wind),
                List.of("2026-11-09T08:00", "2026-11-10T08:02"),
                List.of("2026-11-09T16:00", "2026-11-10T15:58"));
        return new ForecastResponse("Europe/Tallinn", null, null, daily);
    }
}
