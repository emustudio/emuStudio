/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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

import static org.easymock.EasyMock.*;
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
        assertEquals((short) 0, port.read().shortValue());

        verify(transmitter);
    }

    @Test
    public void testWriteCallsWriteFromDeviceOnTransmitter() throws Exception {
        Transmitter transmitter = EasyMock.createMock(Transmitter.class);
        transmitter.writeFromDevice(eq((short) 5));
        expectLastCall().once();
        replay(transmitter);

        PhysicalPort port = new PhysicalPort(transmitter);
        port.write((short) 5);

        verify(transmitter);
    }

}
