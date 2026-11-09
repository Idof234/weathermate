package com.weathermate.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "locations")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private Long externalId;

    @Column(nullable = false)
    private String name;

    private String region;
    private String country;

    @Column(name = "country_code")
    private String countryCode;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    private String timezone;

    protected Location() {
    }

    public Location(Long externalId, String name, String region, String country, String countryCode,
                    double latitude, double longitude, String timezone) {
        this.externalId = externalId;
        this.name = name;
        this.region = region;
        this.country = country;
        this.countryCode = countryCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timezone = timezone;
    }

    /** "Tallinn, Harjumaa, Estonia" without empty parts. */
    public String displayName() {
        StringBuilder sb = new StringBuilder(name);
        if (region != null && !region.isBlank() && !region.equals(name)) sb.append(", ").append(region);
        if (country != null && !country.isBlank()) sb.append(", ").append(country);
        return sb.toString();
    }

    public Long getId() { return id; }
    public Long getExternalId() { return externalId; }
    public String getName() { return name; }
    public String getRegion() { return region; }
    public String getCountry() { return country; }
    public String getCountryCode() { return countryCode; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getTimezone() { return timezone; }
}
