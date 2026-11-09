package com.weathermate.api.dto;

import java.util.List;

public record MeDto(String firstName, SettingsDto settings, List<LocationDto> favourites) {
}
