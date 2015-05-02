package net.sf.emustudio.devices.mits88sio.impl;

import org.easymock.EasyMock;
import org.junit.Test;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

public class Port2Test {

    @Test(expected = NullPointerException.class)
    public void testNullTransmitterInConstructorThrows() throws Exception {
        new Port2(null);
    }

    @Test
    public void testReadCallsReadBufferOnTransmitter() throws Exception {
        Transmitter transmitter = EasyMock.createMock(Transmitter.class);
        expect(transmitter.readBuffer()).andReturn((short) 5).once();
        replay(transmitter);

        Port2 port2 = new Port2(transmitter);
        assertEquals((short)5, port2.read().shortValue());

        verify(transmitter);
    }

    @Test
    public void testWriteCallsWriteToDeviceOnTransmitter() throws Exception {
        Transmitter transmitter = EasyMock.createMock(Transmitter.class);
        transmitter.writeToDevice(eq((short)5));
        expectLastCall().once();
        replay(transmitter);

        Port2 port2 = new Port2(transmitter);
        port2.write((short)5);

        verify(transmitter);
    }

}