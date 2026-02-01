/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class Set8080CPU implements Command {
    public final static Set8080CPU INS = new Set8080CPU();

    @Override
    public void start(Control control) {
        System.out.println("SIMH: Set 8080 CPU command not supported!");
        control.clearCommand();
    }
}
