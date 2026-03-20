/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
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
    public void testReadReturnsStatusRegardlessOfPortAddress() {
        UART uart = mock(UART.class);
        expect(uart.getStatus()).andReturn((byte) 0x23).once();
        replay(uart);

        ControlChannel channel = new ControlChannel(uart);
        assertEquals(0x23, channel.read(0xFF) & 0xFF);
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

    @Test
    public void testWriteSetsStatusRegardlessOfPortAddress() {
        UART uart = mock(UART.class);
        uart.setStatus(eq((byte) 0x55));
        expectLastCall().once();
        replay(uart);

        ControlChannel channel = new ControlChannel(uart);
        channel.write(0x10, (byte) 0x55);

        verify(uart);
    }

    @Test
    public void testGetNameReturnsToString() {
        UART uart = niceMock(UART.class);
        replay(uart);

        ControlChannel channel = new ControlChannel(uart);
        assertEquals(channel.toString(), channel.getName());
    }

    @Test
    public void testToStringContainsSIO() {
        UART uart = niceMock(UART.class);
        replay(uart);

        ControlChannel channel = new ControlChannel(uart);
        assertEquals("88-SIO Control Channel", channel.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testNullUartThrows() {
        new ControlChannel(null);
    }
}
