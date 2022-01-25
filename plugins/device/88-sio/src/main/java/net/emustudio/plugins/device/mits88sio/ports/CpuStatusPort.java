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
package net.emustudio.plugins.device.mits88sio.ports;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.device.mits88sio.Transmitter;

import java.util.Objects;

/**
 * This is the status port of 88-SIO card.
 */
@PluginContext(id = "Status port")
public class CpuStatusPort implements DeviceContext<Byte> {
    private final Transmitter transmitter;

    public CpuStatusPort(Transmitter transmitter) {
        this.transmitter = Objects.requireNonNull(transmitter);
    }

    @Override
    public Byte readData() {
        return transmitter.readStatus();
    }

    @Override
    public void writeData(Byte data) {
        transmitter.writeToStatus(data);
    }

    @Override
    public Class<Byte> getDataType() {
        return Byte.class;
    }

    @Override
    public String toString() {
        return "88-SIO Status Port";
    }
}
