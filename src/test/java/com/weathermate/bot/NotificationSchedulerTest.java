package com.weathermate.bot;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSchedulerTest {

    private static final LocalTime TARGET = LocalTime.of(8, 0);

    @Test
    void dueExactlyAtTargetTime() {
        assertThat(NotificationScheduler.isDue(LocalTime.of(8, 0), TARGET)).isTrue();
    }

    @Test
    void dueShortlyAfterTarget() {
        assertThat(NotificationScheduler.isDue(LocalTime.of(8, 29), TARGET)).isTrue();
    }

    @Test
    void notDueBeforeOrLongAfter() {
        assertThat(NotificationScheduler.isDue(LocalTime.of(7, 59), TARGET)).isFalse();
        assertThat(NotificationScheduler.isDue(LocalTime.of(8, 30), TARGET)).isFalse();
        assertThat(NotificationScheduler.isDue(LocalTime.of(15, 0), TARGET)).isFalse();
    }
}
