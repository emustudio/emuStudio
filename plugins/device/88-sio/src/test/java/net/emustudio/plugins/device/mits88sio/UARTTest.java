/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
import org.junit.Test;

import static org.easymock.EasyMock.mock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UARTTest {

    @Test
    public void testGetDeviceIdReturnsNotNullIfDeviceIsNotAttached() {
        assertNotNull(new UART(mock(Context8080.class)).getDeviceId());
    }

    @Test
    public void testSetNullDeviceDoesNotThrow() {
        new UART(mock(Context8080.class)).setDevice(null);
    }

    @Test
    public void testInitialStatusIs0x02() {
        assertEquals(2, new UART(mock(Context8080.class)).readStatus());
    }

    @Test
    public void testResetOnEmptyBufferSetStatusTo0x02() {
        UART UART = new UART(mock(Context8080.class));
        UART.reset(false);
        assertEquals(0x02, UART.readStatus());
    }

    @Test
    public void testWriteToStatus0x03OnEmptyBufferSetStatusTo0x02() {
        UART UART = new UART(mock(Context8080.class));
        UART.setStatus((byte) 0x03);
        assertEquals(0x02, UART.readStatus());
    }

    @Test
    public void testWriteFromDeviceSetsInputDeviceReady() {
        UART UART = new UART(mock(Context8080.class));
        UART.receiveFromDevice((byte) 5);
        assertEquals(1, UART.readStatus() & 0x01);
    }

    @Test
    public void testReadBufferResetInputDeviceReady() {
        UART UART = new UART(mock(Context8080.class));
        UART.receiveFromDevice((byte) 5);

        assertEquals(5, UART.readBuffer());
        assertEquals(0, UART.readStatus() & 0x01);
    }

    @Test
    public void testBufferIsFIFO() {
        UART UART = new UART(mock(Context8080.class));
        UART.receiveFromDevice((byte) 1);
        UART.receiveFromDevice((byte) 2);
        UART.receiveFromDevice((byte) 3);

        assertEquals(1, UART.readBuffer());
        assertEquals(2, UART.readBuffer());
        assertEquals(3, UART.readBuffer());
    }
}
