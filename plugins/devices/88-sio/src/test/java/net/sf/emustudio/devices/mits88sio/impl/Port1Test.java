/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
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
