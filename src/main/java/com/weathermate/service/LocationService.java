package com.weathermate.service;

import com.weathermate.client.OpenMeteoClient;
import com.weathermate.client.dto.GeocodingResponse;
import com.weathermate.entity.Location;
import com.weathermate.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class LocationService {

    private final OpenMeteoClient client;
    private final LocationRepository repository;

    public LocationService(OpenMeteoClient client, LocationRepository repository) {
        this.client = client;
        this.repository = repository;
    }

    public List<GeocodingResponse.Place> search(String query, String language) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        return client.searchPlaces(query.trim(), language);
    }

    /** Saves a place found by search (or returns the already saved one). */
    @Transactional
    public Location saveFromPlace(GeocodingResponse.Place place) {
        return repository.findByExternalId(place.id()).orElseGet(() -> repository.save(new Location(
                place.id(), place.name(), place.admin1(), place.country(), place.countryCode(),
                place.latitude(), place.longitude(), place.timezone())));
    }

    /**
     * Location from the phone's GPS. Open-Meteo has no reverse geocoding, so the point
     * gets a synthetic negative id built from the coordinates rounded to ~1 km.
     */
    @Transactional
    public Location saveFromCoordinates(double latitude, double longitude) {
        double lat = Math.round(latitude * 100) / 100.0;
        double lon = Math.round(longitude * 100) / 100.0;
        long syntheticId = -(Math.round((lat + 90) * 100) * 100_000L + Math.round((lon + 180) * 100));
        String name = String.format(Locale.ROOT, "📍 %.2f, %.2f", lat, lon);
        return repository.findByExternalId(syntheticId).orElseGet(() -> repository.save(
                new Location(syntheticId, name, null, null, null, lat, lon, null)));
    }

    public Location getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Location " + id + " not found"));
    }
}
