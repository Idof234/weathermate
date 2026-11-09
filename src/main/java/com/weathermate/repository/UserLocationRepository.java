package com.weathermate.repository;

import com.weathermate.entity.AppUser;
import com.weathermate.entity.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {

    List<UserLocation> findByUserOrderByPrimaryDescAddedAtAsc(AppUser user);

    Optional<UserLocation> findByUserAndPrimaryTrue(AppUser user);

    Optional<UserLocation> findByUserAndLocationId(AppUser user, Long locationId);
}
