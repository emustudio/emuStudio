package net.sf.emustudio.devices.mits88sio.impl;

import org.easymock.EasyMock;
import org.junit.Test;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

public class Port1Test {

    @Test(expected = NullPointerException.class)
    public void testNullTransmitterInConstructorThrows() throws Exception {
        new Port1(null);
    }

    @Test
    public void testReadCallsReadStatusOnTransmitter() throws Exception {
        Transmitter transmitter = EasyMock.createMock(Transmitter.class);
        expect(transmitter.readStatus()).andReturn((short) 5).once();
        replay(transmitter);

        Port1 port1 = new Port1(transmitter);
        assertEquals((short)5, port1.read().shortValue());

        verify(transmitter);
    }

    @Test
    public void testWriteCallsWriteToStatusOnTransmitter() throws Exception {
        Transmitter transmitter = EasyMock.createMock(Transmitter.class);
        transmitter.writeToStatus(eq((short)5));
        expectLastCall().once();
        replay(transmitter);

        Port1 port1 = new Port1(transmitter);
        port1.write((short)5);

        verify(transmitter);
    }
}