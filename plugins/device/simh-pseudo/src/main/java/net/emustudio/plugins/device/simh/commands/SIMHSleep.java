/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import java.util.concurrent.TimeUnit;

public class SIMHSleep implements Command {
    public final static SIMHSleep INS = new SIMHSleep();
    private final static long SIMHSleepMillis = TimeUnit.NANOSECONDS.toMillis(1000000);

    @Override
    public void start(Control control) {
        // Do not sleep when timer interrupts are pending or are about to be created.
        // Otherwise, there is the possibility that such interrupts are skipped.

        // time to sleep and SIO not attached to a file.
        if (StartTimerInterrupts.INS.callback.get() == null) {
            try {
                Thread.sleep(SIMHSleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        control.clearCommand();
    }
}
