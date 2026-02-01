/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class PrintTime implements Command {
    public final static PrintTime INS = new PrintTime();

    @Override
    public void start(Control control) {
        System.out.printf("SIMH: Current time in milliseconds = %d.\n", System.currentTimeMillis());
        control.clearCommand();
    }
}
