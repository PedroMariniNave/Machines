package com.zpedroo.voltzmachines.utils.formatter;

import java.util.concurrent.TimeUnit;

import static com.zpedroo.voltzmachines.utils.config.Messages.*;

public class TimeFormatter {

    private static TimeFormatter instance;
    public static TimeFormatter getInstance() { return instance; }

    public TimeFormatter() {
        instance = this;
    }

    public String format(Long time) {
        long days = TimeUnit.MILLISECONDS.toDays(time);
        long hours = TimeUnit.MILLISECONDS.toHours(time) - (days * 24);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time) - (TimeUnit.MILLISECONDS.toHours(time) * 60);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time) - (TimeUnit.MILLISECONDS.toMinutes(time) * 60);

        StringBuilder builder = new StringBuilder();

        if (days > 0) builder.append(days).append(" ").append(days == 1 ? DAY : DAYS).append(" ");
        if (hours > 0) builder.append(hours).append(" ").append(hours == 1 ? HOUR : HOURS).append(" ");
        if (minutes > 0) builder.append(minutes).append(" ").append(minutes == 1 ? MINUTE : MINUTES).append(" ");
        if (seconds > 0) builder.append(seconds).append(" ").append(seconds == 1 ? SECOND : SECONDS);

        String ret = builder.toString();

        return ret.isEmpty() ? NOW : ret;
    }
}