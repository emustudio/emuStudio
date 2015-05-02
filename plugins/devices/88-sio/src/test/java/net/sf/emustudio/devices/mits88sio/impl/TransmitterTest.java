package net.sf.emustudio.devices.mits88sio.impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TransmitterTest {

    @Test
    public void testGetDeviceIdReturnsNotNullIfDeviceIsNotAttached() throws Exception {
        assertNotNull(new Transmitter().getDeviceId());
    }

    @Test
    public void testSetNullDeviceDoesNotThrow() throws Exception {
        new Transmitter().setDevice(null);
    }

    @Test
    public void testInitialStatusIs0x00() throws Exception {
        assertEquals(0, new Transmitter().readStatus());
    }

    @Test
    public void testResetOnEmptyBufferSetStatusTo0x02() throws Exception {
        Transmitter transmitter = new Transmitter();
        transmitter.reset();
        assertEquals(0x02, transmitter.readStatus());
    }

    @Test
    public void testWriteToStatus0x03OnEmptyBufferSetStatusTo0x02() throws Exception {
        Transmitter transmitter = new Transmitter();
        transmitter.writeToStatus((short)0x03);
        assertEquals(0x02, transmitter.readStatus());
    }

    @Test
    public void testWriteFromDeviceSetsInputDeviceReady() throws Exception {
        Transmitter transmitter = new Transmitter();
        transmitter.writeFromDevice((short)5);
        assertEquals(1, transmitter.readStatus() & 0x01);
    }

    @Test
    public void testReadBufferResetInputDeviceReady() throws Exception {
        Transmitter transmitter = new Transmitter();
        transmitter.writeFromDevice((short)5);

        assertEquals(5, transmitter.readBuffer());
        assertEquals(0, transmitter.readStatus() & 0x01);
    }

    @Test
    public void testBufferIsFIFO() throws Exception {
        Transmitter transmitter = new Transmitter();
        transmitter.writeFromDevice((short)1);
        transmitter.writeFromDevice((short)2);
        transmitter.writeFromDevice((short)3);

        assertEquals(1, transmitter.readBuffer());
        assertEquals(2, transmitter.readBuffer());
        assertEquals(3, transmitter.readBuffer());
    }
}