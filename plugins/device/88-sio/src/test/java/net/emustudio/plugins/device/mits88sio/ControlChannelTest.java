/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
        assertEquals(2, channel.read(0) & 0xFF);
    }

    @Test
    public void testWriteSetsStatus() {
        UART uart = mock(UART.class);
        uart.setStatus(eq((byte) 2));
        expectLastCall().once();
        replay(uart);

        ControlChannel channel = new ControlChannel(uart);
        channel.write(0, (byte) 2);

        verify(uart);
    }
}
