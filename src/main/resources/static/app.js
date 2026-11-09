// WeatherMate Mini App. Talks to the Java backend through /api.
'use strict';

const tg = window.Telegram?.WebApp;
tg?.ready();
tg?.expand();

const $ = (sel) => document.querySelector(sel);
const state = { settings: null, favourites: [] };

// ---------- API ----------

function authHeaders() {
    if (tg?.initData) {
        return { Authorization: 'tma ' + tg.initData };
    }
    // Opened in a normal browser: works only when the backend runs with DEV_MODE=true
    const devId = new URLSearchParams(location.search).get('dev') || '1';
    return { 'X-Dev-User-Id': devId };
}

async function api(method, path, body) {
    const res = await fetch('/api' + path, {
        method,
        headers: { 'Content-Type': 'application/json', ...authHeaders() },
        body: body ? JSON.stringify(body) : undefined,
    });
    if (res.status === 204) return null;
    const data = await res.json().catch(() => null);
    if (!res.ok) {
        const err = new Error(data?.error || 'Ошибка ' + res.status);
        err.status = res.status;
        throw err;
    }
    return data;
}

// ---------- UI helpers ----------

function toast(text) {
    const el = $('#toast');
    el.textContent = text;
    el.classList.remove('hidden');
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => el.classList.add('hidden'), 2500);
}

function haptic(type = 'light') {
    tg?.HapticFeedback?.impactOccurred(type);
}

function escapeHtml(s) {
    return String(s ?? '').replace(/[&<>"']/g, (c) =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function goto(screen) {
    document.querySelectorAll('.screen').forEach((s) => s.classList.toggle('active', s.id === 'screen-' + screen));
    document.querySelectorAll('.tabbar button').forEach((b) => b.classList.toggle('active', b.dataset.goto === screen));
    if (screen === 'cities') $('#search').focus();
}

document.addEventListener('click', (e) => {
    const target = e.target.closest('[data-goto]');
    if (target) {
        haptic();
        goto(target.dataset.goto);
    }
});

// ---------- Weather screen ----------

async function loadWeather() {
    try {
        const w = await api('GET', '/me/weather');
        renderWeather(w);
    } catch (e) {
        if (e.status === 404) {
            $('#weather-empty').classList.remove('hidden');
            $('#weather-content').classList.add('hidden');
        } else {
            toast(e.message);
        }
    }
}

function renderWeather(w) {
    $('#weather-empty').classList.add('hidden');
    $('#weather-content').classList.remove('hidden');

    const u = w.unitSymbol;
    const c = w.current;
    $('#city-switch').textContent = w.location.name;
    $('#now-icon').textContent = c.icon;
    $('#now-temp').textContent = c.temperature + '°';
    $('#now-desc').textContent = c.description;
    $('#now-details').textContent =
        `Ощущается как ${c.feelsLike}${u} · 💨 ${c.windSpeed} м/с · 💧 ${c.humidity}%`;

    const alerts = $('#alerts');
    alerts.classList.toggle('hidden', w.alerts.length === 0);
    alerts.innerHTML = w.alerts.map((a) => `<div>${escapeHtml(a)}</div>`).join('');

    $('#hourly').innerHTML = w.hourly.map((h, i) => `
        <div class="hour">
            <div>${i === 0 ? 'Сейчас' : h.time}</div>
            <div class="icon">${h.icon}</div>
            <div><b>${h.temperature}°</b></div>
            <div class="precip">${h.precipitationChance >= 20 ? h.precipitationChance + '%' : ''}</div>
        </div>`).join('');

    const dayFormat = new Intl.DateTimeFormat('ru', { weekday: 'short' });
    $('#daily').innerHTML = w.daily.map((d, i) => `
        <div class="day">
            <span class="dow">${i === 0 ? 'Сегодня' : dayFormat.format(new Date(d.date + 'T12:00'))}</span>
            <span class="icon">${d.icon}</span>
            <span class="precip">${d.precipitationChance >= 20 ? '☔ ' + d.precipitationChance + '%' : ''}</span>
            <span class="temps"><span class="min">${d.min}°</span><b>${d.max}°</b></span>
        </div>`).join('');

    $('#advice').innerHTML = w.advice.map((a) => `<li>${escapeHtml(a)}</li>`).join('');
}

// ---------- Cities screen ----------

let searchTimer;
$('#search').addEventListener('input', (e) => {
    clearTimeout(searchTimer);
    const q = e.target.value.trim();
    if (q.length < 2) {
        $('#search-results').innerHTML = '';
        return;
    }
    searchTimer = setTimeout(() => search(q), 300);
});

async function search(q) {
    try {
        const lang = state.settings?.language || 'ru';
        const places = await api('GET', `/locations/search?q=${encodeURIComponent(q)}&lang=${lang}`);
        const list = $('#search-results');
        if (places.length === 0) {
            list.innerHTML = '<li class="muted">Ничего не найдено</li>';
            return;
        }
        list.innerHTML = '';
        places.forEach((p) => {
            const li = document.createElement('li');
            li.innerHTML = `<div><div>${escapeHtml(p.name)}</div>
                <div class="sub">${escapeHtml([p.region, p.country].filter(Boolean).join(', '))}</div></div><span>＋</span>`;
            li.addEventListener('click', () => addCity({ place: p, primary: true }));
            list.appendChild(li);
        });
    } catch (e) {
        toast(e.message);
    }
}

async function addCity(request) {
    try {
        haptic('medium');
        await api('POST', '/me/locations', request);
        $('#search').value = '';
        $('#search-results').innerHTML = '';
        await Promise.all([loadFavourites(), loadWeather()]);
        goto('weather');
    } catch (e) {
        toast(e.message);
    }
}

$('#geo-btn').addEventListener('click', () => {
    if (!navigator.geolocation) {
        toast('Геолокация недоступна');
        return;
    }
    navigator.geolocation.getCurrentPosition(
        (pos) => addCity({ latitude: pos.coords.latitude, longitude: pos.coords.longitude, primary: true }),
        () => toast('Не удалось получить местоположение. Можно отправить геолокацию боту в чат 📍'),
        { timeout: 10000 },
    );
});

async function loadFavourites() {
    state.favourites = await api('GET', '/me/locations');
    renderFavourites();
}

function renderFavourites() {
    const list = $('#favourites');
    $('#favourites-empty').classList.toggle('hidden', state.favourites.length > 0);
    list.innerHTML = '';
    state.favourites.forEach((l) => {
        const li = document.createElement('li');
        li.innerHTML = `
            <div><div>${escapeHtml(l.name)} ${l.primary ? '<span class="badge">основной</span>' : ''}</div>
            <div class="sub">${escapeHtml([l.region, l.country].filter(Boolean).join(', '))}</div></div>
            <div class="actions"><button class="icon-btn" title="Удалить">🗑</button></div>`;
        li.addEventListener('click', async (e) => {
            try {
                if (e.target.closest('.icon-btn')) {
                    state.favourites = await api('DELETE', `/me/locations/${l.id}`);
                } else {
                    state.favourites = await api('PUT', `/me/locations/${l.id}/primary`);
                    goto('weather');
                }
                haptic();
                renderFavourites();
                loadWeather();
            } catch (err) {
                toast(err.message);
            }
        });
        list.appendChild(li);
    });
}

// ---------- Settings screen ----------

function renderSettings() {
    const s = state.settings;
    document.querySelectorAll('#units button').forEach((b) => b.classList.toggle('active', b.dataset.value === s.units));
    $('#daily-enabled').checked = s.dailyEnabled;
    $('#alerts-enabled').checked = s.alertsEnabled;
    $('#notify-time').value = s.notifyTime;
    $('#timezone').textContent = s.timezone;
}

async function saveSettings(patch) {
    try {
        const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
        state.settings = await api('PUT', '/me/settings', { ...patch, timezone });
        renderSettings();
        haptic();
        if (patch.units) loadWeather();
    } catch (e) {
        toast(e.message);
    }
}

document.querySelectorAll('#units button').forEach((b) =>
    b.addEventListener('click', () => saveSettings({ units: b.dataset.value })));
$('#daily-enabled').addEventListener('change', (e) => saveSettings({ dailyEnabled: e.target.checked }));
$('#alerts-enabled').addEventListener('change', (e) => saveSettings({ alertsEnabled: e.target.checked }));
$('#notify-time').addEventListener('change', (e) => saveSettings({ notifyTime: e.target.value }));

// ---------- Start ----------

(async function init() {
    try {
        const me = await api('GET', '/me');
        state.settings = me.settings;
        state.favourites = me.favourites;
        renderSettings();
        renderFavourites();
        await loadWeather();
    } catch (e) {
        if (e.status === 401) {
            document.body.innerHTML =
                '<div class="empty"><div class="empty-icon">🔒</div><p>Открой приложение через Telegram-бота</p></div>';
        } else {
            toast(e.message);
        }
    }
})();
