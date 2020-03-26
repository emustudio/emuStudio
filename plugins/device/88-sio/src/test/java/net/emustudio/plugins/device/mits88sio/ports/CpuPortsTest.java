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

import net.emustudio.plugins.cpu.intel8080.api.ExtendedContext;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.easymock.EasyMock.*;

public class CpuPortsTest {


    @Test
    public void testStatusPortsAreCorrectAfterReattaching() throws Exception {
        CpuStatusPort cpuStatusPort = mock(CpuStatusPort.class);
        ExtendedContext cpu = mock(ExtendedContext.class);

        expect(cpu.attachDevice(cpuStatusPort, 1)).andReturn(true);
        expect(cpu.attachDevice(cpuStatusPort, 2)).andReturn(true);

        cpu.detachDevice(1);
        expectLastCall();
        cpu.detachDevice(2);
        expectLastCall();

        expect(cpu.attachDevice(cpuStatusPort, 3)).andReturn(true);
        expect(cpu.attachDevice(cpuStatusPort, 2)).andReturn(true);
        replay(cpu);

        CpuPorts ports = new CpuPorts(cpu);

        ports.reattachStatusPort(Arrays.asList(1, 2), cpuStatusPort);
        ports.reattachStatusPort(Arrays.asList(3, 2), cpuStatusPort);

        verify(cpu);
    }


    @Test
    public void testDataPortsAreCorrectAfterReattaching() throws CouldNotAttachException {
        CpuDataPort cpuDataPort = mock(CpuDataPort.class);
        ExtendedContext cpu = mock(ExtendedContext.class);

        expect(cpu.attachDevice(cpuDataPort, 1)).andReturn(true);
        expect(cpu.attachDevice(cpuDataPort, 2)).andReturn(true);

        cpu.detachDevice(1);
        expectLastCall();
        cpu.detachDevice(2);
        expectLastCall();

        expect(cpu.attachDevice(cpuDataPort, 3)).andReturn(true);
        expect(cpu.attachDevice(cpuDataPort, 2)).andReturn(true);
        replay(cpu);

        CpuPorts ports = new CpuPorts(cpu);

        ports.reattachDataPort(Arrays.asList(1, 2), cpuDataPort);
        ports.reattachDataPort(Arrays.asList(3, 2), cpuDataPort);

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

        CpuPorts ports = new CpuPorts(cpu);

        ports.reattachStatusPort(Collections.singletonList(1), mock(CpuStatusPort.class));
        ports.reattachDataPort(Collections.singletonList(2), mock(CpuDataPort.class));

        ports.destroy();

        verify(cpu);
    }
}
