package com.weathermate.api;

import com.weathermate.api.dto.WeatherDto;
import com.weathermate.entity.Location;
import com.weathermate.entity.Units;
import com.weathermate.service.LocationService;
import com.weathermate.service.WeatherPresenter;
import com.weathermate.service.WeatherService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final LocationService locationService;
    private final WeatherService weatherService;
    private final WeatherPresenter presenter;

    public WeatherController(LocationService locationService, WeatherService weatherService,
                             WeatherPresenter presenter) {
        this.locationService = locationService;
        this.weatherService = weatherService;
        this.presenter = presenter;
    }

    /** GET /api/weather/{locationId}?units=CELSIUS: current weather, 24 hours and 7 days. */
    @GetMapping("/{locationId}")
    public WeatherDto weather(@PathVariable Long locationId,
                              @RequestParam(defaultValue = "CELSIUS") Units units) {
        Location location = locationService.getById(locationId);
        return presenter.present(location, false, weatherService.getForecast(location), units);
    }
}
