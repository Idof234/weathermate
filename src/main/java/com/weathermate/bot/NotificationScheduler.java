package com.weathermate.bot;

import com.weathermate.api.dto.WeatherDto;
import com.weathermate.entity.AppUser;
import com.weathermate.entity.Location;
import com.weathermate.entity.NotificationLog;
import com.weathermate.entity.Subscription;
import com.weathermate.repository.NotificationLogRepository;
import com.weathermate.repository.SubscriptionRepository;
import com.weathermate.service.UserService;
import com.weathermate.service.WeatherPresenter;
import com.weathermate.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.Optional;

/**
 * Runs every minute and sends:
 * <ul>
 *   <li>the daily forecast at the time the user picked (in their time zone);</li>
 *   <li>warnings about tomorrow's bad weather at {@link #ALERT_TIME}.</li>
 * </ul>
 * notification_log guarantees each message is sent at most once a day, even after a restart.
 */
@Component
public class NotificationScheduler {

    static final LocalTime ALERT_TIME = LocalTime.of(19, 0);
    /** If the app was down at the exact minute, a message is still sent within this window. */
    static final Duration WINDOW = Duration.ofMinutes(30);

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final SubscriptionRepository subscriptions;
    private final NotificationLogRepository notificationLog;
    private final UserService userService;
    private final WeatherService weatherService;
    private final WeatherPresenter presenter;
    private final BotFormatter formatter;
    private final BotMessenger messenger;
    private final Clock clock;

    public NotificationScheduler(SubscriptionRepository subscriptions, NotificationLogRepository notificationLog,
                                 UserService userService, WeatherService weatherService,
                                 WeatherPresenter presenter, BotFormatter formatter,
                                 BotMessenger messenger, Clock clock) {
        this.subscriptions = subscriptions;
        this.notificationLog = notificationLog;
        this.userService = userService;
        this.weatherService = weatherService;
        this.presenter = presenter;
        this.formatter = formatter;
        this.messenger = messenger;
        this.clock = clock;
    }

    @Scheduled(cron = "0 * * * * *")
    public void tick() {
        Instant now = clock.instant();
        subscriptions.findByDailyEnabledTrue().forEach(s -> safely(() -> sendDailyIfDue(s, now)));
        subscriptions.findByAlertsEnabledTrue().forEach(s -> safely(() -> sendAlertsIfDue(s, now)));
    }

    void sendDailyIfDue(Subscription sub, Instant now) {
        ZonedDateTime local = now.atZone(ZoneId.of(sub.getTimezone()));
        if (!isDue(local.toLocalTime(), sub.getNotifyTime())) return;
        AppUser user = sub.getUser();
        if (alreadySent(user, NotificationLog.Type.DAILY, local.toLocalDate())) return;

        weather(user).ifPresent(w -> {
            if (messenger.sendHtml(user.getTelegramId(), "☀️ <b>Доброе утро!</b>\n\n" + formatter.current(w))) {
                notificationLog.save(new NotificationLog(user, NotificationLog.Type.DAILY, local.toLocalDate()));
            }
        });
    }

    void sendAlertsIfDue(Subscription sub, Instant now) {
        ZonedDateTime local = now.atZone(ZoneId.of(sub.getTimezone()));
        if (!isDue(local.toLocalTime(), ALERT_TIME)) return;
        AppUser user = sub.getUser();
        if (alreadySent(user, NotificationLog.Type.ALERT, local.toLocalDate())) return;

        weather(user).ifPresent(w -> {
            boolean sent = w.alerts().isEmpty()
                    || messenger.sendHtml(user.getTelegramId(), formatter.alerts(w.location().name(), w.alerts()));
            if (sent) {
                // logged even when there was nothing to warn about, so the forecast is not re-checked all evening
                notificationLog.save(new NotificationLog(user, NotificationLog.Type.ALERT, local.toLocalDate()));
            }
        });
    }

    static boolean isDue(LocalTime now, LocalTime target) {
        return !now.isBefore(target) && now.isBefore(target.plus(WINDOW));
    }

    private boolean alreadySent(AppUser user, NotificationLog.Type type, LocalDate date) {
        return notificationLog.existsByUserAndTypeAndSentDate(user, type, date);
    }

    private Optional<WeatherDto> weather(AppUser user) {
        Optional<Location> location = userService.primaryLocation(user);
        return location.map(l -> presenter.present(l, true, weatherService.getForecast(l), user.getUnits()));
    }

    private static void safely(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("Notification failed", e);
        }
    }
}
