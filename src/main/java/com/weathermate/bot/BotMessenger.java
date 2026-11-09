package com.weathermate.bot;

/** Sends messages to Telegram chats. Separate interface so the scheduler can be tested without Telegram. */
public interface BotMessenger {

    /** @return true if the message was delivered */
    boolean sendHtml(long chatId, String html);
}
