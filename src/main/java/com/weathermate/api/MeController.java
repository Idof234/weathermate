package com.weathermate.api;

import com.weathermate.api.dto.*;
import com.weathermate.entity.AppUser;
import com.weathermate.entity.Location;
import com.weathermate.entity.Subscription;
import com.weathermate.entity.UserLocation;
import com.weathermate.security.CurrentUser;
import com.weathermate.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Everything about the logged-in user. All endpoints need Telegram initData. */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;
    private final LocationService locationService;
    private final WeatherService weatherService;
    private final WeatherPresenter presenter;

    public MeController(UserService userService, LocationService locationService,
                        WeatherService weatherService, WeatherPresenter presenter) {
        this.userService = userService;
        this.locationService = locationService;
        this.weatherService = weatherService;
        this.presenter = presenter;
    }

    @GetMapping
    public MeDto me(@CurrentUser AppUser user) {
        return new MeDto(user.getFirstName(), settings(user), favourites(user));
    }

    /** Weather in the primary city. 404 if the user has not chosen a city yet. */
    @GetMapping("/weather")
    public WeatherDto weather(@CurrentUser AppUser user) {
        Location location = userService.primaryLocation(user)
                .orElseThrow(() -> new NotFoundException("Выбери свой город"));
        return presenter.present(location, true, weatherService.getForecast(location), user.getUnits());
    }

    @GetMapping("/locations")
    public List<LocationDto> favourites(@CurrentUser AppUser user) {
        return userService.favourites(user).stream()
                .map(ul -> LocationDto.from(ul.getLocation(), ul.isPrimary()))
                .toList();
    }

    @PostMapping("/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationDto addFavourite(@CurrentUser AppUser user, @Valid @RequestBody AddLocationRequest request) {
        Location location;
        if (request.place() != null) {
            location = locationService.saveFromPlace(request.place().toPlace());
        } else if (request.latitude() != null && request.longitude() != null) {
            location = locationService.saveFromCoordinates(request.latitude(), request.longitude());
        } else {
            throw new IllegalArgumentException("Нужен город или координаты");
        }
        UserLocation saved = userService.addFavourite(user, location, request.primary());
        return LocationDto.from(saved.getLocation(), saved.isPrimary());
    }

    @PutMapping("/locations/{locationId}/primary")
    public List<LocationDto> setPrimary(@CurrentUser AppUser user, @PathVariable Long locationId) {
        userService.setPrimary(user, locationId);
        return favourites(user);
    }

    @DeleteMapping("/locations/{locationId}")
    public List<LocationDto> removeFavourite(@CurrentUser AppUser user, @PathVariable Long locationId) {
        userService.removeFavourite(user, locationId);
        return favourites(user);
    }

    @GetMapping("/settings")
    public SettingsDto settings(@CurrentUser AppUser user) {
        Subscription s = userService.subscription(user);
        return new SettingsDto(user.getLanguage(), user.getUnits(), s.getNotifyTime(), s.getTimezone(),
                s.isDailyEnabled(), s.isAlertsEnabled());
    }

    @PutMapping("/settings")
    public SettingsDto updateSettings(@CurrentUser AppUser user, @RequestBody SettingsDto request) {
        userService.updateSettings(user, request.language(), request.units(), request.notifyTime(),
                request.timezone(), request.dailyEnabled(), request.alertsEnabled());
        return settings(user);
    }
}
