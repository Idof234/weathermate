package com.weathermate.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Last Open-Meteo answer for a location, so the API is not called on every request. */
@Entity
@Table(name = "weather_cache")
public class WeatherCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", unique = true)
    private Location location;

    @Column(name = "json_data", nullable = false, columnDefinition = "TEXT")
    private String jsonData;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    protected WeatherCache() {
    }

    public WeatherCache(Location location, String jsonData, LocalDateTime fetchedAt) {
        this.location = location;
        this.jsonData = jsonData;
        this.fetchedAt = fetchedAt;
    }

    public boolean isFresh(int ttlMinutes, LocalDateTime now) {
        return fetchedAt.plusMinutes(ttlMinutes).isAfter(now);
    }

    public void update(String jsonData, LocalDateTime fetchedAt) {
        this.jsonData = jsonData;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() { return id; }
    public Location getLocation() { return location; }
    public String getJsonData() { return jsonData; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
}
