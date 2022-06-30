package net.emustudio.plugins.device.mits88dcdd.cpmfs.entry;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Override
    public String toString() {
        return dateTime.toString();
    }
}
