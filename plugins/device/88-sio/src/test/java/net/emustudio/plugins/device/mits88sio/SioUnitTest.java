/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
        expect(cpu.attachDevice(eq(1), anyObject(ControlChannel.class))).andReturn(true).times(2);
        expect(cpu.attachDevice(eq(2), anyObject(ControlChannel.class))).andReturn(true).times(2);
        expect(cpu.attachDevice(eq(4), anyObject(DataChannel.class))).andReturn(true).times(2);
        expect(cpu.attachDevice(eq(5), anyObject(DataChannel.class))).andReturn(true).times(2);

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
