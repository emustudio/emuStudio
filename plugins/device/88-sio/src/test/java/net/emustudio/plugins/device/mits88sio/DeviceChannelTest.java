/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
