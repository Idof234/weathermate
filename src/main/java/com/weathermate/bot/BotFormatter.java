package com.weathermate.bot;

import com.weathermate.api.dto.WeatherDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/** Builds the HTML text of bot messages. */
@Component
public class BotFormatter {

    private static final Locale RU = Locale.forLanguageTag("ru");

    public String current(WeatherDto w) {
        WeatherDto.Current c = w.current();
        String u = w.unitSymbol();
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(escape(w.location().displayName())).append("</b>\n\n");
        sb.append(c.icon()).append(" <b>").append(c.temperature()).append(u).append("</b>, ")
                .append(c.description().toLowerCase(RU)).append('\n');
        sb.append("Ощущается как ").append(c.feelsLike()).append(u).append('\n');
        sb.append("💨 ").append(c.windSpeed()).append(" м/с   💧 ").append(c.humidity()).append("%\n");

        if (!w.daily().isEmpty()) {
            WeatherDto.Day today = w.daily().get(0);
            sb.append("Сегодня: ").append(today.min()).append("…").append(today.max()).append(u)
                    .append(", осадки ").append(today.precipitationChance()).append("%\n");
        }
        appendList(sb, "\n👕 <b>Что надеть</b>", w.advice());
        appendList(sb, "\n⚠️ <b>Завтра</b>", w.alerts());
        return sb.toString();
    }

    public String week(WeatherDto w) {
        StringBuilder sb = new StringBuilder("<b>Прогноз на неделю: ")
                .append(escape(w.location().name())).append("</b>\n\n");
        for (WeatherDto.Day d : w.daily()) {
            LocalDate date = LocalDate.parse(d.date());
            String dow = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, RU);
            sb.append(d.icon()).append(' ').append(dow).append(' ').append(date.getDayOfMonth())
                    .append(": ").append(d.min()).append("…").append(d.max()).append(w.unitSymbol());
            if (d.precipitationChance() >= 30) {
                sb.append(", ☔ ").append(d.precipitationChance()).append('%');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public String alerts(String locationName, List<String> alerts) {
        StringBuilder sb = new StringBuilder("⚠️ <b>Предупреждение о погоде: ")
                .append(escape(locationName)).append("</b>\n\n");
        alerts.forEach(a -> sb.append(a).append('\n'));
        return sb.toString();
    }

    private static void appendList(StringBuilder sb, String title, List<String> items) {
        if (items == null || items.isEmpty()) return;
        sb.append(title).append('\n');
        items.forEach(i -> sb.append("• ").append(escape(i)).append('\n'));
    }

    static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
