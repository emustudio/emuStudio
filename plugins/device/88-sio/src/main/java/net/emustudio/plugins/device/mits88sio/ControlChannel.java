/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88sio;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;

import java.util.Objects;

public class ControlChannel implements Context8080.CpuPortDevice {
    private final UART uart;

    public ControlChannel(UART uart) {
        this.uart = Objects.requireNonNull(uart);
    }

    @Override
    public byte read(int portAddress) {
        return uart.getStatus();
    }

    @Override
    public void write(int portAddress, byte data) {
        uart.setStatus(data);
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        return "88-SIO Control Channel";
    }
}
