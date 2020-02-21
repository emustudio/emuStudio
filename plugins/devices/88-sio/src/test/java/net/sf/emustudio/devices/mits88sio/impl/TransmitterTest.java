/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.devices.mits88sio.impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TransmitterTest {

    @Test
    public void testGetDeviceIdReturnsNotNullIfDeviceIsNotAttached() throws Exception {
        assertNotNull(new Transmitter().getDeviceId());
    }

    @Test
    public void testSetNullDeviceDoesNotThrow() throws Exception {
        new Transmitter().setDevice(null);
    }

    @Test
    public void testInitialStatusIs0x02() throws Exception {
        assertEquals(2, new Transmitter().readStatus());
    }

    @Test
    public void testResetOnEmptyBufferSetStatusTo0x02() throws Exception {
        Transmitter transmitter = new Transmitter();
        transmitter.reset();
        assertEquals(0x02, transmitter.readStatus());
    }

    @Test
    public void testWriteToStatus0x03OnEmptyBufferSetStatusTo0x02() throws Exception {
        Transmitter transmitter = new Transmitter();
        transmitter.writeToStatus((short) 0x03);
        assertEquals(0x02, transmitter.readStatus());
    }

    @Test
    public void testWriteFromDeviceSetsInputDeviceReady() throws Exception {
        Transmitter transmitter = new Transmitter();
        transmitter.writeFromDevice((short) 5);
        assertEquals(1, transmitter.readStatus() & 0x01);
    }

    @Test
    public void testReadBufferResetInputDeviceReady() throws Exception {
        Transmitter transmitter = new Transmitter();
        transmitter.writeFromDevice((short) 5);

        assertEquals(5, transmitter.readBuffer());
        assertEquals(0, transmitter.readStatus() & 0x01);
    }

    @Test
    public void testBufferIsFIFO() throws Exception {
        Transmitter transmitter = new Transmitter();
        transmitter.writeFromDevice((short) 1);
        transmitter.writeFromDevice((short) 2);
        transmitter.writeFromDevice((short) 3);

        assertEquals(1, transmitter.readBuffer());
        assertEquals(2, transmitter.readBuffer());
        assertEquals(3, transmitter.readBuffer());
    }
}
