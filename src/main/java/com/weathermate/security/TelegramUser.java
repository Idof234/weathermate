package com.weathermate.security;

/** The Telegram user taken from verified Mini App initData (or from a bot update). */
public record TelegramUser(long id, String firstName, String username, String languageCode) {
}
