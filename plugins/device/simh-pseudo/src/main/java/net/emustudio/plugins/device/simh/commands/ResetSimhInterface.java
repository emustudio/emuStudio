/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class ResetSimhInterface implements Command {
    public final static ResetSimhInterface INS = new ResetSimhInterface();

    @Override
    public void start(Control control) {
        StartTimer.INS.markTimeSP = 0;
        control.clearCommand();
        GetHostFilenames.INS.reset(control);
        control.clearCommand();
    }
}
