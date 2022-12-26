/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.device.mits88sio;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import org.junit.Test;

import java.util.List;

import static org.easymock.EasyMock.*;

public class SioUnitTest {

    @Test
    public void testCpuPortsAreReattached() {
        SioUnitSettings s = mock(SioUnitSettings.class);
        expect(s.getStatusPorts()).andReturn(List.of(1, 2)).anyTimes();
        expect(s.getDataPorts()).andReturn(List.of(4, 5)).anyTimes();
        expect(s.getInterruptsSupported()).andReturn(true).anyTimes();
        expect(s.getInputInterruptVector()).andReturn(7).anyTimes();
        expect(s.getOutputInterruptVector()).andReturn(7).anyTimes();
        s.addObserver(anyObject());
        expectLastCall().once();
        replay(s);

        Context8080 cpu = mock(Context8080.class);
        expect(cpu.attachDevice(anyObject(ControlChannel.class), eq(1))).andReturn(true).times(2);
        expect(cpu.attachDevice(anyObject(ControlChannel.class), eq(2))).andReturn(true).times(2);
        expect(cpu.attachDevice(anyObject(DataChannel.class), eq(4))).andReturn(true).times(2);
        expect(cpu.attachDevice(anyObject(DataChannel.class), eq(5))).andReturn(true).times(2);

        cpu.detachDevice(1);
        expectLastCall().times(2); // reattach + close()
        cpu.detachDevice(2);
        expectLastCall().times(2);
        cpu.detachDevice(4);
        expectLastCall().times(2);
        cpu.detachDevice(5);
        expectLastCall().times(2);
        replay(cpu);

        try (SioUnit sio = new SioUnit(s, cpu)) {
            sio.attach();
            sio.attach();
        }
        verify(s, cpu);
    }
}
