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

public class GetClockZSDOS implements Command {
    public final static GetClockZSDOS INS = new GetClockZSDOS();

    private boolean currentTimeValid = false;
    private LocalDateTime currentTime;

    // ZSDOS clock definitions
   // private int ClockZSDOSDelta = 0; // delta between real clock and Altair clock
    private int getClockZSDOSPos = 0; // determines state for sending clock information

    @Override
    public void reset(Control control) {
        currentTimeValid = false;
        getClockZSDOSPos = 0;
    }

    @Override
    public byte read(Control control) {
        byte result = 0;
        if (currentTimeValid) {
            switch (getClockZSDOSPos) {

                case 0:
                    result = (byte) bin2bcd(currentTime.getYear() % 100);
                    getClockZSDOSPos = 1;
                    break;

                case 1:
                    result = (byte) bin2bcd(currentTime.getMonthValue());
                    getClockZSDOSPos = 2;
                    break;

                case 2:
                    result = (byte) bin2bcd(currentTime.getDayOfMonth());
                    getClockZSDOSPos = 3;
                    break;

                case 3:
                    result = (byte) bin2bcd(currentTime.getHour());
                    getClockZSDOSPos = 4;
                    break;

                case 4:
                    result = (byte) bin2bcd(currentTime.getMinute());
                    getClockZSDOSPos = 5;
                    break;

                case 5:
                    result = (byte) bin2bcd(currentTime.getSecond());
                    getClockZSDOSPos = 0;
                    control.clearCommand();
                    break;
            }
        } else {
            getClockZSDOSPos = 0;
            control.clearCommand();
        }
        return result;
    }

    @Override
    public void start(Control control) {
        int delta = SetClockZSDOS.INS.ClockZSDOSDelta;
        currentTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(Instant.now().getEpochSecond() + delta), ZoneOffset.UTC);
        currentTimeValid = true;
        getClockZSDOSPos = 0;
        control.clearWriteCommand();
    }
}
