package com.weathermate.service;

import com.weathermate.client.dto.ForecastResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Looks at tomorrow's forecast and finds things worth warning about. */
@Component
public class WeatherAlerts {

    static final int PRECIP_THRESHOLD = 60;
    static final double FROST_THRESHOLD = -10;
    static final double WIND_THRESHOLD = 15;
    static final double HEAT_THRESHOLD = 30;

    /** Returns warnings for tomorrow (daily index 1), empty list if all is fine. */
    public List<String> forTomorrow(ForecastResponse forecast) {
        ForecastResponse.Daily daily = forecast.daily();
        List<String> alerts = new ArrayList<>();
        if (daily == null || daily.time() == null || daily.time().size() < 2) {
            return alerts;
        }
        int i = 1;
        WeatherCode code = WeatherCode.of(daily.weatherCode().get(i));
        int precip = valueOrZero(daily.precipitationProbabilityMax().get(i));
        double min = daily.temperatureMin().get(i);
        double max = daily.temperatureMax().get(i);
        double wind = daily.windSpeedMax().get(i);

        if (code == WeatherCode.THUNDERSTORM) {
            alerts.add("⛈️ Завтра ожидается гроза");
        } else if (code.isSnow() && precip >= PRECIP_THRESHOLD) {
            alerts.add("🌨️ Завтра снег (вероятность " + precip + "%)");
        } else if (code.isPrecipitation() && precip >= PRECIP_THRESHOLD) {
            alerts.add("🌧️ Завтра дождь (вероятность " + precip + "%), возьми зонт");
        }
        if (min <= FROST_THRESHOLD) {
            alerts.add("🥶 Сильный мороз: до " + Math.round(min) + "°C");
        }
        if (max >= HEAT_THRESHOLD) {
            alerts.add("🥵 Жара: до " + Math.round(max) + "°C");
        }
        if (wind >= WIND_THRESHOLD) {
            alerts.add("💨 Сильный ветер: до " + Math.round(wind) + " м/с");
        }
        return alerts;
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
