package com.weathermate.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", nullable = false, unique = true)
    private Long telegramId;

    private String username;

    @Column(name = "first_name")
    private String firstName;

    @Column(nullable = false)
    private String language = "ru";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Units units = Units.CELSIUS;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected AppUser() {
    }

    public AppUser(Long telegramId, String username, String firstName) {
        this.telegramId = telegramId;
        this.username = username;
        this.firstName = firstName;
    }

    public Long getId() { return id; }
    public Long getTelegramId() { return telegramId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Units getUnits() { return units; }
    public void setUnits(Units units) { this.units = units; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
