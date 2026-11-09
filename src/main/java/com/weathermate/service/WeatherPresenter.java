package com.weathermate.service;

import com.weathermate.api.dto.LocationDto;
import com.weathermate.api.dto.WeatherDto;
import com.weathermate.client.dto.ForecastResponse;
import com.weathermate.entity.Location;
import com.weathermate.entity.Units;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Converts the raw Open-Meteo forecast into what the Mini App and the bot show. */
@Component
public class WeatherPresenter {

    static final int HOURS = 24;
    private static final int ADVICE_HOURS = 6;

    private final ClothingAdvisor clothingAdvisor;
    private final WeatherAlerts weatherAlerts;

    public WeatherPresenter(ClothingAdvisor clothingAdvisor, WeatherAlerts weatherAlerts) {
        this.clothingAdvisor = clothingAdvisor;
        this.weatherAlerts = weatherAlerts;
    }

    public WeatherDto present(Location location, boolean primary, ForecastResponse f, Units units) {
        ForecastResponse.Current c = f.current();
        WeatherCode code = WeatherCode.of(c.weatherCode());
        boolean day = c.isDay() == 1;

        WeatherDto.Current current = new WeatherDto.Current(
                c.time(),
                Math.round(units.convert(c.temperature())),
                Math.round(units.convert(c.apparentTemperature())),
                c.humidity(),
                round1(c.windSpeed()),
                code.description(),
                code.icon(day));

        List<WeatherDto.Hour> hourly = hourly(f, units);
        int precipSoon = hourly.stream().limit(ADVICE_HOURS)
                .mapToInt(WeatherDto.Hour::precipitationChance).max().orElse(0);

        return new WeatherDto(
                LocationDto.from(location, primary),
                units.symbol(),
                current,
                hourly,
                daily(f, units),
                clothingAdvisor.advise(c.apparentTemperature(), c.windSpeed(), precipSoon, code),
                weatherAlerts.forTomorrow(f));
    }

    private List<WeatherDto.Hour> hourly(ForecastResponse f, Units units) {
        ForecastResponse.Hourly h = f.hourly();
        List<WeatherDto.Hour> result = new ArrayList<>();
        if (h == null || h.time() == null) return result;

        LocalDateTime from = LocalDateTime.parse(f.current().time()).truncatedTo(ChronoUnit.HOURS);
        for (int i = 0; i < h.time().size() && result.size() < HOURS; i++) {
            LocalDateTime t = LocalDateTime.parse(h.time().get(i));
            if (t.isBefore(from)) continue;
            WeatherCode code = WeatherCode.of(h.weatherCode().get(i));
            boolean day = t.getHour() >= 7 && t.getHour() < 20;
            result.add(new WeatherDto.Hour(
                    t.toLocalTime().toString(),
                    Math.round(units.convert(h.temperature().get(i))),
                    nz(h.precipitationProbability() == null ? null : h.precipitationProbability().get(i)),
                    code.icon(day)));
        }
        return result;
    }

    private List<WeatherDto.Day> daily(ForecastResponse f, Units units) {
        ForecastResponse.Daily d = f.daily();
        List<WeatherDto.Day> result = new ArrayList<>();
        if (d == null || d.time() == null) return result;

        for (int i = 0; i < d.time().size(); i++) {
            WeatherCode code = WeatherCode.of(d.weatherCode().get(i));
            result.add(new WeatherDto.Day(
                    d.time().get(i),
                    Math.round(units.convert(d.temperatureMin().get(i))),
                    Math.round(units.convert(d.temperatureMax().get(i))),
                    nz(d.precipitationProbabilityMax().get(i)),
                    round1(d.windSpeedMax().get(i)),
                    code.description(),
                    code.icon(true),
                    timeOnly(d.sunrise(), i),
                    timeOnly(d.sunset(), i)));
        }
        return result;
    }

    private static String timeOnly(List<String> list, int i) {
        if (list == null || list.size() <= i || list.get(i) == null) return null;
        String v = list.get(i);
        int t = v.indexOf('T');
        return t >= 0 ? v.substring(t + 1) : v;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
