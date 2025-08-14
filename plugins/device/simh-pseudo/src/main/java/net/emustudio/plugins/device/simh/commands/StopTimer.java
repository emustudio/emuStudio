/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
