/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88sio;

import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class DataChannelTest {

    private SioUnitSettings defaultSettings() {
        SioUnitSettings settings = niceMock(SioUnitSettings.class);
        expect(settings.isClearInputBit8()).andReturn(false).anyTimes();
        expect(settings.isClearOutputBit8()).andReturn(false).anyTimes();
        expect(settings.isInputToUpperCase()).andReturn(false).anyTimes();
        expect(settings.getMapBackspaceChar()).andReturn(SioUnitSettings.MAP_CHAR.UNCHANGED).anyTimes();
        expect(settings.getMapDeleteChar()).andReturn(SioUnitSettings.MAP_CHAR.UNCHANGED).anyTimes();
        return settings;
    }

    @Test
    public void testWriteClearsOutputBit8() {
        SioUnitSettings settings = defaultSettings();
        reset(settings);
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
    public void testReadClearsInputBit8() {
        SioUnitSettings settings = defaultSettings();
        reset(settings);
        expect(settings.isClearInputBit8()).andReturn(true).anyTimes();
        expect(settings.isInputToUpperCase()).andReturn(false).anyTimes();
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
    public void testReadInputToUpperCase() {
        SioUnitSettings settings = defaultSettings();
        reset(settings);
        expect(settings.isInputToUpperCase()).andReturn(true).anyTimes();
        expect(settings.isClearInputBit8()).andReturn(false).anyTimes();
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
    public void testWriteMapBackspaceToDelete() {
        SioUnitSettings settings = defaultSettings();
        reset(settings);
        expect(settings.isClearOutputBit8()).andReturn(false).anyTimes();
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
    public void testWriteMapDeleteToBackspace() {
        SioUnitSettings settings = defaultSettings();
        reset(settings);
        expect(settings.isClearOutputBit8()).andReturn(false).anyTimes();
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

    @Test
    public void testReadMapDeleteToBackspace() {
        SioUnitSettings settings = defaultSettings();
        reset(settings);
        expect(settings.isClearInputBit8()).andReturn(false).anyTimes();
        expect(settings.isInputToUpperCase()).andReturn(false).anyTimes();
        expect(settings.getMapDeleteChar()).andReturn(SioUnitSettings.MAP_CHAR.BACKSPACE).anyTimes();
        expect(settings.getMapBackspaceChar()).andReturn(SioUnitSettings.MAP_CHAR.UNCHANGED).anyTimes();
        replay(settings);

        UART uart = mock(UART.class);
        expect(uart.readBuffer()).andReturn((byte) 0x7F).once();
        replay(uart);

        DataChannel channel = new DataChannel(settings, uart);
        assertEquals('\b', channel.read(0) & 0xFF);

        verify(uart);
    }

    @Test
    public void testReadMapBackspaceToDelete() {
        SioUnitSettings settings = defaultSettings();
        reset(settings);
        expect(settings.isClearInputBit8()).andReturn(false).anyTimes();
        expect(settings.isInputToUpperCase()).andReturn(false).anyTimes();
        expect(settings.getMapDeleteChar()).andReturn(SioUnitSettings.MAP_CHAR.UNCHANGED).anyTimes();
        expect(settings.getMapBackspaceChar()).andReturn(SioUnitSettings.MAP_CHAR.DELETE).anyTimes();
        replay(settings);

        UART uart = mock(UART.class);
        expect(uart.readBuffer()).andReturn((byte) '\b').once();
        replay(uart);

        DataChannel channel = new DataChannel(settings, uart);
        assertEquals(0x7F, channel.read(0) & 0xFF);

        verify(uart);
    }

    @Test
    public void testReadPassthroughWhenNoTransformations() {
        SioUnitSettings settings = defaultSettings();
        replay(settings);

        UART uart = mock(UART.class);
        expect(uart.readBuffer()).andReturn((byte) 0x42).once();
        replay(uart);

        DataChannel channel = new DataChannel(settings, uart);
        assertEquals(0x42, channel.read(0) & 0xFF);

        verify(uart);
    }

    @Test
    public void testWritePassthroughWhenNoTransformations() {
        SioUnitSettings settings = defaultSettings();
        replay(settings);

        UART uart = mock(UART.class);
        uart.sendToDevice(eq((byte) 0x42));
        expectLastCall().once();
        replay(uart);

        DataChannel channel = new DataChannel(settings, uart);
        channel.write(0, (byte) 0x42);

        verify(uart);
    }

    @Test
    public void testReadIgnoresPortAddress() {
        SioUnitSettings settings = defaultSettings();
        replay(settings);

        UART uart = mock(UART.class);
        expect(uart.readBuffer()).andReturn((byte) 0x42).once();
        replay(uart);

        DataChannel channel = new DataChannel(settings, uart);
        assertEquals(0x42, channel.read(0xFF) & 0xFF);
    }

    @Test
    public void testWriteIgnoresPortAddress() {
        SioUnitSettings settings = defaultSettings();
        replay(settings);

        UART uart = mock(UART.class);
        uart.sendToDevice(eq((byte) 0x42));
        expectLastCall().once();
        replay(uart);

        DataChannel channel = new DataChannel(settings, uart);
        channel.write(0xFF, (byte) 0x42);

        verify(uart);
    }

    @Test
    public void testToStringContainsSIO() {
        SioUnitSettings settings = defaultSettings();
        replay(settings);

        UART uart = niceMock(UART.class);
        replay(uart);

        DataChannel channel = new DataChannel(settings, uart);
        assertEquals("88-SIO Data Channel", channel.toString());
    }

    @Test
    public void testGetNameReturnsToString() {
        SioUnitSettings settings = defaultSettings();
        replay(settings);

        UART uart = niceMock(UART.class);
        replay(uart);

        DataChannel channel = new DataChannel(settings, uart);
        assertEquals(channel.toString(), channel.getName());
    }

    @Test(expected = NullPointerException.class)
    public void testNullSettingsThrows() {
        UART uart = niceMock(UART.class);
        replay(uart);
        new DataChannel(null, uart);
    }

    @Test(expected = NullPointerException.class)
    public void testNullUartThrows() {
        SioUnitSettings settings = defaultSettings();
        replay(settings);
        new DataChannel(settings, null);
    }

    @Test
    public void testReadWithBothClearBit8AndUpperCase() {
        SioUnitSettings settings = defaultSettings();
        reset(settings);
        expect(settings.isInputToUpperCase()).andReturn(true).anyTimes();
        expect(settings.isClearInputBit8()).andReturn(true).anyTimes();
        expect(settings.getMapDeleteChar()).andReturn(SioUnitSettings.MAP_CHAR.UNCHANGED).anyTimes();
        expect(settings.getMapBackspaceChar()).andReturn(SioUnitSettings.MAP_CHAR.UNCHANGED).anyTimes();
        replay(settings);

        UART uart = mock(UART.class);
        // 'a' = 0x61, with bit8 set = 0xE1, toUpperCase on 0xE1 (after clearing bit8 -> 0x61 -> 'A')
        expect(uart.readBuffer()).andReturn((byte) 0xE1).once();
        replay(uart);

        DataChannel channel = new DataChannel(settings, uart);
        // First toUpperCase is applied, then clearBit8
        // toUpperCase((char)(0xE1 & 0xFF)) = toUpperCase(0xE1) - this is 'á' -> 'Á' = 0xC1
        // then clearBit8: 0xC1 & 0x7F = 0x41 = 'A'
        assertEquals('A', channel.read(0) & 0xFF);
    }
}
