package com.weathermate.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeocodingResponse(List<Place> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Place(
            long id,
            String name,
            double latitude,
            double longitude,
            String country,
            @JsonProperty("country_code") String countryCode,
            String admin1,
            String timezone
    ) {
    }
}
