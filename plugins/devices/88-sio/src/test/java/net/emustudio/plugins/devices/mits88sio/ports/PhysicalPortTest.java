/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.devices.mits88sio.ports;

import net.emustudio.plugins.devices.mits88sio.Transmitter;
import org.easymock.EasyMock;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class PhysicalPortTest {

    @Test(expected = NullPointerException.class)
    public void testNullTransmitterInConstructorThrows() {
        new PhysicalPort(null);
    }

    @Test
    public void testReadReturnsZero() {
        Transmitter transmitter = EasyMock.createMock(Transmitter.class);
        replay(transmitter);

        PhysicalPort port = new PhysicalPort(transmitter);
        assertEquals((short) 0, port.readData().shortValue());

        verify(transmitter);
    }

    @Test
    public void testWriteCallsWriteFromDeviceOnTransmitter() {
        Transmitter transmitter = EasyMock.createMock(Transmitter.class);
        transmitter.writeFromDevice(eq((short) 5));
        expectLastCall().once();
        replay(transmitter);

        PhysicalPort port = new PhysicalPort(transmitter);
        port.writeData((short) 5);

        verify(transmitter);
    }

}
