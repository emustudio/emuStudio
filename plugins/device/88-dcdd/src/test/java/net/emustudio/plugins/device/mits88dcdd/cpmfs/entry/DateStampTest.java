/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cpmfs.entry;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class DateStampTest {

    @Test
    public void testFirstDayIsJan1_1978() {
        assertEquals(LocalDate.of(1978, 1, 1), DateStamp.FIRST_DAY);
    }

    @Test
    public void testEmptyDateStamp() {
        assertEquals(0, DateStamp.EMPTY.days);
        assertEquals(0, DateStamp.EMPTY.hour);
        assertEquals(0, DateStamp.EMPTY.minute);
    }

    @Test
    public void testDateTimeDays1IsJan1_1978() {
        DateStamp ds = new DateStamp(1, 0, 0);
        assertEquals(LocalDateTime.of(1978, 1, 2, 0, 0), ds.dateTime);
    }

    @Test
    public void testDateTimeWithHoursAndMinutes() {
        DateStamp ds = new DateStamp(365, 14, 30);
        assertEquals(365, ds.days);
        assertEquals(14, ds.hour);
        assertEquals(30, ds.minute);
        assertEquals(LocalDateTime.of(1979, 1, 1, 14, 30), ds.dateTime);
    }

    @Test
    public void testNowReturnsReasonableValues() {
        DateStamp now = DateStamp.now();
        assertTrue(now.days > 0);
        assertTrue(now.hour >= 0 && now.hour <= 23);
        assertTrue(now.minute >= 0 && now.minute <= 59);
    }

    @Test
    public void testDateOnlyZeroesTime() {
        DateStamp ds = new DateStamp(100, 15, 45);
        DateStamp dateOnly = ds.dateOnly();
        assertEquals(100, dateOnly.days);
        assertEquals(0, dateOnly.hour);
        assertEquals(0, dateOnly.minute);
    }

    @Test
    public void testToStringReturnsDateTime() {
        DateStamp ds = new DateStamp(0, 0, 0);
        assertNotNull(ds.toString());
        assertTrue(ds.toString().contains("1978"));
    }
}

