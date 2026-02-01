/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class SetCPUClockFrequency implements Command {
    public final static SetCPUClockFrequency INS = new SetCPUClockFrequency();

    private int newClockFrequency;
    private int setClockFrequencyPos = 0; // determines state for sending the clock frequency

    @Override
    public void reset(Control control) {
        setClockFrequencyPos = 0;
    }

    @Override
    public void write(byte data, Control control) {
        if (setClockFrequencyPos == 0) {
            newClockFrequency = data & 0xFF;
            setClockFrequencyPos = 1;
        } else {
            control.getCpu().setCPUFrequency(((data << 8) & 0xFF00) | newClockFrequency);
            setClockFrequencyPos = 0;
            control.clearCommand();
        }
    }

    @Override
    public void start(Control control) {
        setClockFrequencyPos = 0;
        control.clearReadCommand();
    }
}
