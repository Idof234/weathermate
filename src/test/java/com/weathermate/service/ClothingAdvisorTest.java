package com.weathermate.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClothingAdvisorTest {

    private final ClothingAdvisor advisor = new ClothingAdvisor();

    @Test
    void frostNeedsWinterClothes() {
        assertThat(advisor.advise(-18, 2, 0, WeatherCode.CLEAR))
                .singleElement().asString().contains("пуховик");
    }

    @Test
    void rainNeedsUmbrella() {
        assertThat(advisor.advise(10, 3, 80, WeatherCode.OVERCAST))
                .anyMatch(t -> t.contains("зонт"));
        assertThat(advisor.advise(10, 3, 0, WeatherCode.RAIN))
                .anyMatch(t -> t.contains("зонт"));
    }

    @Test
    void snowNeedsWaterproofShoesNotUmbrella() {
        var tips = advisor.advise(-3, 2, 90, WeatherCode.SNOW);
        assertThat(tips).anyMatch(t -> t.contains("обувь"));
        assertThat(tips).noneMatch(t -> t.contains("зонт"));
    }

    @Test
    void strongWindIsMentioned() {
        assertThat(advisor.advise(15, 12, 0, WeatherCode.CLEAR))
                .anyMatch(t -> t.contains("ветер"));
    }

    @Test
    void hotWeather() {
        assertThat(advisor.advise(31, 1, 0, WeatherCode.CLEAR))
                .singleElement().asString().contains("Жарко");
    }
}
