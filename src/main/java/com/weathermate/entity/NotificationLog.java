package com.weathermate.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Remembers what was already sent, so a user never gets the same message twice a day. */
@Entity
@Table(name = "notification_log")
public class NotificationLog {

    public enum Type { DAILY, ALERT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(name = "sent_date", nullable = false)
    private LocalDate sentDate;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    protected NotificationLog() {
    }

    public NotificationLog(AppUser user, Type type, LocalDate sentDate) {
        this.user = user;
        this.type = type;
        this.sentDate = sentDate;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public Type getType() { return type; }
    public LocalDate getSentDate() { return sentDate; }
    public LocalDateTime getSentAt() { return sentAt; }
}
