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

import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class DeviceChannelTest {

    @Test
    public void testReadReturnsZero() {
        assertEquals(0, (byte) new UART.DeviceChannel().readData());
    }

    @Test
    public void testWriteCallsReceiveFromDevice() {
        UART uart = mock(UART.class);
        uart.receiveFromDevice(eq((byte) 10));
        expectLastCall().once();
        replay(uart);

        UART.DeviceChannel channel = new UART.DeviceChannel();
        channel.setUART(uart);
        channel.writeData((byte) 10);

        verify(uart);
    }
}
