package com.weathermate.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Simple rules that turn weather into a "what to wear" tip. */
@Component
public class ClothingAdvisor {

    /**
     * @param feelsLike     apparent temperature in °C
     * @param windSpeed     wind in m/s
     * @param precipChance  chance of precipitation in the next hours, 0..100
     * @param code          current weather
     */
    public List<String> advise(double feelsLike, double windSpeed, int precipChance, WeatherCode code) {
        List<String> tips = new ArrayList<>();

        if (feelsLike <= -15) {
            tips.add("Очень холодно: тёплый пуховик, шапка, шарф и перчатки");
        } else if (feelsLike <= -5) {
            tips.add("Зимняя куртка, шапка и перчатки");
        } else if (feelsLike <= 5) {
            tips.add("Тёплая куртка, не забудь шапку");
        } else if (feelsLike <= 12) {
            tips.add("Лёгкая куртка или худи");
        } else if (feelsLike <= 20) {
            tips.add("Кофта или ветровка");
        } else if (feelsLike <= 27) {
            tips.add("Футболка, лёгкая одежда");
        } else {
            tips.add("Жарко: лёгкая одежда, вода и головной убор");
        }

        if (code.isSnow()) {
            tips.add("Идёт снег: непромокаемая обувь");
        } else if (code.isPrecipitation() || precipChance >= 60) {
            tips.add("Возьми зонт ☂️");
        } else if (precipChance >= 30) {
            tips.add("Возможен дождь, зонт лучше взять с собой");
        }

        if (windSpeed >= 10) {
            tips.add("Сильный ветер: подойдёт капюшон или ветровка");
        }
        return tips;
    }
}
