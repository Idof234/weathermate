package com.weathermate.client;

/** Thrown when the weather provider is unavailable or answers with an error. */
public class WeatherApiException extends RuntimeException {

    public WeatherApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
