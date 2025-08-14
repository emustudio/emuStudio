/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class GetHostOSPathSeparator implements Command {
    public final static GetHostOSPathSeparator INS = new GetHostOSPathSeparator();

    @Override
    public byte read(Control control) {
        return (byte) GetHostFilenames.hostPathSeparator;
    }

    @Override
    public void start(Control control) {
        control.clearWriteCommand();
    }
}
