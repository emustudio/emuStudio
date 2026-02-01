/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class SetTimerInterruptAdr implements Command {
    public static final SetTimerInterruptAdr INS = new SetTimerInterruptAdr();
    public int timerInterruptHandler = 0x0fc00; // default address of interrupt handling routine
    private int setTimerInterruptAdrPos = 0; // determines state for receiving timerInterruptHandler

    @Override
    public void reset(Control control) {
        setTimerInterruptAdrPos = 0;
    }

    @Override
    public void write(byte data, Control control) {
        if (setTimerInterruptAdrPos == 0) {
            timerInterruptHandler = data;
            setTimerInterruptAdrPos = 1;
        } else {
            timerInterruptHandler |= (data << 8);
            setTimerInterruptAdrPos = 0;
            control.clearCommand();
        }
    }

    @Override
    public void start(Control control) {
        reset(control);
        control.clearReadCommand();
    }
}
