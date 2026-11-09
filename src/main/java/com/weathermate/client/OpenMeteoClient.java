package com.weathermate.client;

import com.weathermate.client.dto.GeocodingResponse;
import com.weathermate.config.OpenMeteoProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * HTTP client for the free Open-Meteo API (no API key needed).
 * https://open-meteo.com/en/docs
 */
@Component
public class OpenMeteoClient {

    private static final String CURRENT =
            "temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,is_day";
    private static final String HOURLY = "temperature_2m,weather_code,precipitation_probability";
    private static final String DAILY =
            "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,"
                    + "wind_speed_10m_max,sunrise,sunset";

    private final RestClient restClient;
    private final OpenMeteoProperties properties;

    public OpenMeteoClient(RestClient.Builder builder, OpenMeteoProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    /** Finds cities by name, e.g. "Tallinn" or "Нарва". */
    public List<GeocodingResponse.Place> searchPlaces(String query, String language) {
        String uri = UriComponentsBuilder.fromUriString(properties.geocodingUrl())
                .queryParam("name", query)
                .queryParam("count", 8)
                .queryParam("language", language)
                .queryParam("format", "json")
                .encode()
                .toUriString();
        try {
            GeocodingResponse response = restClient.get().uri(uri).retrieve().body(GeocodingResponse.class);
            return response == null || response.results() == null ? List.of() : response.results();
        } catch (RestClientException e) {
            throw new WeatherApiException("Geocoding request failed", e);
        }
    }

    /** Returns the raw forecast JSON: it is stored in the cache as is. */
    public String fetchForecastJson(double latitude, double longitude) {
        String uri = UriComponentsBuilder.fromUriString(properties.forecastUrl())
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", CURRENT)
                .queryParam("hourly", HOURLY)
                .queryParam("daily", DAILY)
                .queryParam("timezone", "auto")
                .queryParam("forecast_days", 7)
                .queryParam("wind_speed_unit", "ms")
                .encode()
                .toUriString();
        try {
            String body = restClient.get().uri(uri).retrieve().body(String.class);
            if (body == null || body.isBlank()) {
                throw new WeatherApiException("Empty forecast response", null);
            }
            return body;
        } catch (RestClientException e) {
            throw new WeatherApiException("Forecast request failed", e);
        }
    }
}
