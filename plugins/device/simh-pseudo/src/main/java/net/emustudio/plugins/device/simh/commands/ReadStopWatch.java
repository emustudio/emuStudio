/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class ReadStopWatch implements Command {
    public final static ReadStopWatch INS = new ReadStopWatch();
    public long stopWatchNow = 0; // stores starting time of stop watch
    private int getStopWatchDeltaPos = 0; // determines the state for receiving stopWatchDelta
    private long stopWatchDelta = 0; // stores elapsed time of stop watch

    @Override
    public void reset(Control control) {
        getStopWatchDeltaPos = 0;
    }

    @Override
    public byte read(Control control) {
        byte result;
        switch (getStopWatchDeltaPos) {
            case 0:
                result = (byte) (stopWatchDelta & 0xff);
                getStopWatchDeltaPos = 1;
                break;
            case 1:
                result = (byte) ((stopWatchDelta >> 8) & 0xff);
                getStopWatchDeltaPos = 2;
                break;
            case 2:
                result = (byte) ((stopWatchDelta >> 16) & 0xff);
                getStopWatchDeltaPos = 3;
                break;
            case 3:
            default:
                result = (byte) ((stopWatchDelta >> 24) & 0xff);
                getStopWatchDeltaPos = 0;
                control.clearCommand();
                break;
        }
        return result;
    }

    @Override
    public void start(Control control) {
        getStopWatchDeltaPos = 0;
        stopWatchDelta = System.currentTimeMillis() - stopWatchNow;
        control.clearWriteCommand();
    }
}
