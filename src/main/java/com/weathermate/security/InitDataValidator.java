package com.weathermate.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weathermate.config.AppProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Checks the initData string that Telegram passes to a Mini App.
 * Algorithm from https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app :
 * <pre>
 *   secret_key = HMAC_SHA256(key = "WebAppData", message = bot_token)
 *   hash       = hex(HMAC_SHA256(key = secret_key, message = data_check_string))
 * </pre>
 * where data_check_string is every field except "hash", sorted by key, as "key=value" joined with '\n'.
 * Only Telegram knows the bot token, so a valid hash proves who the user is.
 */
@Component
public class InitDataValidator {

    static final Duration MAX_AGE = Duration.ofHours(24);
    private static final String HMAC = "HmacSHA256";

    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public InitDataValidator(AppProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public Optional<TelegramUser> validate(String initData) {
        if (initData == null || initData.isBlank() || !properties.botEnabled()) {
            return Optional.empty();
        }
        Map<String, String> fields = parse(initData);
        String receivedHash = fields.remove("hash");
        if (receivedHash == null) {
            return Optional.empty();
        }

        String dataCheckString = fields.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));
        String expectedHash = sign(dataCheckString);
        if (!MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.UTF_8),
                receivedHash.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }

        if (!isFresh(fields.get("auth_date"))) {
            return Optional.empty();
        }
        return parseUser(fields.get("user"));
    }

    /** Package-private so tests can build valid initData. */
    String sign(String dataCheckString) {
        try {
            byte[] secretKey = hmac("WebAppData".getBytes(StandardCharsets.UTF_8),
                    properties.botToken().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmac(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }

    private static byte[] hmac(byte[] key, byte[] message) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC);
        mac.init(new SecretKeySpec(key, HMAC));
        return mac.doFinal(message);
    }

    private static Map<String, String> parse(String initData) {
        Map<String, String> fields = new TreeMap<>();
        for (String pair : initData.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            fields.put(key, value);
        }
        return fields;
    }

    private boolean isFresh(String authDate) {
        if (authDate == null) return false;
        try {
            Instant signedAt = Instant.ofEpochSecond(Long.parseLong(authDate));
            return Duration.between(signedAt, clock.instant()).compareTo(MAX_AGE) <= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Optional<TelegramUser> parseUser(String userJson) {
        if (userJson == null) return Optional.empty();
        try {
            JsonNode node = objectMapper.readTree(userJson);
            if (!node.hasNonNull("id")) return Optional.empty();
            return Optional.of(new TelegramUser(
                    node.get("id").asLong(),
                    node.path("first_name").asText(null),
                    node.path("username").asText(null),
                    node.path("language_code").asText(null)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
