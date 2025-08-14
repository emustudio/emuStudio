/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class DetachPTR implements Command {
    public final static DetachPTR INS = new DetachPTR();

    @Override
    public void start(Control control) {
        //detach_unit( & ptr_unit);
        control.clearCommand();
    }
}
