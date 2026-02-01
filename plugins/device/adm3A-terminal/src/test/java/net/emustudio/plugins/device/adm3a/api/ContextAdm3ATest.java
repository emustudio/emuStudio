/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
