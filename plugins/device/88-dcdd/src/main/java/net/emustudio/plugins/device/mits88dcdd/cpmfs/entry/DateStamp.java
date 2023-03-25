/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
