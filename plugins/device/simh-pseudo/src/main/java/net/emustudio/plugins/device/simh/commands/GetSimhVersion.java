/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class GetSimhVersion implements Command {
    public final static GetSimhVersion INS = new GetSimhVersion();

    private static final byte[] version = "SIMH004\0".getBytes();

    private int versionPos = 0; // determines state for sending device identifier

    @Override
    public void reset(Control control) {
        versionPos = 0;
    }

    @Override
    public byte read(Control control) {
        byte result = version[versionPos++];
        if (result == 0) {
            versionPos = 0;
            control.clearCommand();
        }
        return result;
    }

    @Override
    public void start(Control control) {
        versionPos = 0;
        control.clearWriteCommand();
    }
}
