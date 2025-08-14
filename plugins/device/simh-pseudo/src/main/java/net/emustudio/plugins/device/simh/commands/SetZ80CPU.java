/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class SetZ80CPU implements Command {
    public final static SetZ80CPU INS = new SetZ80CPU();

    @Override
    public void start(Control control) {
        System.out.println("SIMH: Set Z80 CPU command not supported!");
        control.clearCommand();
    }
}
