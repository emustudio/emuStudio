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

public class CpuDataPortTest {

    @Test(expected = NullPointerException.class)
    public void testNullTransmitterInConstructorThrows() {
        new CpuDataPort(null);
    }

    @Test
    public void testReadCallsReadBufferOnTransmitter() {
        Transmitter transmitter = EasyMock.createMock(Transmitter.class);
        expect(transmitter.readBuffer()).andReturn((short) 5).once();
        replay(transmitter);

        CpuDataPort cpuDataPort = new CpuDataPort(transmitter);
        assertEquals((short) 5, cpuDataPort.readData().shortValue());

        verify(transmitter);
    }

    @Test
    public void testWriteCallsWriteToDeviceOnTransmitter() throws Exception {
        Transmitter transmitter = EasyMock.createMock(Transmitter.class);
        transmitter.writeToDevice(eq((short) 5));
        expectLastCall().once();
        replay(transmitter);

        CpuDataPort cpuDataPort = new CpuDataPort(transmitter);
        cpuDataPort.writeData((short) 5);

        verify(transmitter);
    }

}
