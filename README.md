# 🌤 WeatherMate

Telegram-бот с погодой и **Mini App** (веб-приложение внутри Telegram).
Бэкенд полностью на **Java 21 + Spring Boot 3**, база данных **PostgreSQL**.

## Возможности

**Бот**
- `/start`: приветствие и кнопка, которая открывает Mini App
- `/weather`: погода сейчас в выбранном городе и совет, что надеть
- `/week`: прогноз на 7 дней
- `/subscribe` и `/unsubscribe`: ежедневный прогноз в выбранное время
- отправка геолокации в чат сохраняет это место как основное
- вечером бот предупреждает, если завтра дождь, снег, гроза, мороз, жара или сильный ветер

**Mini App**
- поиск города с автодополнением или определение по геолокации
- текущая погода, прогноз по часам на 24 часа и на 7 дней
- совет «что надеть»
- избранные города и выбор основного
- настройки: °C/°F, время рассылки, предупреждения
- цвета берутся из темы Telegram (светлая и тёмная)

## Архитектура

```
Telegram ──► WeatherBot (long polling) ─┐
                                        ├─► Services ─► PostgreSQL (Flyway)
Mini App (HTML/JS) ──► REST API /api ───┘      │
                                               └─► Open-Meteo API (погода + геокодинг)
```

| Пакет | Что внутри |
|---|---|
| `api` | REST-контроллеры и DTO |
| `bot` | Telegram-бот, форматирование сообщений, планировщик рассылки |
| `client` | HTTP-клиент Open-Meteo |
| `config` | настройки приложения |
| `entity`, `repository` | JPA-сущности и Spring Data репозитории |
| `security` | проверка Telegram initData (HMAC-SHA256) |
| `service` | бизнес-логика: погода с кэшем, пользователи, советы, предупреждения |

### Безопасность

Mini App отправляет на сервер `initData` от Telegram в заголовке `Authorization: tma <initData>`.
`InitDataValidator` проверяет подпись HMAC-SHA256 токеном бота
([алгоритм из документации Telegram](https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app)),
поэтому сервер точно знает, какой пользователь делает запрос, и отдельный логин не нужен.

### Кэш погоды

Ответы Open-Meteo хранятся в таблице `weather_cache` 30 минут. Если API недоступен,
возвращается последний сохранённый прогноз.

## База данных

| Таблица | Назначение |
|---|---|
| `users` | пользователи Telegram, язык, единицы |
| `locations` | города (из геокодинга или по координатам) |
| `user_locations` | избранные города пользователя, флаг основного |
| `subscriptions` | время рассылки, часовой пояс, предупреждения |
| `weather_cache` | кэш ответов Open-Meteo |
| `notification_log` | что уже отправлено (без дублей) |

Схема создаётся миграцией Flyway: [`V1__init.sql`](src/main/resources/db/migration/V1__init.sql).

## REST API

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/locations/search?q=Tallinn&lang=ru` | поиск городов |
| GET | `/api/weather/{locationId}?units=CELSIUS` | погода по id города |
| GET | `/api/me` | профиль, настройки, избранное |
| GET | `/api/me/weather` | погода в основном городе |
| GET | `/api/me/locations` | избранные города |
| POST | `/api/me/locations` | добавить город (`place`) или координаты (`latitude`, `longitude`) |
| PUT | `/api/me/locations/{id}/primary` | сделать основным |
| DELETE | `/api/me/locations/{id}` | удалить из избранного |
| GET / PUT | `/api/me/settings` | настройки |

## Запуск

### 1. Создать бота
1. Напиши [@BotFather](https://t.me/BotFather), выполни `/newbot` и получи токен.
2. Скопируй `.env.example` в `.env` и впиши `BOT_TOKEN`.

### 2. Получить HTTPS-адрес
Telegram открывает Mini App только по HTTPS. Для разработки подойдёт туннель:
```bash
cloudflared tunnel --url http://localhost:8080
# или
ngrok http 8080
```
Впиши полученный адрес в `WEB_APP_URL` в `.env`.

### 3. Запустить

**Через Docker (проще всего):**
```bash
docker compose up --build
```

**Без Docker:** нужны JDK 21, Maven и PostgreSQL.
```bash
docker compose up -d db        # или свой PostgreSQL
export $(cat .env | xargs)     # Windows: задай переменные в IDE
mvn spring-boot:run
```

Напиши боту `/start` и нажми «Открыть WeatherMate».

### Тестирование в обычном браузере
Запусти с `DEV_MODE=true` и открой `http://localhost:8080/?dev=1`.
Тогда API принимает заголовок `X-Dev-User-Id` вместо подписи Telegram. В продакшене не включать!

## Тесты

```bash
mvn test
```
Юнит-тесты: проверка подписи initData, советы по одежде, предупреждения, коды погоды, расписание рассылки.
Тест контекста запускает приложение на H2. Тесты запускаются на GitHub Actions при каждом push.

## Технологии

Java 21 · Spring Boot 3 (Web, Data JPA, Validation, Scheduling) · PostgreSQL · Flyway ·
TelegramBots 9 · Open-Meteo API · JUnit 5 · Docker · GitHub Actions
