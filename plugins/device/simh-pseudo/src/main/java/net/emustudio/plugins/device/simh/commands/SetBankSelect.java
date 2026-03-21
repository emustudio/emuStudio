/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class SetBankSelect implements Command {
    public final static SetBankSelect INS = new SetBankSelect();

    @Override
    public void write(byte data, Control control) {
        control.getMemory().selectBank(data);
        control.clearCommand();
    }

    @Override
    public void start(Control control) {
        control.clearReadCommand();
    }
}
