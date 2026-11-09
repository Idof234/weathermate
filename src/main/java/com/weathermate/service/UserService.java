package com.weathermate.service;

import com.weathermate.entity.*;
import com.weathermate.repository.AppUserRepository;
import com.weathermate.repository.SubscriptionRepository;
import com.weathermate.repository.UserLocationRepository;
import com.weathermate.security.TelegramUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private static final Set<String> LANGUAGES = Set.of("ru", "en", "et");
    private static final int MAX_FAVOURITES = 10;

    private final AppUserRepository userRepository;
    private final UserLocationRepository userLocationRepository;
    private final SubscriptionRepository subscriptionRepository;

    public UserService(AppUserRepository userRepository, UserLocationRepository userLocationRepository,
                       SubscriptionRepository subscriptionRepository) {
        this.userRepository = userRepository;
        this.userLocationRepository = userLocationRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    /** Finds the user by Telegram id or registers a new one. Name and username are kept up to date. */
    @Transactional
    public AppUser getOrCreate(TelegramUser tg) {
        AppUser user = userRepository.findByTelegramId(tg.id()).orElseGet(() -> {
            AppUser created = new AppUser(tg.id(), tg.username(), tg.firstName());
            if (tg.languageCode() != null && LANGUAGES.contains(tg.languageCode())) {
                created.setLanguage(tg.languageCode());
            }
            created = userRepository.save(created);
            subscriptionRepository.save(new Subscription(created));
            return created;
        });
        user.setUsername(tg.username());
        user.setFirstName(tg.firstName());
        return user;
    }

    @Transactional(readOnly = true)
    public List<UserLocation> favourites(AppUser user) {
        return userLocationRepository.findByUserOrderByPrimaryDescAddedAtAsc(user);
    }

    @Transactional(readOnly = true)
    public Optional<Location> primaryLocation(AppUser user) {
        return userLocationRepository.findByUserAndPrimaryTrue(user).map(UserLocation::getLocation);
    }

    /** Adds a city to favourites. The first city (or one added with makePrimary) becomes primary. */
    @Transactional
    public UserLocation addFavourite(AppUser user, Location location, boolean makePrimary) {
        Optional<UserLocation> existing = userLocationRepository.findByUserAndLocationId(user, location.getId());
        if (existing.isPresent()) {
            if (makePrimary) setPrimary(user, location.getId());
            return existing.get();
        }
        List<UserLocation> all = favourites(user);
        if (all.size() >= MAX_FAVOURITES) {
            throw new IllegalArgumentException("Можно сохранить не больше " + MAX_FAVOURITES + " городов");
        }
        boolean primary = all.isEmpty() || makePrimary;
        if (primary) {
            all.forEach(ul -> ul.setPrimary(false));
        }
        return userLocationRepository.save(new UserLocation(user, location, primary));
    }

    @Transactional
    public void setPrimary(AppUser user, Long locationId) {
        UserLocation target = userLocationRepository.findByUserAndLocationId(user, locationId)
                .orElseThrow(() -> new NotFoundException("City is not in favourites"));
        favourites(user).forEach(ul -> ul.setPrimary(false));
        target.setPrimary(true);
    }

    @Transactional
    public void removeFavourite(AppUser user, Long locationId) {
        UserLocation target = userLocationRepository.findByUserAndLocationId(user, locationId)
                .orElseThrow(() -> new NotFoundException("City is not in favourites"));
        boolean wasPrimary = target.isPrimary();
        userLocationRepository.delete(target);
        userLocationRepository.flush();
        if (wasPrimary) {
            favourites(user).stream().findFirst().ifPresent(ul -> ul.setPrimary(true));
        }
    }

    @Transactional
    public Subscription subscription(AppUser user) {
        return subscriptionRepository.findByUser(user)
                .orElseGet(() -> subscriptionRepository.save(new Subscription(user)));
    }

    @Transactional
    public void setDailyEnabled(AppUser user, boolean enabled) {
        subscription(user).setDailyEnabled(enabled);
    }

    /** Updates settings; null fields are left unchanged. */
    @Transactional
    public void updateSettings(AppUser user, String language, Units units, LocalTime notifyTime,
                               String timezone, Boolean dailyEnabled, Boolean alertsEnabled) {
        if (language != null) {
            if (!LANGUAGES.contains(language)) throw new IllegalArgumentException("Unsupported language: " + language);
            user.setLanguage(language);
        }
        if (units != null) user.setUnits(units);
        userRepository.save(user);

        Subscription sub = subscription(user);
        if (notifyTime != null) sub.setNotifyTime(notifyTime.withSecond(0).withNano(0));
        if (timezone != null) {
            ZoneId.of(timezone); // throws on unknown zone
            sub.setTimezone(timezone);
        }
        if (dailyEnabled != null) sub.setDailyEnabled(dailyEnabled);
        if (alertsEnabled != null) sub.setAlertsEnabled(alertsEnabled);
    }
}
