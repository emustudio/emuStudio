/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.device.simh.commands;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static net.emustudio.emulib.runtime.helpers.NumberUtils.bin2bcd;

public class GetClockCPM3 implements Command {
    public static final GetClockCPM3 INS = new GetClockCPM3();

    public final static long CPM3_ORIGIN = LocalDateTime
            .of(1977, 12, 31, 0, 0, 0)
            .toEpochSecond(ZoneOffset.UTC);
    public final static int SECONDS_PER_MINUTE = 60;
    public final static int SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE;
    public final static int SECONDS_PER_DAY = 24 * SECONDS_PER_HOUR;


    private boolean currentTimeValid = false;
    private LocalDateTime currentTime;

    private int getClockCPM3Pos = 0; // determines state for sending clock information
    private int daysCPM3SinceOrg = 0; // days since 1 Jan 1978

    @Override
    public void reset(Control control) {
        getClockCPM3Pos = 0;
        currentTimeValid = false;
    }

    @Override
    public byte read(Control control) {
        byte result = 0;
        if (currentTimeValid) {
            switch (getClockCPM3Pos) {
                case 0:
                    result = (byte) (daysCPM3SinceOrg & 0xff);
                    getClockCPM3Pos = 1;
                    break;

                case 1:
                    result = (byte) ((daysCPM3SinceOrg >> 8) & 0xff);
                    getClockCPM3Pos = 2;
                    break;

                case 2:
                    result = (byte) bin2bcd(currentTime.getHour());
                    getClockCPM3Pos = 3;
                    break;

                case 3:
                    result = (byte) bin2bcd(currentTime.getMinute());
                    getClockCPM3Pos = 4;
                    break;

                case 4:
                    result = (byte) bin2bcd(currentTime.getSecond());
                    getClockCPM3Pos = 0;
                    control.clearCommand();
                    break;
            }
        } else {
            getClockCPM3Pos = 0;
            control.clearCommand();
        }

        return result;
    }

    @Override
    public void start(Control control) {
        int delta = SetClockCPM3.INS.ClockCPM3Delta;
        currentTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(Instant.now().getEpochSecond() + delta), ZoneOffset.UTC);
        currentTimeValid = true;
        daysCPM3SinceOrg = (int) ((currentTime.toEpochSecond(ZoneOffset.UTC) - CPM3_ORIGIN) / SECONDS_PER_DAY);
        getClockCPM3Pos = 0;
        control.clearWriteCommand();
    }
}
