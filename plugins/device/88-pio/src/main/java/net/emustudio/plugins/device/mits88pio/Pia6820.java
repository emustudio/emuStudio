/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import java.util.Arrays;

/** Motorola 6820 register and handshake behavior; one CPU access is one E pulse. */
public final class Pia6820 {
    public interface Signals {
        void outputChanged(int side, int data, int mask);
        void controlOutputChanged(int side, boolean high);
        void interruptChanged();
    }

    private final Signals signals;
    private final int[] control = new int[2];
    private final int[] direction = new int[2];
    private final int[] output = new int[2];
    private final int[] input = {255, 255};
    private final boolean[] c1 = {true, true};
    private final boolean[] c2 = {true, true};
    private final boolean[] c2Output = {true, true};
    private final boolean[] irq1 = new boolean[2];
    private final boolean[] irq2 = new boolean[2];

    public Pia6820(Signals signals) { this.signals = signals; }

    public int readControl(int side) {
        return control[side] | (irq1[side] ? 0x80 : 0) | (irq2[side] && (control[side] & 0x20) == 0 ? 0x40 : 0);
    }

    public void writeControl(int side, int value) {
        control[side] = value & 0x3F;
        if ((control[side] & 0x20) != 0) {
            setOutputLine(side, c2Mode(side) != 6);
        }
        signals.interruptChanged();
    }

    public int readData(int side) {
        if ((control[side] & 4) == 0) { return direction[side]; }
        int result = (output[side] & direction[side]) | (input[side] & ~direction[side] & 255);
        irq1[side] = false;
        irq2[side] = false;
        signals.interruptChanged();
        if (side == 0) { strobe(side); }
        return result;
    }

    public void writeData(int side, int value) {
        if ((control[side] & 4) == 0) {
            direction[side] = value & 255;
        } else {
            output[side] = value & 255;
        }
        signals.outputChanged(side, getOutput(side), direction[side]);
        if (side == 1 && (control[side] & 4) != 0) { strobe(side); }
    }

    public void setInput(int side, int value) { input[side] = value & 255; }
    public int getOutput(int side) { return output[side] & direction[side]; }
    public int getDirection(int side) { return direction[side]; }
    public int getInput(int side) { return input[side]; }

    public void setC1(int side, boolean high) {
        if (c1[side] != high && high == ((control[side] & 2) != 0)) {
            irq1[side] = true;
            if (c2Mode(side) == 4) { setOutputLine(side, true); }
        }
        c1[side] = high;
        signals.interruptChanged();
    }

    public void setC2(int side, boolean high) {
        if ((control[side] & 0x20) == 0 && c2[side] != high && high == ((control[side] & 0x10) != 0)) {
            irq2[side] = true;
        }
        c2[side] = high;
        signals.interruptChanged();
    }

    public boolean isInterruptPending() {
        for (int side = 0; side < 2; side++) {
            if ((irq1[side] && (control[side] & 1) != 0)
                    || (irq2[side] && (control[side] & 0x20) == 0 && (control[side] & 8) != 0)) {
                return true;
            }
        }
        return false;
    }

    public void reset() {
        Arrays.fill(control, 0);
        Arrays.fill(direction, 0);
        Arrays.fill(output, 0);
        Arrays.fill(input, 255);
        Arrays.fill(c1, true);
        Arrays.fill(c2, true);
        Arrays.fill(c2Output, true);
        Arrays.fill(irq1, false);
        Arrays.fill(irq2, false);
        signals.interruptChanged();
    }

    private int c2Mode(int side) { return (control[side] >>> 3) & 7; }

    private void strobe(int side) {
        int mode = c2Mode(side);
        if (mode == 4 || mode == 5) {
            setOutputLine(side, false);
            if (mode == 5) { setOutputLine(side, true); }
        }
    }

    private void setOutputLine(int side, boolean high) {
        if (c2Output[side] != high) {
            c2Output[side] = high;
            signals.controlOutputChanged(side, high);
        }
    }
}
