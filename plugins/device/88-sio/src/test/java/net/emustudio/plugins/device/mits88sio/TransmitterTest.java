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
package net.emustudio.plugins.device.mits88sio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TransmitterTest {

    @Test
    public void testGetDeviceIdReturnsNotNullIfDeviceIsNotAttached() {
        assertNotNull(new Transmitter().getDeviceId());
    }

    @Test
    public void testSetNullDeviceDoesNotThrow() {
        new Transmitter().setDevice(null);
    }

    @Test
    public void testInitialStatusIs0x02() {
        assertEquals(2, new Transmitter().readStatus());
    }

    @Test
    public void testResetOnEmptyBufferSetStatusTo0x02() {
        Transmitter transmitter = new Transmitter();
        transmitter.reset(false);
        assertEquals(0x02, transmitter.readStatus());
    }

    @Test
    public void testWriteToStatus0x03OnEmptyBufferSetStatusTo0x02() {
        Transmitter transmitter = new Transmitter();
        transmitter.writeToStatus((short) 0x03);
        assertEquals(0x02, transmitter.readStatus());
    }

    @Test
    public void testWriteFromDeviceSetsInputDeviceReady() {
        Transmitter transmitter = new Transmitter();
        transmitter.writeFromDevice((byte) 5);
        assertEquals(1, transmitter.readStatus() & 0x01);
    }

    @Test
    public void testReadBufferResetInputDeviceReady() {
        Transmitter transmitter = new Transmitter();
        transmitter.writeFromDevice((byte) 5);

        assertEquals(5, transmitter.readBuffer());
        assertEquals(0, transmitter.readStatus() & 0x01);
    }

    @Test
    public void testBufferIsFIFO() {
        Transmitter transmitter = new Transmitter();
        transmitter.writeFromDevice((byte) 1);
        transmitter.writeFromDevice((byte) 2);
        transmitter.writeFromDevice((byte) 3);

        assertEquals(1, transmitter.readBuffer());
        assertEquals(2, transmitter.readBuffer());
        assertEquals(3, transmitter.readBuffer());
    }
}
