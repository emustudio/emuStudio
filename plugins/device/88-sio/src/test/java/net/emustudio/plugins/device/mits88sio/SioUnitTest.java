/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88sio;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertNotNull;

public class SioUnitTest {

    private SioUnitSettings createSettings(List<Integer> statusPorts, List<Integer> dataPorts) {
        SioUnitSettings s = mock(SioUnitSettings.class);
        expect(s.getStatusPorts()).andReturn(statusPorts).anyTimes();
        expect(s.getDataPorts()).andReturn(dataPorts).anyTimes();
        expect(s.getInterruptsSupported()).andReturn(true).anyTimes();
        expect(s.getInputInterruptVector()).andReturn(7).anyTimes();
        expect(s.getOutputInterruptVector()).andReturn(7).anyTimes();
        s.addObserver(anyObject());
        expectLastCall().anyTimes();
        return s;
    }

    @Test
    public void testCpuPortsAreReattached() {
        SioUnitSettings s = createSettings(List.of(1, 2), List.of(4, 5));
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

    @Test
    public void testGetUARTReturnsNotNull() {
        SioUnitSettings s = createSettings(Collections.emptyList(), Collections.emptyList());
        replay(s);

        Context8080 cpu = niceMock(Context8080.class);
        replay(cpu);

        try (SioUnit sio = new SioUnit(s, cpu)) {
            assertNotNull(sio.getUART());
        }
    }

    @Test
    public void testCloseDetachesAllPorts() {
        SioUnitSettings s = createSettings(List.of(0x10), List.of(0x11));
        replay(s);

        Context8080 cpu = mock(Context8080.class);
        expect(cpu.attachDevice(eq(0x10), anyObject(ControlChannel.class))).andReturn(true).once();
        expect(cpu.attachDevice(eq(0x11), anyObject(DataChannel.class))).andReturn(true).once();
        cpu.detachDevice(0x10);
        expectLastCall().once();
        cpu.detachDevice(0x11);
        expectLastCall().once();
        replay(cpu);

        SioUnit sio = new SioUnit(s, cpu);
        sio.attach();
        sio.close();

        verify(cpu);
    }

    @Test
    public void testAttachWithEmptyPortsDoesNotFail() {
        SioUnitSettings s = createSettings(Collections.emptyList(), Collections.emptyList());
        replay(s);

        Context8080 cpu = niceMock(Context8080.class);
        replay(cpu);

        try (SioUnit sio = new SioUnit(s, cpu)) {
            sio.attach();
        }
    }

    @Test
    public void testDetachBeforeAttachDoesNotFail() {
        SioUnitSettings s = createSettings(Collections.emptyList(), Collections.emptyList());
        replay(s);

        Context8080 cpu = niceMock(Context8080.class);
        replay(cpu);

        try (SioUnit sio = new SioUnit(s, cpu)) {
            sio.detach(); // should not throw even though nothing was attached
        }
    }

    @Test
    public void testResetDelegatesToUART() {
        SioUnitSettings s = createSettings(Collections.emptyList(), Collections.emptyList());
        replay(s);

        Context8080 cpu = niceMock(Context8080.class);
        replay(cpu);

        try (SioUnit sio = new SioUnit(s, cpu)) {
            // reset should not throw
            sio.reset(false);
            sio.reset(true);
        }
    }

    @SuppressWarnings("resource")
    @Test(expected = NullPointerException.class)
    public void testNullSettingsThrows() {
        Context8080 cpu = niceMock(Context8080.class);
        replay(cpu);
        new SioUnit(null, cpu);
    }

    @SuppressWarnings("resource")
    @Test(expected = NullPointerException.class)
    public void testNullCpuThrows() {
        SioUnitSettings s = createSettings(Collections.emptyList(), Collections.emptyList());
        replay(s);
        new SioUnit(s, null);
    }
}
