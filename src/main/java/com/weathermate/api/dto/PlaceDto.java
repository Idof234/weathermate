package com.weathermate.api.dto;

import com.weathermate.client.dto.GeocodingResponse;

/** A city found by search; not yet saved in the database. */
public record PlaceDto(long externalId, String name, String region, String country, String countryCode,
                       double latitude, double longitude, String timezone) {

    public static PlaceDto from(GeocodingResponse.Place p) {
        return new PlaceDto(p.id(), p.name(), p.admin1(), p.country(), p.countryCode(),
                p.latitude(), p.longitude(), p.timezone());
    }

    public GeocodingResponse.Place toPlace() {
        return new GeocodingResponse.Place(externalId, name, latitude, longitude, country, countryCode, region, timezone);
    }
}
