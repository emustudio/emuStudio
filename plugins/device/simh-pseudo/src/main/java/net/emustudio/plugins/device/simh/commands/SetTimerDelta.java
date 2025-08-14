/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class SetTimerDelta implements Command {
    public final static SetTimerDelta INS = new SetTimerDelta();

    private final static int DEFAULT_TIMER_DELTA = 100; // default value for timer delta in ms
    public int timerDelta = DEFAULT_TIMER_DELTA;  // interrupt every 100 ms
    private int setTimerDeltaPos = 0; // determines state for receiving timerDelta

    @Override
    public void reset(Control control) {
        setTimerDeltaPos = 0;
    }

    @Override
    public void write(byte data, Control control) {
        if (setTimerDeltaPos == 0) {
            timerDelta = data & 0xFF;
            setTimerDeltaPos = 1;
        } else {
            timerDelta = (timerDelta | (data << 8)) & 0xFFFF;
            setTimerDeltaPos = 0;
            control.clearCommand();
            if (timerDelta == 0) {
                timerDelta = DEFAULT_TIMER_DELTA;
                System.out.println("SIMH: Timer delta set to 0 ms ignored. Using " + DEFAULT_TIMER_DELTA + " ms instead.");
            }
        }
    }

    @Override
    public void start(Control control) {
        reset(control);
        control.clearReadCommand();
    }
}
