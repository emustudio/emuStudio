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

    @Test
    public void testWriteWithNullUartDoesNotThrow() {
        UART.DeviceChannel channel = new UART.DeviceChannel();
        channel.writeData((byte) 10); // should not throw
    }

    @Test
    public void testGetDataTypeReturnsByteClass() {
        assertEquals(Byte.class, new UART.DeviceChannel().getDataType());
    }

    @Test
    public void testReadAlwaysReturnsZeroEvenAfterWrite() {
        UART uart = niceMock(UART.class);
        replay(uart);

        UART.DeviceChannel channel = new UART.DeviceChannel();
        channel.setUART(uart);
        channel.writeData((byte) 42);
        assertEquals(0, (byte) channel.readData());
    }

    @Test
    public void testSetUartToNullAfterSetting() {
        UART uart = mock(UART.class);
        uart.receiveFromDevice(eq((byte) 10));
        expectLastCall().once();
        replay(uart);

        UART.DeviceChannel channel = new UART.DeviceChannel();
        channel.setUART(uart);
        channel.writeData((byte) 10);

        // set to null - should not throw on further writes
        channel.setUART(null);
        channel.writeData((byte) 20);

        verify(uart);
    }
}
