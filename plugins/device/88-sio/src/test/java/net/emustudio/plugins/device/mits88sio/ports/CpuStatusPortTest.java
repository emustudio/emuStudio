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
package net.emustudio.plugins.device.mits88sio.ports;

import net.emustudio.plugins.device.mits88sio.Transmitter;
import org.easymock.EasyMock;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class CpuStatusPortTest {

    @Test(expected = NullPointerException.class)
    public void testNullTransmitterInConstructorThrows() {
        new CpuStatusPort(null);
    }

    @Test
    public void testReadCallsReadStatusOnTransmitter() {
        Transmitter transmitter = EasyMock.createMock(Transmitter.class);
        expect(transmitter.readStatus()).andReturn((short) 5).once();
        replay(transmitter);

        CpuStatusPort cpuStatusPort = new CpuStatusPort(transmitter);
        assertEquals((short) 5, cpuStatusPort.readData().shortValue());

        verify(transmitter);
    }

    @Test
    public void testWriteCallsWriteToStatusOnTransmitter() {
        Transmitter transmitter = EasyMock.createMock(Transmitter.class);
        transmitter.writeToStatus(eq((short) 5));
        expectLastCall().once();
        replay(transmitter);

        CpuStatusPort cpuStatusPort = new CpuStatusPort(transmitter);
        cpuStatusPort.writeData((short) 5);

        verify(transmitter);
    }
}
