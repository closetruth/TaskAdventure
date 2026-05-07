package com.closetruth.season;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;

public record WeekBounds(String weekKey, Instant startInclusive, Instant endExclusive, String rangeLabel) {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    public static WeekBounds current(ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        Instant start = monday.atStartOfDay(zone).toInstant();
        Instant end = monday.plusWeeks(1).atStartOfDay(zone).toInstant();
        int y = monday.get(IsoFields.WEEK_BASED_YEAR);
        int w = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        String key = y + "-W" + w;
        String label = monday.format(DAY) + " ~ " + sunday.format(DAY);
        return new WeekBounds(key, start, end, label);
    }
}
