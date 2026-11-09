package com.weathermate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** Injected instead of calling now() directly, so time can be fixed in tests. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
