package com.weathermate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weathermate.client.OpenMeteoClient;
import com.weathermate.client.WeatherApiException;
import com.weathermate.client.dto.ForecastResponse;
import com.weathermate.config.AppProperties;
import com.weathermate.entity.Location;
import com.weathermate.entity.WeatherCache;
import com.weathermate.repository.WeatherCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Gives the forecast for a location. Answers are cached in the weather_cache table:
 * a fresh cache entry is returned without calling the API, and if the API is down
 * the last known (stale) forecast is returned instead of an error.
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final OpenMeteoClient client;
    private final WeatherCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;
    private final Clock clock;

    public WeatherService(OpenMeteoClient client, WeatherCacheRepository cacheRepository,
                          ObjectMapper objectMapper, AppProperties properties, Clock clock) {
        this.client = client;
        this.cacheRepository = cacheRepository;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public ForecastResponse getForecast(Location location) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<WeatherCache> cached = cacheRepository.findByLocationId(location.getId());

        if (cached.isPresent() && cached.get().isFresh(properties.cacheTtlMinutes(), now)) {
            return parse(cached.get().getJsonData());
        }

        try {
            String json = client.fetchForecastJson(location.getLatitude(), location.getLongitude());
            ForecastResponse forecast = parse(json);
            if (cached.isPresent()) {
                cached.get().update(json, now);
            } else {
                cacheRepository.save(new WeatherCache(location, json, now));
            }
            return forecast;
        } catch (WeatherApiException e) {
            if (cached.isPresent()) {
                log.warn("Open-Meteo unavailable, using stale cache for location {}", location.getId());
                return parse(cached.get().getJsonData());
            }
            throw e;
        }
    }

    private ForecastResponse parse(String json) {
        try {
            return objectMapper.readValue(json, ForecastResponse.class);
        } catch (JsonProcessingException e) {
            throw new WeatherApiException("Cannot parse forecast JSON", e);
        }
    }
}
