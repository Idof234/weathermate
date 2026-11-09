package com.weathermate.api.dto;

import java.util.List;

public record WeatherDto(
        LocationDto location,
        String unitSymbol,
        Current current,
        List<Hour> hourly,
        List<Day> daily,
        List<String> advice,
        List<String> alerts
) {
    public record Current(String time, long temperature, long feelsLike, int humidity, double windSpeed,
                          String description, String icon) {
    }

    public record Hour(String time, long temperature, int precipitationChance, String icon) {
    }

    public record Day(String date, long min, long max, int precipitationChance, double windSpeedMax,
                      String description, String icon, String sunrise, String sunset) {
    }
}
