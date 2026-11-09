package com.weathermate.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A city in the user's favourites. Exactly one of them is primary. */
@Entity
@Table(name = "user_locations")
public class UserLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt = LocalDateTime.now();

    protected UserLocation() {
    }

    public UserLocation(AppUser user, Location location, boolean primary) {
        this.user = user;
        this.location = location;
        this.primary = primary;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public Location getLocation() { return location; }
    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }
    public LocalDateTime getAddedAt() { return addedAt; }
}
