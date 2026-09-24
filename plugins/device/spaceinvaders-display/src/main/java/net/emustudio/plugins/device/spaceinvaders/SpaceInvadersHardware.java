/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;

import java.util.concurrent.atomic.AtomicInteger;

final class SpaceInvadersHardware implements Context8080.CpuPortDevice {
    static final int INPUT_1_PORT = 1;
    static final int INPUT_2_PORT = 2;
    static final int SHIFT_RESULT_PORT = 3;
    static final int SHIFT_DATA_PORT = 4;

    static final int COIN = 0x01;
    static final int START_2 = 0x02;
    static final int START_1 = 0x04;
    static final int ALWAYS_ON = 0x08;
    static final int FIRE = 0x10;
    static final int LEFT = 0x20;
    static final int RIGHT = 0x40;
    static final int TILT = 0x04;

    private final AtomicInteger input1 = new AtomicInteger(ALWAYS_ON);
    private final AtomicInteger input2 = new AtomicInteger();
    private int shiftRegister;
    private int shiftAmount;

    @Override
    public synchronized byte read(int portAddress) {
        switch (portAddress & 0xFF) {
            case INPUT_1_PORT:
                return (byte) input1.get();
            case INPUT_2_PORT:
                return (byte) input2.get();
            case SHIFT_RESULT_PORT:
                return (byte) ((shiftRegister >>> (8 - shiftAmount)) & 0xFF);
            default:
                return 0;
        }
    }

    @Override
    public synchronized void write(int portAddress, byte value) {
        switch (portAddress & 0xFF) {
            case INPUT_2_PORT:
                shiftAmount = value & 0x07;
                break;
            case SHIFT_DATA_PORT:
                shiftRegister = ((value & 0xFF) << 8) | ((shiftRegister >>> 8) & 0xFF);
                break;
            default:
                break;
        }
    }

    void setInput1(int mask, boolean pressed) {
        update(input1, mask, pressed);
    }

    void setInput2(int mask, boolean pressed) {
        update(input2, mask, pressed);
    }

    void reset() {
        input1.set(ALWAYS_ON);
        input2.set(0);
        synchronized (this) {
            shiftRegister = 0;
            shiftAmount = 0;
        }
    }

    private static void update(AtomicInteger input, int mask, boolean pressed) {
        input.updateAndGet(value -> pressed ? value | mask : value & ~mask);
    }

    @Override
    public String getName() {
        return "Space Invaders I/O";
    }
}
