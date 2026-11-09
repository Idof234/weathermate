package com.weathermate.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weathermate.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class InitDataValidatorTest {

    private static final String TOKEN = "123456:TEST-TOKEN";
    private static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");

    private final InitDataValidator validator = new InitDataValidator(
            new AppProperties(TOKEN, "https://example.com", 30, false),
            new ObjectMapper(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void acceptsCorrectlySignedData() {
        String initData = signed(NOW.getEpochSecond());

        var user = validator.validate(initData);

        assertThat(user).isPresent();
        assertThat(user.get().id()).isEqualTo(42L);
        assertThat(user.get().firstName()).isEqualTo("Тихон");
        assertThat(user.get().username()).isEqualTo("tihon");
        assertThat(user.get().languageCode()).isEqualTo("ru");
    }

    @Test
    void rejectsTamperedData() {
        String initData = signed(NOW.getEpochSecond()).replace("%22id%22%3A42", "%22id%22%3A43");

        assertThat(validator.validate(initData)).isEmpty();
    }

    @Test
    void rejectsDataSignedWithAnotherToken() {
        InitDataValidator other = new InitDataValidator(
                new AppProperties("999:OTHER", "https://example.com", 30, false),
                new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(other.validate(signed(NOW.getEpochSecond()))).isEmpty();
    }

    @Test
    void rejectsExpiredData() {
        long twoDaysAgo = NOW.minusSeconds(2 * 24 * 3600).getEpochSecond();

        assertThat(validator.validate(signed(twoDaysAgo))).isEmpty();
    }

    @Test
    void rejectsMissingHashAndGarbage() {
        assertThat(validator.validate("auth_date=1&user=%7B%7D")).isEmpty();
        assertThat(validator.validate("")).isEmpty();
        assertThat(validator.validate(null)).isEmpty();
        assertThat(validator.validate("not-init-data")).isEmpty();
    }

    /** Builds initData the same way Telegram does. */
    private String signed(long authDate) {
        Map<String, String> fields = new TreeMap<>();
        fields.put("auth_date", String.valueOf(authDate));
        fields.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
        fields.put("user", "{\"id\":42,\"first_name\":\"Тихон\",\"username\":\"tihon\",\"language_code\":\"ru\"}");

        String dataCheckString = fields.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));
        String hash = validator.sign(dataCheckString);

        return fields.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&")) + "&hash=" + hash;
    }
}
