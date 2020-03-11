/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.cpu.zilogZ80.impl;

import net.emustudio.emulib.plugins.device.DeviceContext;

public class FakeDevice implements DeviceContext<Short> {
    private byte value;

    public void setValue(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    @Override
    public Short readData() {
        return (short) (value & 0xFF);
    }

    @Override
    public void writeData(Short value) {
        this.value = value.byteValue();
    }

    @Override
    public Class<Short> getDataType() {
        return Short.class;
    }
}
