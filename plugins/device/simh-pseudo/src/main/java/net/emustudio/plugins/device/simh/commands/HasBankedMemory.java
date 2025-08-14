/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class HasBankedMemory implements Command {
    public final static HasBankedMemory INS = new HasBankedMemory();

    @Override
    public byte read(Control control) {
        byte result = (byte) control.getMemory().getBanksCount();
        control.clearCommand();
        return result;
    }

    @Override
    public void start(Control control) {
        control.clearWriteCommand();
    }
}
