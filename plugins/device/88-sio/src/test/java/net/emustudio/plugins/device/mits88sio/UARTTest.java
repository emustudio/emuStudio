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

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88sio.settings.SioUnitSettings;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UARTTest {
    private SioUnitSettings settings;
    private Context8080 context;

    @Before
    public void setup() {
        this.settings = mock(SioUnitSettings.class);
        this.context = mock(Context8080.class);
    }

    @Test
    public void testGetDeviceIdReturnsNotNullIfDeviceIsNotAttached() {
        assertNotNull(new UART(context, settings).getDeviceId());
    }

    @Test
    public void testSetNullDeviceDoesNotThrow() {
        new UART(context, settings).setDevice(null);
    }

    @Test
    public void testInitialStatusIs0x02() {
        assertEquals(2, new UART(context, settings).readStatus());
    }

    @Test
    public void testResetOnEmptyBufferSetStatusTo0x02() {
        UART uart = new UART(context, settings);
        uart.reset(false);
        assertEquals(0x02, uart.readStatus());
    }

    @Test
    public void testWriteToStatus0x03OnEmptyBufferSetStatusTo0x02() {
        UART uart = new UART(context, settings);
        uart.setStatus((byte) 0x03);
        assertEquals(0x02, uart.readStatus());
    }

    @Test
    public void testWriteFromDeviceSetsInputDeviceReady() {
        UART uart = new UART(context, settings);
        uart.receiveFromDevice((byte) 5);
        assertEquals(1, uart.readStatus() & 0x01);
    }

    @Test
    public void testReadBufferResetInputDeviceReady() {
        UART uart = new UART(context, settings);
        uart.receiveFromDevice((byte) 5);

        assertEquals(5, uart.readBuffer());
        assertEquals(0, uart.readStatus() & 0x01);
    }

    @Test
    public void testBufferIsFIFO() {
        UART uart = new UART(context, settings);
        uart.receiveFromDevice((byte) 1);
        uart.receiveFromDevice((byte) 2);
        uart.receiveFromDevice((byte) 3);

        assertEquals(1, uart.readBuffer());
        assertEquals(2, uart.readBuffer());
        assertEquals(3, uart.readBuffer());
    }

    @Test
    public void testInputInterruptIsTriggered() {
        SioUnitSettings settings = mock(SioUnitSettings.class);
        expect(settings.getInputInterruptVector()).andReturn(5).once();

        Context8080 context = mock(Context8080.class);
        expect(context.isInterruptSupported()).andReturn(true).once();
        context.signalInterrupt(new byte[]{(byte) 0xEF});
        expectLastCall().once();

        replay(settings, context);
        UART uart = new UART(context, settings);
        uart.setStatus((byte) 1);
        uart.receiveFromDevice((byte) 1);

        verify(context);
    }

    @Test
    public void testOutputInterruptIsTriggered() {
        SioUnitSettings settings = mock(SioUnitSettings.class);
        expect(settings.getOutputInterruptVector()).andReturn(5).once();

        Context8080 context = mock(Context8080.class);
        expect(context.isInterruptSupported()).andReturn(true).once();
        context.signalInterrupt(new byte[]{(byte) 0xEF});
        expectLastCall().once();

        replay(settings, context);
        UART uart = new UART(context, settings);
        uart.setStatus((byte) 2);
        uart.sendToDevice((byte) 1);

        verify(context);
    }

    @Test
    public void testInputInterruptIsTriggeredOnNonemptyBuffer() {
        SioUnitSettings settings = mock(SioUnitSettings.class);
        expect(settings.getInputInterruptVector()).andReturn(5).once();

        Context8080 context = mock(Context8080.class);
        expect(context.isInterruptSupported()).andReturn(true).once();
        context.signalInterrupt(new byte[]{(byte) 0xEF});
        expectLastCall().once();

        replay(settings, context);
        UART uart = new UART(context, settings);
        uart.receiveFromDevice((byte) 1); // interrupts still disabled
        uart.setStatus((byte) 1); // interrupts enabled - here the interrupt happens

        verify(context);
    }
}
