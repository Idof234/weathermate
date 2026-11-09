package com.weathermate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings from application.yml (prefix "app").
 *
 * @param botToken        Telegram bot token from @BotFather
 * @param webAppUrl       public HTTPS address of the Mini App
 * @param cacheTtlMinutes how long weather from the API is kept in the database
 * @param devMode         allows the X-Dev-User-Id header instead of Telegram initData (local testing only)
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String botToken,
        String webAppUrl,
        int cacheTtlMinutes,
        boolean devMode
) {
    public AppProperties {
        if (cacheTtlMinutes <= 0) {
            cacheTtlMinutes = 30;
        }
    }

    public boolean botEnabled() {
        return botToken != null && !botToken.isBlank();
    }
}
