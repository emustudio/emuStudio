/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cpmfs.entry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DateStamp {
    public final static LocalDate FIRST_DAY = LocalDate.of(1978, 1, 1);
    public final static DateStamp EMPTY = new DateStamp(0, 0, 0);

    public final int days; // Julian day; day1 = 1 Jan 1978
    public final int hour; // not in BCD
    public final int minute; // not in BCD

    public final LocalDateTime dateTime;

    public DateStamp(int days, int hour, int minute) {
        this.days = days;
        this.hour = hour;
        this.minute = minute;
        this.dateTime = FIRST_DAY.plusDays(days).atTime(hour, minute);
    }

    /**
     * Creates a DateStamp from the current system date and time.
     *
     * @return DateStamp representing current date/time in CP/M format
     */
    public static DateStamp now() {
        LocalDateTime now = LocalDateTime.now();
        int days = (int) ChronoUnit.DAYS.between(FIRST_DAY, now.toLocalDate());
        return new DateStamp(days, now.getHour(), now.getMinute());
    }

    /**
     * Creates a DateStamp with only the date portion (hour and minute set to 0).
     * Used by NATIVE format where creation timestamp has only the date.
     *
     * @return DateStamp with only date portion
     */
    public DateStamp dateOnly() {
        return new DateStamp(days, 0, 0);
    }

    @Override
    public String toString() {
        return dateTime.toString();
    }
}
