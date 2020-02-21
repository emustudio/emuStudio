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

import net.sf.emustudio.intel8080.api.ExtendedContext;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.easymock.EasyMock.*;

public class CPUPortsTest {


    @Test
    public void testStatusPortsAreCorrectAfterReattaching() throws Exception {
        Port1 statusPort = mock(Port1.class);
        ExtendedContext cpu = mock(ExtendedContext.class);

        expect(cpu.attachDevice(statusPort, 1)).andReturn(true);
        expect(cpu.attachDevice(statusPort, 2)).andReturn(true);

        cpu.detachDevice(1);
        expectLastCall();
        cpu.detachDevice(2);
        expectLastCall();

        expect(cpu.attachDevice(statusPort, 3)).andReturn(true);
        expect(cpu.attachDevice(statusPort, 2)).andReturn(true);
        replay(cpu);

        CPUPorts ports = new CPUPorts(cpu);

        ports.reattachStatusPort(Arrays.asList(1, 2), statusPort);
        ports.reattachStatusPort(Arrays.asList(3, 2), statusPort);

        verify(cpu);
    }


    @Test
    public void testDataPortsAreCorrectAfterReattaching() throws Exception {
        Port2 dataPort = mock(Port2.class);
        ExtendedContext cpu = mock(ExtendedContext.class);

        expect(cpu.attachDevice(dataPort, 1)).andReturn(true);
        expect(cpu.attachDevice(dataPort, 2)).andReturn(true);

        cpu.detachDevice(1);
        expectLastCall();
        cpu.detachDevice(2);
        expectLastCall();

        expect(cpu.attachDevice(dataPort, 3)).andReturn(true);
        expect(cpu.attachDevice(dataPort, 2)).andReturn(true);
        replay(cpu);

        CPUPorts ports = new CPUPorts(cpu);

        ports.reattachDataPort(Arrays.asList(1, 2), dataPort);
        ports.reattachDataPort(Arrays.asList(3, 2), dataPort);

        verify(cpu);
    }

    @Test
    public void testDestroyDetachesAllPorts() throws Exception {
        ExtendedContext cpu = mock(ExtendedContext.class);
        expect(cpu.attachDevice(anyObject(), anyInt())).andReturn(true).anyTimes();

        cpu.detachDevice(1);
        expectLastCall();
        cpu.detachDevice(2);
        expectLastCall();
        replay(cpu);

        CPUPorts ports = new CPUPorts(cpu);

        ports.reattachStatusPort(Collections.singletonList(1), mock(Port1.class));
        ports.reattachDataPort(Collections.singletonList(2), mock(Port2.class));

        ports.destroy();

        verify(cpu);
    }
}
