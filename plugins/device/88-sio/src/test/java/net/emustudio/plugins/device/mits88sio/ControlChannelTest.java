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

import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class ControlChannelTest {

    @Test
    public void testReadReturnsStatus() {
        UART uart = mock(UART.class);
        expect(uart.getStatus()).andReturn((byte) 2).once();
        replay(uart);

        ControlChannel channel = new ControlChannel(uart);
        assertEquals(2, channel.readData() & 0xFF);
    }

    @Test
    public void testWriteSetsStatus() {
        UART uart = mock(UART.class);
        uart.setStatus(eq((byte) 2));
        expectLastCall().once();
        replay(uart);

        ControlChannel channel = new ControlChannel(uart);
        channel.writeData((byte) 2);

        verify(uart);
    }
}
