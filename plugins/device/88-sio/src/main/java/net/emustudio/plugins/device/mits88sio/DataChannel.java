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

import net.emustudio.emulib.plugins.device.DeviceContext;

import java.util.Objects;

public class DataChannel implements DeviceContext<Byte> {
    private final static byte DELETE_CHAR = 0x7F;
    private final static byte BACKSPACE_CHAR = 0x08;

    private final SioUnitSettings settings;
    private final UART uart;

    public DataChannel(SioUnitSettings settings, UART uart) {
        this.settings = Objects.requireNonNull(settings);
        this.uart = Objects.requireNonNull(uart);
    }

    @Override
    public Byte readData() {
        byte data = uart.readBuffer();
        data = settings.isInputToUpperCase() ? (byte) Character.toUpperCase((char) (data & 0xFF)) : data;
        data = settings.isClearInputBit8() ? (byte) (data & 0x7F) : data;
        return mapCharacter(data);
    }

    @Override
    public void writeData(Byte data) {
        data = settings.isClearOutputBit8() ? (byte) (data & 0x7F) : data;
        uart.sendToDevice(mapCharacter(data));
    }

    @Override
    public Class<Byte> getDataType() {
        return Byte.class;
    }

    private byte mapCharacter(byte data) {
        if (data == DELETE_CHAR) {
            data = settings.getMapDeleteChar() == SioUnitSettings.MAP_CHAR.BACKSPACE ? BACKSPACE_CHAR : DELETE_CHAR;
        }
        if (data == BACKSPACE_CHAR) {
            data = settings.getMapBackspaceChar() == SioUnitSettings.MAP_CHAR.DELETE ? DELETE_CHAR : BACKSPACE_CHAR;
        }
        return data;
    }
}
