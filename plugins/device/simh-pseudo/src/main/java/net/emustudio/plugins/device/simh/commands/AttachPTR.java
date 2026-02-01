/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class AttachPTR implements Command {
    public final static AttachPTR INS = new AttachPTR();

    @Override
    public byte read(Control control) {
        return 0;
    }

    @Override
    public void start(Control control) {
        //attachCPM( & ptr_unit);
        control.clearWriteCommand();
    }
}
