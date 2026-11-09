package com.weathermate.entity;

import jakarta.persistence.*;

import java.time.LocalTime;

/** Notification settings of one user. */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private AppUser user;

    @Column(name = "notify_time", nullable = false)
    private LocalTime notifyTime = LocalTime.of(8, 0);

    @Column(nullable = false)
    private String timezone = "Europe/Tallinn";

    @Column(name = "daily_enabled", nullable = false)
    private boolean dailyEnabled;

    @Column(name = "alerts_enabled", nullable = false)
    private boolean alertsEnabled = true;

    protected Subscription() {
    }

    public Subscription(AppUser user) {
        this.user = user;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public LocalTime getNotifyTime() { return notifyTime; }
    public void setNotifyTime(LocalTime notifyTime) { this.notifyTime = notifyTime; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public boolean isDailyEnabled() { return dailyEnabled; }
    public void setDailyEnabled(boolean dailyEnabled) { this.dailyEnabled = dailyEnabled; }
    public boolean isAlertsEnabled() { return alertsEnabled; }
    public void setAlertsEnabled(boolean alertsEnabled) { this.alertsEnabled = alertsEnabled; }
}
