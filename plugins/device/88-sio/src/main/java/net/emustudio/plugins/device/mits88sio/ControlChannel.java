/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
