package com.weathermate.repository;

import com.weathermate.entity.AppUser;
import com.weathermate.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUser(AppUser user);

    List<Subscription> findByDailyEnabledTrue();

    List<Subscription> findByAlertsEnabledTrue();
}
