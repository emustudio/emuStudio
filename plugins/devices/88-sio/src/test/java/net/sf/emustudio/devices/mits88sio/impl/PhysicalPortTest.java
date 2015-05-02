package net.sf.emustudio.devices.mits88sio.impl;

import org.easymock.EasyMock;
import org.junit.Test;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

public class PhysicalPortTest {

    @Test(expected = NullPointerException.class)
    public void testNullTransmitterInConstructorThrows() throws Exception {
        new PhysicalPort(null);
    }

    @Test
    public void testReadReturnsZero() throws Exception {
        Transmitter transmitter = EasyMock.createMock(Transmitter.class);
        replay(transmitter);

        PhysicalPort port = new PhysicalPort(transmitter);
        assertEquals((short)0, port.read().shortValue());

        verify(transmitter);
    }

    @Test
    public void testWriteCallsWriteFromDeviceOnTransmitter() throws Exception {
        Transmitter transmitter = EasyMock.createMock(Transmitter.class);
        transmitter.writeFromDevice(eq((short)5));
        expectLastCall().once();
        replay(transmitter);

        PhysicalPort port = new PhysicalPort(transmitter);
        port.write((short)5);

        verify(transmitter);
    }

}