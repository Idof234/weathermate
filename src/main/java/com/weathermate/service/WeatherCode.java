package com.weathermate.service;

/**
 * WMO weather interpretation codes used by Open-Meteo.
 * https://open-meteo.com/en/docs#weathervariables
 */
public enum WeatherCode {
    CLEAR("Ясно", "☀️", "🌙"),
    PARTLY_CLOUDY("Переменная облачность", "⛅", "☁️"),
    OVERCAST("Пасмурно", "☁️", "☁️"),
    FOG("Туман", "🌫️", "🌫️"),
    DRIZZLE("Морось", "🌦️", "🌧️"),
    RAIN("Дождь", "🌧️", "🌧️"),
    FREEZING_RAIN("Ледяной дождь", "🌧️", "🌧️"),
    SNOW("Снег", "🌨️", "🌨️"),
    SHOWERS("Ливень", "🌧️", "🌧️"),
    SNOW_SHOWERS("Снегопад", "❄️", "❄️"),
    THUNDERSTORM("Гроза", "⛈️", "⛈️"),
    UNKNOWN("Нет данных", "❔", "❔");

    private final String description;
    private final String dayIcon;
    private final String nightIcon;

    WeatherCode(String description, String dayIcon, String nightIcon) {
        this.description = description;
        this.dayIcon = dayIcon;
        this.nightIcon = nightIcon;
    }

    public static WeatherCode of(int code) {
        return switch (code) {
            case 0 -> CLEAR;
            case 1, 2 -> PARTLY_CLOUDY;
            case 3 -> OVERCAST;
            case 45, 48 -> FOG;
            case 51, 53, 55 -> DRIZZLE;
            case 56, 57, 66, 67 -> FREEZING_RAIN;
            case 61, 63, 65 -> RAIN;
            case 71, 73, 75, 77 -> SNOW;
            case 80, 81, 82 -> SHOWERS;
            case 85, 86 -> SNOW_SHOWERS;
            case 95, 96, 99 -> THUNDERSTORM;
            default -> UNKNOWN;
        };
    }

    public boolean isPrecipitation() {
        return switch (this) {
            case DRIZZLE, RAIN, FREEZING_RAIN, SNOW, SHOWERS, SNOW_SHOWERS, THUNDERSTORM -> true;
            default -> false;
        };
    }

    public boolean isSnow() {
        return this == SNOW || this == SNOW_SHOWERS;
    }

    public String description() { return description; }

    public String icon(boolean day) { return day ? dayIcon : nightIcon; }
}
