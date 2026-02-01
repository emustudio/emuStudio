/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class ResetStopWatch implements Command {
    public final static ResetStopWatch INS = new ResetStopWatch();

    @Override
    public void start(Control control) {
        ReadStopWatch.INS.stopWatchNow = System.currentTimeMillis();
        control.clearCommand();
    }
}
