-- WeatherMate database schema

CREATE TABLE users (
    id           BIGSERIAL PRIMARY KEY,
    telegram_id  BIGINT       NOT NULL UNIQUE,
    username     VARCHAR(64),
    first_name   VARCHAR(128),
    language     VARCHAR(8)   NOT NULL DEFAULT 'ru',
    units        VARCHAR(16)  NOT NULL DEFAULT 'CELSIUS',
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE locations (
    id           BIGSERIAL PRIMARY KEY,
    external_id  BIGINT       NOT NULL UNIQUE,      -- id from Open-Meteo geocoding
    name         VARCHAR(128) NOT NULL,
    region       VARCHAR(128),
    country      VARCHAR(128),
    country_code VARCHAR(4),
    latitude     DOUBLE PRECISION NOT NULL,
    longitude    DOUBLE PRECISION NOT NULL,
    timezone     VARCHAR(64)
);

CREATE TABLE user_locations (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT  NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    location_id  BIGINT  NOT NULL REFERENCES locations (id) ON DELETE CASCADE,
    is_primary   BOOLEAN NOT NULL DEFAULT FALSE,
    added_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_location UNIQUE (user_id, location_id)
);

CREATE TABLE subscriptions (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT  NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    notify_time    TIME    NOT NULL DEFAULT '08:00',
    timezone       VARCHAR(64) NOT NULL DEFAULT 'Europe/Tallinn',
    daily_enabled  BOOLEAN NOT NULL DEFAULT FALSE,
    alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE weather_cache (
    id           BIGSERIAL PRIMARY KEY,
    location_id  BIGINT    NOT NULL UNIQUE REFERENCES locations (id) ON DELETE CASCADE,
    json_data    TEXT      NOT NULL,
    fetched_at   TIMESTAMP NOT NULL
);

CREATE TABLE notification_log (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type         VARCHAR(16) NOT NULL,   -- DAILY or ALERT
    sent_date    DATE        NOT NULL,
    sent_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_notification UNIQUE (user_id, type, sent_date)
);

CREATE INDEX idx_user_locations_user ON user_locations (user_id);
