/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88sio;

import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class DataChannelTest {

    @Test
    public void testInputBit8Cleared() {
        SioUnitSettings settings = niceMock(SioUnitSettings.class);
        expect(settings.isClearOutputBit8()).andReturn(true).anyTimes();
        expect(settings.getMapBackspaceChar()).andReturn(SioUnitSettings.MAP_CHAR.UNCHANGED).anyTimes();
        expect(settings.getMapDeleteChar()).andReturn(SioUnitSettings.MAP_CHAR.UNCHANGED).anyTimes();
        replay(settings);

        UART uart = mock(UART.class);
        uart.sendToDevice(eq((byte) 0x7F));
        expectLastCall().once();
        replay(uart);

        DataChannel channel = new DataChannel(settings, uart);
        channel.write(0, (byte) 0xFF);

        verify(uart);
    }

    @Test
    public void testOutputBit8Cleared() {
        SioUnitSettings settings = niceMock(SioUnitSettings.class);
        expect(settings.isClearInputBit8()).andReturn(true).anyTimes();
        expect(settings.getMapBackspaceChar()).andReturn(SioUnitSettings.MAP_CHAR.UNCHANGED).anyTimes();
        expect(settings.getMapDeleteChar()).andReturn(SioUnitSettings.MAP_CHAR.UNCHANGED).anyTimes();
        replay(settings);

        UART uart = mock(UART.class);
        expect(uart.readBuffer()).andReturn((byte) 0xFF).once();
        replay(uart);

        DataChannel channel = new DataChannel(settings, uart);

        assertEquals(0x7F, channel.read(0) & 0xFF);
        verify(uart);
    }

    @Test
    public void testInputUpperCase() {
        SioUnitSettings settings = niceMock(SioUnitSettings.class);
        expect(settings.isInputToUpperCase()).andReturn(true).anyTimes();
        expect(settings.getMapBackspaceChar()).andReturn(SioUnitSettings.MAP_CHAR.UNCHANGED).anyTimes();
        expect(settings.getMapDeleteChar()).andReturn(SioUnitSettings.MAP_CHAR.UNCHANGED).anyTimes();
        replay(settings);

        UART uart = mock(UART.class);
        expect(uart.readBuffer()).andReturn((byte) 'a').once();
        replay(uart);

        DataChannel channel = new DataChannel(settings, uart);
        assertEquals('A', channel.read(0) & 0xFF);

        verify(uart);
    }

    @Test
    public void testMapDeleteCharToBackspace() {
        SioUnitSettings settings = niceMock(SioUnitSettings.class);
        expect(settings.getMapBackspaceChar()).andReturn(SioUnitSettings.MAP_CHAR.DELETE).anyTimes();
        expect(settings.getMapDeleteChar()).andReturn(SioUnitSettings.MAP_CHAR.UNCHANGED).anyTimes();
        replay(settings);

        UART uart = mock(UART.class);
        uart.sendToDevice(eq((byte) 0x7F));
        expectLastCall().once();
        replay(uart);

        DataChannel channel = new DataChannel(settings, uart);
        channel.write(0, (byte) '\b');

        verify(uart);
    }

    @Test
    public void testMapBackspaceCharToDelete() {
        SioUnitSettings settings = niceMock(SioUnitSettings.class);
        expect(settings.getMapBackspaceChar()).andReturn(SioUnitSettings.MAP_CHAR.UNCHANGED).anyTimes();
        expect(settings.getMapDeleteChar()).andReturn(SioUnitSettings.MAP_CHAR.BACKSPACE).anyTimes();
        replay(settings);

        UART uart = mock(UART.class);
        uart.sendToDevice(eq((byte) '\b'));
        expectLastCall().once();
        replay(uart);

        DataChannel channel = new DataChannel(settings, uart);
        channel.write(0, (byte) 0x7F);

        verify(uart);
    }
}
