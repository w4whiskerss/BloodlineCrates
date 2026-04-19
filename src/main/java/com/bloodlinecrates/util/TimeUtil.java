package com.bloodlinecrates.util;

import java.time.Duration;

public final class TimeUtil {
    private TimeUtil() {
    }

    public static String formatMillis(long millis) {
        if (millis <= 0) {
            return "0s";
        }
        Duration duration = Duration.ofMillis(millis);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
