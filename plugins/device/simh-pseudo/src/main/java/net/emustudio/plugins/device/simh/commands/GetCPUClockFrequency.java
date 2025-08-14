/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import java.util.concurrent.atomic.AtomicInteger;

public class GetCPUClockFrequency implements Command {
    public final static GetCPUClockFrequency INS = new GetCPUClockFrequency();
    private final AtomicInteger cpuFreq = new AtomicInteger();
    private int getClockFrequencyPos = 0; // determines state for receiving the clock frequency

    @Override
    public void reset(Control control) {
        getClockFrequencyPos = 0;
    }

    @Override
    public byte read(Control control) {
        byte result;
        if (getClockFrequencyPos == 0) {
            cpuFreq.set(control.getCpu().getCPUFrequency());
            result = (byte) (cpuFreq.get() & 0xff);
            getClockFrequencyPos = 1;
        } else {
            result = (byte) ((cpuFreq.get() >> 8) & 0xff);
            getClockFrequencyPos = 0;
            control.clearCommand();
        }
        return result;
    }

    @Override
    public void start(Control control) {
        getClockFrequencyPos = 0;
        control.clearWriteCommand();
    }
}
