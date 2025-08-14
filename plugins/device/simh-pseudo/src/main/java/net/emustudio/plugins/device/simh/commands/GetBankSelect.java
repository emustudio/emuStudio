/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class GetBankSelect implements Command {
    public final static GetBankSelect INS = new GetBankSelect();

    @Override
    public byte read(Control control) {
        byte result = (byte) control.getMemory().getSelectedBank();
        control.clearCommand();
        return result;
    }

    @Override
    public void start(Control control) {
        control.clearWriteCommand();
    }
}
