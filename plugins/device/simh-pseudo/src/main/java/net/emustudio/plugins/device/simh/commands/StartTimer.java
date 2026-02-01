/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class StartTimer implements Command {
    public final static StartTimer INS = new StartTimer();

    private final static int TIMER_STACK_LIMIT = 10; // stack depth of timer stack
    // stop watch and timer related
    public int markTimeSP = 0; // stack pointer for timer stack
    public long[] markTime = new long[TIMER_STACK_LIMIT];  // timer stack

    @Override
    public void reset(Control control) {
        markTimeSP = 0;
    }

    @Override
    public void start(Control control) {
        if (markTimeSP < TIMER_STACK_LIMIT) {
            markTime[markTimeSP++] = System.currentTimeMillis();
        } else {
            System.out.println("SIMH: Timer stack overflow");
        }
        control.clearCommand();
    }
}
