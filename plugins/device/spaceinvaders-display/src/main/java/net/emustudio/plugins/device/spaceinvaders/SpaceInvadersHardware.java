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
    static final int SOUND_1_PORT = 3;
    static final int SOUND_2_PORT = 5;

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
    private final SoundOutput sound;
    private int sound1;
    private int sound2;

    SpaceInvadersHardware() {
        this(SoundOutput.SILENT);
    }

    SpaceInvadersHardware(SoundOutput sound) {
        this.sound = sound;
    }

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
            case SOUND_1_PORT:
                writeSound1(value & 0xFF);
                break;
            case SOUND_2_PORT:
                int rising = (value & 0xFF) & ~sound2;
                sound2 = value & 0xFF;
                if ((sound1 & 0x20) != 0) {
                    trigger(rising, 0, SoundOutput.Sample.FLEET_1);
                    trigger(rising, 1, SoundOutput.Sample.FLEET_2);
                    trigger(rising, 2, SoundOutput.Sample.FLEET_3);
                    trigger(rising, 3, SoundOutput.Sample.FLEET_4);
                    trigger(rising, 4, SoundOutput.Sample.UFO_HIT);
                }
                break;
            default:
                break;
        }
    }

    private void writeSound1(int value) {
        int rising = value & ~sound1;
        boolean wasUfoOn = (sound1 & 0x21) == 0x21;
        boolean ufoOn = (value & 0x21) == 0x21;
        if ((value & 0x20) == 0) {
            if ((sound1 & 0x20) != 0) {
                sound.stopAll();
            }
        } else {
            if (ufoOn && !wasUfoOn) {
                sound.play(SoundOutput.Sample.UFO, true);
            } else if (!ufoOn && wasUfoOn) {
                sound.stop(SoundOutput.Sample.UFO);
            }
            trigger(rising, 1, SoundOutput.Sample.SHOT);
            trigger(rising, 2, SoundOutput.Sample.PLAYER_HIT);
            trigger(rising, 3, SoundOutput.Sample.INVADER_HIT);
            trigger(rising, 4, SoundOutput.Sample.BONUS);
        }
        sound1 = value;
    }

    private void trigger(int rising, int bit, SoundOutput.Sample sample) {
        if ((rising & (1 << bit)) != 0) {
            sound.play(sample, false);
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
            sound1 = 0;
            sound2 = 0;
            sound.stopAll();
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
