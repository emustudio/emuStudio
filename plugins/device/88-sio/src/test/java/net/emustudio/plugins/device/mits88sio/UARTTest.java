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

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import org.junit.Before;
import org.junit.Ignore;
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
        expect(settings.getInterruptsSupported()).andReturn(true).anyTimes();
        expect(settings.getInputInterruptVector()).andReturn(7).anyTimes();
        expect(settings.getOutputInterruptVector()).andReturn(7).anyTimes();
        settings.addObserver(anyObject());
        expectLastCall().once();
        this.context = mock(Context8080.class);
        replay(settings, context);
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
        assertEquals(2, new UART(context, settings).getStatus());
    }

    @Test
    public void testResetOnEmptyBufferSetStatusTo0x02() {
        UART uart = new UART(context, settings);
        uart.reset(false);
        assertEquals(0x02, uart.getStatus());
    }

    @Test
    public void testWriteToStatus0x03OnEmptyBufferSetStatusTo0x02() {
        UART uart = new UART(context, settings);
        uart.setStatus((byte) 0x03);
        assertEquals(0x02, uart.getStatus());
    }

    @Test
    public void testWriteFromDeviceSetsInputDeviceReady() {
        UART uart = new UART(context, settings);
        uart.receiveFromDevice((byte) 5);
        assertEquals(1, uart.getStatus() & 0x01);
    }

    @Test
    public void testReadBufferResetInputDeviceReady() {
        UART uart = new UART(context, settings);
        uart.receiveFromDevice((byte) 5);

        assertEquals(5, uart.readBuffer());
        assertEquals(0, uart.getStatus() & 0x01);
    }

    @Test
    public void testInputInterruptIsTriggered() {
        Context8080 context = mock(Context8080.class);
        expect(context.isInterruptSupported()).andReturn(true).once();
        context.signalInterrupt(new byte[]{(byte) 0xFF});
        expectLastCall().once();

        replay(context);
        UART uart = new UART(context, settings);
        uart.setStatus((byte) 1);
        uart.receiveFromDevice((byte) 1);

        verify(context);
    }

    @Test
    public void testOutputInterruptIsTriggered() {
        Context8080 context = mock(Context8080.class);
        expect(context.isInterruptSupported()).andReturn(true).once();
        context.signalInterrupt(new byte[]{(byte) 0xFF});
        expectLastCall().once();

        DeviceContext<Byte> device = mock(DeviceContext.class);
        device.writeData((byte) 1);
        expectLastCall().once();

        replay(context, device);
        UART uart = new UART(context, settings);
        uart.setDevice(device);
        uart.setStatus((byte) 2);
        uart.sendToDevice((byte) 1);

        verify(context);
    }
}
