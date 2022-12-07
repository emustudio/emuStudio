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
        channel.writeData((byte) 0xFF);

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

        assertEquals(0x7F, channel.readData() & 0xFF);
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
        assertEquals('A', channel.readData() & 0xFF);

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
        channel.writeData((byte) '\b');

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
        channel.writeData((byte) 0x7F);

        verify(uart);
    }
}
