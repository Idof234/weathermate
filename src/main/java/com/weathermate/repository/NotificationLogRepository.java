package com.weathermate.repository;

import com.weathermate.entity.AppUser;
import com.weathermate.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    boolean existsByUserAndTypeAndSentDate(AppUser user, NotificationLog.Type type, LocalDate sentDate);
}
