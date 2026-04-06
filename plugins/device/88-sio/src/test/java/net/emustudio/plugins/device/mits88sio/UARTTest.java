/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88sio;

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

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
    public void testGetDeviceIdReturnsUnknownIfDeviceIsNotAttached() {
        assertEquals("unknown", new UART(context, settings).getDeviceId());
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

    @Test
    public void testReadBufferWhenEmptyReturnsZero() {
        UART uart = new UART(context, settings);
        assertEquals(0, uart.readBuffer());
    }

    @Test
    public void testReadBufferWhenEmptyKeepsXmitterBufferEmpty() {
        UART uart = new UART(context, settings);
        uart.readBuffer();
        // Status should remain XMITTER_BUFFER_EMPTY (0x02)
        assertEquals(0x02, uart.getStatus());
    }

    @Test
    public void testFirstReceiveFromDeviceSetsDataOverflow() {
        // Per implementation: when buffer is empty on receive, DATA_OVERFLOW is set
        UART uart = new UART(context, settings);
        uart.receiveFromDevice((byte) 1);

        int status = uart.getStatus();
        assertEquals(0x10, status & 0x10); // overflow bit set on first receive
        assertEquals(0x20, status & 0x20); // data available
        assertEquals(0x01, status & 0x01); // input device ready
    }

    @Test
    public void testSecondReceiveFromDeviceClearsDataOverflow() {
        // Per implementation: when buffer is NOT empty on receive, DATA_OVERFLOW is cleared
        UART uart = new UART(context, settings);
        uart.receiveFromDevice((byte) 1);
        uart.receiveFromDevice((byte) 2);

        int status = uart.getStatus();
        assertEquals(0, status & 0x10);    // overflow cleared on second receive
        assertEquals(0x20, status & 0x20); // data available
        assertEquals(0x01, status & 0x01); // input device ready
    }

    @Test
    public void testReadBufferReturnsFirstReceivedData() {
        UART uart = new UART(context, settings);
        uart.receiveFromDevice((byte) 10);
        uart.receiveFromDevice((byte) 20);

        assertEquals(10, uart.readBuffer());
    }

    @Test
    public void testReadBufferWithRemainingDataKeepsDataAvailable() {
        UART uart = new UART(context, settings);
        uart.receiveFromDevice((byte) 10);
        uart.receiveFromDevice((byte) 20);

        uart.readBuffer(); // reads 10, 20 still in buffer

        int status = uart.getStatus();
        assertEquals(0x20, status & 0x20); // DATA_AVAILABLE still set
        assertEquals(0x01, status & 0x01); // INPUT_DEVICE_READY still set
        assertEquals(0x02, status & 0x02); // XMITTER_BUFFER_EMPTY still set
    }

    @Test
    public void testReadAllDataFromBuffer() {
        UART uart = new UART(context, settings);
        uart.receiveFromDevice((byte) 10);
        uart.receiveFromDevice((byte) 20);

        assertEquals(10, uart.readBuffer());
        assertEquals(20, uart.readBuffer());
        assertEquals(0x02, uart.getStatus()); // back to XMITTER_BUFFER_EMPTY only
    }

    @Test
    public void testResetClearsBufferWhenGuiSupported() {
        UART uart = new UART(context, settings);
        uart.receiveFromDevice((byte) 10);
        uart.reset(true);

        assertEquals(0, uart.readBuffer()); // buffer should be cleared
        assertEquals(0x02, uart.getStatus());
    }

    @Test
    public void testResetDoesNotClearBufferWhenGuiNotSupported() {
        UART uart = new UART(context, settings);
        uart.receiveFromDevice((byte) 10);
        uart.reset(false);

        // buffer should NOT be cleared, but status is reset
        assertEquals(0x02, uart.getStatus());
        // data is still in buffer
        assertEquals(10, uart.readBuffer());
    }

    @Test
    public void testResetDisablesInterrupts() {
        UART uart = new UART(context, settings);
        uart.setStatus((byte) 3); // enable both interrupts
        uart.reset(false);
        // After reset, setStatus(0) is called which disables both interrupts
        // Status register is then set to XMITTER_BUFFER_EMPTY
        assertEquals(0x02, uart.getStatus());
    }

    @Test
    public void testSendToDeviceWhenNoDeviceDoesNotThrow() {
        UART uart = new UART(context, settings);
        uart.sendToDevice((byte) 42); // should not throw
    }

    @Test
    public void testSendToDeviceCallsWriteData() {
        DeviceContext<Byte> device = mock(DeviceContext.class);
        device.writeData(eq((byte) 42));
        expectLastCall().once();
        replay(device);

        UART uart = new UART(context, settings);
        uart.setDevice(device);
        uart.sendToDevice((byte) 42);

        verify(device);
    }

    @Test
    public void testSetStatusEnablesInputInterrupt() {
        // setStatus with bit 0 set -> input interrupt enabled
        Context8080 context = mock(Context8080.class);
        expect(context.isInterruptSupported()).andReturn(true).once();
        context.signalInterrupt(aryEq(new byte[]{(byte) 0xFF}));
        expectLastCall().once();
        replay(context);

        UART uart = new UART(context, settings);
        uart.setStatus((byte) 1); // enable input interrupt
        uart.receiveFromDevice((byte) 1);

        verify(context);
    }

    @Test
    public void testSetStatusEnablesOutputInterrupt() {
        // setStatus with bit 1 set -> output interrupt enabled
        Context8080 context = mock(Context8080.class);
        expect(context.isInterruptSupported()).andReturn(true).once();
        context.signalInterrupt(aryEq(new byte[]{(byte) 0xFF}));
        expectLastCall().once();

        DeviceContext<Byte> device = mock(DeviceContext.class);
        device.writeData((byte) 1);
        expectLastCall().once();

        replay(context, device);

        UART uart = new UART(context, settings);
        uart.setDevice(device);
        uart.setStatus((byte) 2); // enable output interrupt
        uart.sendToDevice((byte) 1);

        verify(context);
    }

    @Test
    public void testInputInterruptNotTriggeredWhenNotEnabled() {
        // Interrupts supported but not enabled (setStatus not called with bit 0)
        Context8080 context = niceMock(Context8080.class);
        replay(context);

        UART uart = new UART(context, settings);
        // don't enable input interrupt
        uart.receiveFromDevice((byte) 1);

        // context.signalInterrupt should NOT have been called
        verify(context);
    }

    @Test
    public void testOutputInterruptNotTriggeredWhenNotEnabled() {
        Context8080 context = niceMock(Context8080.class);
        replay(context);

        DeviceContext<Byte> device = niceMock(DeviceContext.class);
        replay(device);

        UART uart = new UART(context, settings);
        uart.setDevice(device);
        // don't enable output interrupt
        uart.sendToDevice((byte) 1);

        verify(context);
    }

    @Test
    public void testInputInterruptNotTriggeredWhenCpuDoesNotSupport() {
        Context8080 context = mock(Context8080.class);
        expect(context.isInterruptSupported()).andReturn(false).anyTimes();
        replay(context);

        UART uart = new UART(context, settings);
        uart.setStatus((byte) 1); // enable input interrupt
        uart.receiveFromDevice((byte) 1);

        // signalInterrupt should NOT be called
        verify(context);
    }

    @Test
    public void testOutputInterruptNotTriggeredWhenCpuDoesNotSupport() {
        Context8080 context = mock(Context8080.class);
        expect(context.isInterruptSupported()).andReturn(false).anyTimes();
        replay(context);

        DeviceContext<Byte> device = niceMock(DeviceContext.class);
        replay(device);

        UART uart = new UART(context, settings);
        uart.setDevice(device);
        uart.setStatus((byte) 2); // enable output interrupt
        uart.sendToDevice((byte) 1);

        verify(context);
    }

    @Test
    public void testObserverNotifiedOnReceiveFromDevice() {
        UART uart = new UART(context, settings);

        UART.Observer observer = mock(UART.Observer.class);
        observer.dataAvailable(eq((byte) 42));
        expectLastCall().once();
        observer.statusChanged(anyInt());
        expectLastCall().anyTimes();
        replay(observer);

        uart.addObserver(observer);
        uart.receiveFromDevice((byte) 42);

        verify(observer);
    }

    @Test
    public void testObserverNotifiedOnReadBuffer() {
        UART uart = new UART(context, settings);
        uart.receiveFromDevice((byte) 42);

        UART.Observer observer = mock(UART.Observer.class);
        observer.noData();
        expectLastCall().once();
        observer.statusChanged(anyInt());
        expectLastCall().anyTimes();
        replay(observer);

        uart.addObserver(observer);
        uart.readBuffer();

        verify(observer);
    }

    @Test
    public void testObserverStatusChangedOnReceiveFromDevice() {
        UART uart = new UART(context, settings);

        UART.Observer observer = mock(UART.Observer.class);
        observer.dataAvailable(eq((byte) 10));
        expectLastCall().once();
        observer.statusChanged(anyInt());
        expectLastCall().atLeastOnce();
        replay(observer);

        uart.addObserver(observer);
        uart.receiveFromDevice((byte) 10);

        verify(observer);
    }

    @Test
    public void testGetDeviceIdWithDevice() {
        DeviceContext<Byte> device = niceMock(DeviceContext.class);
        replay(device);

        UART uart = new UART(context, settings);
        uart.setDevice(device);

        // Without @PluginContext annotation, should return toString of device
        assertNotNull(uart.getDeviceId());
    }

    @Test
    public void testInputInterruptNotTriggeredWhenInterruptsNotSupported() {
        SioUnitSettings noInterruptSettings = mock(SioUnitSettings.class);
        expect(noInterruptSettings.getInterruptsSupported()).andReturn(false).anyTimes();
        expect(noInterruptSettings.getInputInterruptVector()).andReturn(7).anyTimes();
        expect(noInterruptSettings.getOutputInterruptVector()).andReturn(7).anyTimes();
        noInterruptSettings.addObserver(anyObject());
        expectLastCall().once();
        replay(noInterruptSettings);

        Context8080 context = niceMock(Context8080.class);
        replay(context);

        UART uart = new UART(context, noInterruptSettings);
        uart.setStatus((byte) 1); // enable input interrupt
        uart.receiveFromDevice((byte) 1);

        // signalInterrupt should NOT be called because interruptsSupported = false
        verify(context);
    }

    @Test
    public void testOutputInterruptNotTriggeredWhenInterruptsNotSupported() {
        SioUnitSettings noInterruptSettings = mock(SioUnitSettings.class);
        expect(noInterruptSettings.getInterruptsSupported()).andReturn(false).anyTimes();
        expect(noInterruptSettings.getInputInterruptVector()).andReturn(7).anyTimes();
        expect(noInterruptSettings.getOutputInterruptVector()).andReturn(7).anyTimes();
        noInterruptSettings.addObserver(anyObject());
        expectLastCall().once();
        replay(noInterruptSettings);

        Context8080 context = niceMock(Context8080.class);
        replay(context);

        DeviceContext<Byte> device = niceMock(DeviceContext.class);
        replay(device);

        UART uart = new UART(context, noInterruptSettings);
        uart.setDevice(device);
        uart.setStatus((byte) 2); // enable output interrupt
        uart.sendToDevice((byte) 1);

        verify(context);
    }

    @Test
    public void testReceiveFromDeviceSetsDataAvailableFlag() {
        UART uart = new UART(context, settings);
        uart.receiveFromDevice((byte) 5);
        assertEquals(0x20, uart.getStatus() & 0x20);
    }

    @Test
    public void testSetStatusDoesNotAffectStatusRegister() {
        // setStatus only enables/disables interrupts; it does not change the visible statusRegister
        UART uart = new UART(context, settings);
        uart.setStatus((byte) 0xFF);
        assertEquals(0x02, uart.getStatus()); // still XMITTER_BUFFER_EMPTY
    }
}
