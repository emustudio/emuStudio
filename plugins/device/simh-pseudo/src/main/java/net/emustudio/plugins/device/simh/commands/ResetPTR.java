/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class ResetPTR implements Command {
    public final static ResetPTR INS = new ResetPTR();

    @Override
    public void start(Control control) {
        //ptr_reset( & ptr_dev);
        control.clearCommand();
    }
}
