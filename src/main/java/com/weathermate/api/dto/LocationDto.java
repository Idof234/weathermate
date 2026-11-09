package com.weathermate.api.dto;

import com.weathermate.entity.Location;

public record LocationDto(long id, String name, String region, String country, String countryCode,
                          double latitude, double longitude, String displayName, boolean primary) {

    public static LocationDto from(Location l, boolean primary) {
        return new LocationDto(l.getId(), l.getName(), l.getRegion(), l.getCountry(), l.getCountryCode(),
                l.getLatitude(), l.getLongitude(), l.displayName(), primary);
    }
}
