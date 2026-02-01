/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class StopTimerInterrupts implements Command {
    public final static StopTimerInterrupts INS = new StopTimerInterrupts();

    @Override
    public void start(Control control) {
        StartTimerInterrupts.INS.reset(control);
        control.clearCommand();
    }
}
