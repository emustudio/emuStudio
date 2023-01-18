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
package net.emustudio.plugins.device.adm3a.api;

import net.emustudio.emulib.plugins.device.DeviceContext;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class ContextAdm3ATest {

    private ContextAdm3A context;

    @Before
    public void setUp() {
        this.context = new ContextAdm3A(() -> false);
    }

    @Test
    public void testNoExceptionThrownOnResetWithoutSettingDisplay() {
        context.reset();
    }

    @Test(expected = NullPointerException.class)
    public void testSetNullDisplayThrows() {
        context.setDisplay(null);
    }

    @Test
    public void testResetCallsDisplayReset() {
        Display display = mock(Display.class);
        display.reset();
        expectLastCall().once();
        replay(display);

        context.setDisplay(display);
        context.reset();
        verify(display);
    }

    @Test
    public void testWriteDataCallsDisplayWrite() {
        Display display = mock(Display.class);
        display.write((byte) 0xFF);
        expectLastCall().once();
        replay(display);

        context.setDisplay(display);
        context.writeData((byte) 0xFF);
        verify(display);
    }

    @Test
    public void testGetDataTypeIsByte() {
        assertEquals(Byte.class, context.getDataType());
    }

    @Test(expected = NullPointerException.class)
    public void testSetNullExternalDeviceThrows() {
        context.setExternalDevice(null);
    }

    @Test
    public void testReadDataReturns0() {
        assertEquals((byte) 0, context.readData().byteValue());
    }

    @Test
    public void testOnKeyFromKeyboardSendsDataToDevice() {
        DeviceContext<Byte> device = mock(DeviceContext.class);
        device.writeData((byte) 0xFF);
        replay(device);

        context.setExternalDevice(device);
        context.onKeyFromKeyboard((byte) 0xFF);
        verify(device);
    }

    @Test
    public void testHalfDuplex() {
        Display display = mock(Display.class);
        display.write((byte) 0xFF);
        expectLastCall().once();
        replay(display);

        ContextAdm3A tmpContext = new ContextAdm3A(() -> true);
        tmpContext.setDisplay(display);
        tmpContext.onKeyFromKeyboard((byte) 0xFF);

        verify(display);
    }
}
