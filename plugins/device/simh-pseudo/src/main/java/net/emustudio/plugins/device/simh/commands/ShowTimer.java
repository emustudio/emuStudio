/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class ShowTimer implements Command {
    public final static ShowTimer INS = new ShowTimer();

    @Override
    public void start(Control control) {
        if (StartTimer.INS.markTimeSP > 0) {
            long delta = System.currentTimeMillis() - StartTimer.INS.markTime[StartTimer.INS.markTimeSP - 1];
            System.out.printf("SIMH: Timer running. Elapsed in milliseconds = %d.\n", delta);
        } else {
            System.out.println("SIMH: No timer active.");
        }
        control.clearCommand();
    }
}
