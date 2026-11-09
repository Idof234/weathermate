package com.weathermate.bot;

import com.weathermate.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Bot beans are created only when BOT_TOKEN is set. Without a token the app still runs
 * (API + Mini App), and messages are just written to the log.
 */
@Configuration
public class BotConfig {

    static final String BOT_ENABLED = "!'${app.bot-token:}'.isBlank()";
    static final String BOT_DISABLED = "'${app.bot-token:}'.isBlank()";

    private static final Logger log = LoggerFactory.getLogger(BotConfig.class);

    @Bean
    @ConditionalOnExpression(BOT_ENABLED)
    public TelegramClient telegramClient(AppProperties properties) {
        return new OkHttpTelegramClient(properties.botToken());
    }

    @Bean
    @ConditionalOnExpression(BOT_ENABLED)
    public BotMessenger telegramMessenger(TelegramClient client) {
        return (chatId, html) -> {
            try {
                client.execute(SendMessage.builder()
                        .chatId(String.valueOf(chatId))
                        .text(html)
                        .parseMode("HTML")
                        .build());
                return true;
            } catch (TelegramApiException e) {
                log.warn("Cannot send message to {}: {}", chatId, e.getMessage());
                return false;
            }
        };
    }

    @Bean
    @ConditionalOnExpression(BOT_DISABLED)
    public BotMessenger loggingMessenger() {
        log.warn("BOT_TOKEN is not set: the Telegram bot is disabled");
        return (chatId, html) -> {
            log.info("[bot disabled] message to {}:\n{}", chatId, html);
            return true;
        };
    }
}
