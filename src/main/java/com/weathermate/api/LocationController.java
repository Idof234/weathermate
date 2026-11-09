package com.weathermate.api;

import com.weathermate.api.dto.PlaceDto;
import com.weathermate.service.LocationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /** City autocomplete: GET /api/locations/search?q=Tall&lang=ru */
    @GetMapping("/search")
    public List<PlaceDto> search(@RequestParam("q") String query,
                                 @RequestParam(value = "lang", defaultValue = "ru") String language) {
        return locationService.search(query, language).stream().map(PlaceDto::from).toList();
    }
}
