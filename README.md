# 🌤 WeatherMate

Telegram-бот с погодой и **Mini App** (веб-приложение внутри Telegram).
Бэкенд полностью на **Java 21 + Spring Boot 3**, база данных **H2** (файл, ничего устанавливать не нужно).

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
                                        ├─► Services ─► H2 / PostgreSQL (Flyway)
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

## Запуск (IntelliJ IDEA + cloudflared, без Docker)

1. Установи **JDK 21** и **IntelliJ IDEA**.
2. Открой проект: **File → New → Project from Version Control**, вставь ссылку на этот репозиторий.
   IntelliJ сам скачает зависимости (Maven). Подожди, пока внизу закончится индексация.
3. Создай бота у [@BotFather](https://t.me/BotFather) (`/newbot`) и получи токен.
4. В корне проекта скопируй файл `local.properties.example` в `local.properties` и впиши токен:
   ```properties
   app.bot-token=123456:ABC...
   ```
   Файл `local.properties` есть в `.gitignore`, поэтому в GitHub он не попадёт. **Никогда не коммить токен.**
5. Запусти `WeatherMateApplication` (зелёная стрелка рядом с `main`).
6. Напиши своему боту `/start`. Команды `/weather` и `/week` работают уже сейчас,
   а город можно выбрать, отправив боту геолокацию 📍

Без токена приложение тоже запускается: работает API, а бот выключен.

База данных **H2** хранится в файле `data/weathermate.mv.db`, ничего устанавливать не нужно.
Посмотреть таблицы можно в браузере: <http://localhost:8080/h2-console>
(JDBC URL `jdbc:h2:file:./data/weathermate`, пользователь `sa`, пароль пустой).

### Открыть Mini App в Telegram

Telegram открывает Mini App только по **https**. Для разработки удобно сделать временный
публичный адрес через [cloudflared](https://github.com/cloudflare/cloudflared/releases)
(регистрация не нужна, хватит одного .exe):

```
.\cloudflared-windows-amd64.exe tunnel --url http://localhost:8080
```

Он покажет адрес вида `https://something.trycloudflare.com`. Впиши его в `local.properties`:

```properties
app.web-app-url=https://something.trycloudflare.com
```

Перезапусти приложение. Бот сам поставит кнопку «Погода» слева от поля ввода.
Адрес меняется при каждом запуске cloudflared, тогда его нужно обновить.

### Посмотреть в браузере (без Telegram)

Добавь в `local.properties`:

```properties
app.dev-mode=true
```

Запусти приложение и открой <http://localhost:8080/?dev=123456789> (вместо числа можно свой Telegram ID,
узнать его можно у [@userinfobot](https://t.me/userinfobot)). Это режим разработки:
запросы без подписи Telegram выполняются от имени этого пользователя. **На сервере его не включай.**

## Тесты

```bash
mvn test
```
Юнит-тесты: проверка подписи initData, советы по одежде, предупреждения, коды погоды, расписание рассылки.
Тест контекста запускает приложение на H2. Тесты запускаются на GitHub Actions при каждом push. В IntelliJ: правый клик по папке `src/test/java` → **Run 'All Tests'**.

## Технологии

Java 21 · Spring Boot 3 (Web, Data JPA, Validation, Scheduling) · H2 (совместима с PostgreSQL) · Flyway ·
TelegramBots 9 · Open-Meteo API · JUnit 5 · GitHub Actions
