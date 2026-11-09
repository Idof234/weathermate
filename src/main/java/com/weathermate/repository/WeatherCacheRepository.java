package com.weathermate.repository;

import com.weathermate.entity.WeatherCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WeatherCacheRepository extends JpaRepository<WeatherCache, Long> {

    Optional<WeatherCache> findByLocationId(Long locationId);
}
