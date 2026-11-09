package com.weathermate.bot;

import com.weathermate.api.dto.WeatherDto;
import com.weathermate.config.AppProperties;
import com.weathermate.entity.AppUser;
import com.weathermate.entity.Location;
import com.weathermate.security.TelegramUser;
import com.weathermate.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonWebApp;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Optional;

/**
 * Telegram bot (long polling). Commands:
 * /start, /weather, /week, /subscribe, /unsubscribe, /help.
 * A location sent in the chat becomes the user's primary city.
 */
@Component
@ConditionalOnExpression(BotConfig.BOT_ENABLED)
public class WeatherBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(WeatherBot.class);

    private final AppProperties properties;
    private final TelegramClient telegram;
    private final UserService userService;
    private final LocationService locationService;
    private final WeatherService weatherService;
    private final WeatherPresenter presenter;
    private final BotFormatter formatter;

    public WeatherBot(AppProperties properties, TelegramClient telegram, UserService userService,
                      LocationService locationService, WeatherService weatherService,
                      WeatherPresenter presenter, BotFormatter formatter) {
        this.properties = properties;
        this.telegram = telegram;
        this.userService = userService;
        this.locationService = locationService;
        this.weatherService = weatherService;
        this.presenter = presenter;
        this.formatter = formatter;
        registerCommands();
    }

    @Override
    public String getBotToken() {
        return properties.botToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage()) return;
        Message message = update.getMessage();
        long chatId = message.getChatId();
        try {
            AppUser user = userService.getOrCreate(toTelegramUser(message.getFrom()));
            if (message.hasLocation()) {
                onLocation(chatId, user, message.getLocation().getLatitude(), message.getLocation().getLongitude());
            } else if (message.hasText()) {
                onCommand(chatId, user, message.getText().trim());
            }
        } catch (Exception e) {
            log.error("Failed to handle update {}", update.getUpdateId(), e);
            send(chatId, "Что-то пошло не так 😕 Попробуй ещё раз позже.", null);
        }
    }

    private void onCommand(long chatId, AppUser user, String text) {
        String command = text.split("[\\s@]")[0].toLowerCase();
        switch (command) {
            case "/start" -> send(chatId, """
                    Привет, %s! 👋
                    Я <b>WeatherMate</b>: показываю погоду, прогноз на неделю и подсказываю, что надеть.

                    Нажми кнопку ниже, чтобы выбрать свой город, или просто отправь мне геолокацию 📍
                    """.formatted(BotFormatter.escape(user.getFirstName())), openAppKeyboard());
            case "/weather" -> withPrimary(chatId, user, w -> formatter.current(w));
            case "/week" -> withPrimary(chatId, user, w -> formatter.week(w));
            case "/subscribe" -> {
                userService.setDailyEnabled(user, true);
                var sub = userService.subscription(user);
                send(chatId, "✅ Буду присылать прогноз каждый день в " + sub.getNotifyTime()
                        + " (" + sub.getTimezone() + "). Время можно поменять в приложении.", null);
            }
            case "/unsubscribe" -> {
                userService.setDailyEnabled(user, false);
                send(chatId, "🔕 Ежедневный прогноз отключён.", null);
            }
            case "/help" -> send(chatId, """
                    /weather — погода сейчас
                    /week — прогноз на 7 дней
                    /subscribe — ежедневный прогноз
                    /unsubscribe — отключить прогноз
                    📍 Отправь геолокацию, чтобы выбрать место
                    """, openAppKeyboard());
            default -> send(chatId, "Не понял команду. Напиши /help", null);
        }
    }

    private void onLocation(long chatId, AppUser user, double lat, double lon) {
        Location location = locationService.saveFromCoordinates(lat, lon);
        userService.addFavourite(user, location, true);
        withPrimary(chatId, user, w -> "📍 Место сохранено!\n\n" + formatter.current(w));
    }

    private void withPrimary(long chatId, AppUser user, java.util.function.Function<WeatherDto, String> format) {
        Optional<Location> primary = userService.primaryLocation(user);
        if (primary.isEmpty()) {
            send(chatId, "Сначала выбери город в приложении или отправь геолокацию 📍", openAppKeyboard());
            return;
        }
        Location location = primary.get();
        WeatherDto weather = presenter.present(location, true, weatherService.getForecast(location), user.getUnits());
        send(chatId, format.apply(weather), openAppKeyboard());
    }

    /** Telegram opens Mini Apps only by https; without it the bot works with commands only. */
    private boolean webAppAvailable() {
        String url = properties.webAppUrl();
        return url != null && url.startsWith("https://");
    }

    private InlineKeyboardMarkup openAppKeyboard() {
        if (!webAppAvailable()) {
            return null;
        }
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text("🌤 Открыть WeatherMate")
                .webApp(new WebAppInfo(properties.webAppUrl()))
                .build();
        return InlineKeyboardMarkup.builder().keyboardRow(new InlineKeyboardRow(button)).build();
    }

    private void send(long chatId, String html, InlineKeyboardMarkup keyboard) {
        try {
            telegram.execute(SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text(html)
                    .parseMode("HTML")
                    .replyMarkup(keyboard)
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Cannot send message to {}: {}", chatId, e.getMessage());
        }
    }

    private void registerCommands() {
        try {
            telegram.execute(SetMyCommands.builder().commands(List.of(
                    new BotCommand("weather", "Погода сейчас"),
                    new BotCommand("week", "Прогноз на 7 дней"),
                    new BotCommand("subscribe", "Ежедневный прогноз"),
                    new BotCommand("unsubscribe", "Отключить прогноз"),
                    new BotCommand("help", "Помощь"))).build());
        } catch (TelegramApiException e) {
            log.warn("Cannot register bot commands: {}", e.getMessage());
        }

        if (!webAppAvailable()) {
            log.warn("app.web-app-url is not set (needs https): the Mini App button is disabled");
            return;
        }
        // "Погода" button to the left of the message field, opens the Mini App
        try {
            telegram.execute(SetChatMenuButton.builder()
                    .menuButton(MenuButtonWebApp.builder()
                            .text("Погода")
                            .webAppInfo(new WebAppInfo(properties.webAppUrl()))
                            .build())
                    .build());
            log.info("Mini App button set: {}", properties.webAppUrl());
        } catch (TelegramApiException e) {
            log.warn("Cannot set the Mini App menu button: {}", e.getMessage());
        }
    }

    private static TelegramUser toTelegramUser(User from) {
        return new TelegramUser(from.getId(), from.getFirstName(), from.getUserName(), from.getLanguageCode());
    }
}
