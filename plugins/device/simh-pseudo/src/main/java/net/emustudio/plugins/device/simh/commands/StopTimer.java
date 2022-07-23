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

public class StopTimer implements Command {
    public final static StopTimer INS = new StopTimer();

    @Override
    public void start(Control control) {
        if (StartTimer.INS.markTimeSP > 0) {
            StartTimer.INS.markTimeSP -= 1;
            long delta = System.currentTimeMillis() - StartTimer.INS.markTime[StartTimer.INS.markTimeSP];
            System.out.printf("SIMH: Timer stopped. Elapsed time in milliseconds = %d.\n", delta);
        } else {
            System.out.println("SIMH: No timer active");
        }
        control.clearCommand();
    }
}
