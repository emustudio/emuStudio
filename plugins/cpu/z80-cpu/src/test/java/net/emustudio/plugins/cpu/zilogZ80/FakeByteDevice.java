/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;

public class FakeByteDevice implements Context8080.CpuPortDevice {
    private byte value;

    public byte getValue() {
        return value;
    }

    public void setValue(byte value) {
        this.value = value;
    }

    @Override
    public byte read(int portAddress) {
        return (byte) (value & 0xFF);
    }

    @Override
    public void write(int portAddress, byte value) {
        this.value = value;
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        return "FakeByteDevice";
    }
}
