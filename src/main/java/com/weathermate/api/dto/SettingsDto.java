package com.weathermate.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.weathermate.entity.Units;

import java.time.LocalTime;

public record SettingsDto(
        String language,
        Units units,
        @JsonFormat(pattern = "HH:mm") LocalTime notifyTime,
        String timezone,
        Boolean dailyEnabled,
        Boolean alertsEnabled
) {
}
