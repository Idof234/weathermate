package com.weathermate.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * Either {@code place} (picked from search) or {@code latitude}/{@code longitude} (GPS) must be set.
 */
public record AddLocationRequest(
        @Valid PlaceDto place,
        @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude,
        boolean primary
) {
}
