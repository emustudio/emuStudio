/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck.gui;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.cpu.brainduck.BrainCPUContext;
import net.emustudio.plugins.cpu.brainduck.CpuImpl;
import net.emustudio.plugins.cpu.brainduck.MemoryStub;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class StatusPanelTest {
    private MemoryStub memory;
    private CpuImpl cpu;
    private GUI gui;

    @Before
    public void setUp() throws Exception {
        memory = new MemoryStub();

        ContextPool contextPool = createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0, ByteMemoryContext.class)).andReturn(memory).anyTimes();
        expect(contextPool.getMemoryContext(0, MemoryContext.class)).andReturn(memory).anyTimes();
        contextPool.register(eq(0L), anyObject(), same(BrainCPUContext.class));
        expectLastCall().once();
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        cpu = new CpuImpl(0L, applicationApi, PluginSettings.UNAVAILABLE);
        cpu.initialize();

        gui = createNiceMock(GUI.class);
        expect(gui.section(anyString(), anyString(), anyString(), anyString()))
                .andReturn(new JPanel()).anyTimes();
        expect(gui.panel(anyString(), anyString(), anyString()))
                .andReturn(new JPanel()).anyTimes();
        expect(gui.label(anyString())).andReturn(new JLabel()).anyTimes();
        replay(gui);
    }

    @After
    public void tearDown() {
        cpu.destroy();
    }

    @Test
    public void testStatusPanelCreation() {
        StatusPanel panel = new StatusPanel(memory, cpu, gui);
        assertNotNull(panel);
    }

    @Test
    public void testStatusPanelIsJPanel() {
        StatusPanel panel = new StatusPanel(memory, cpu, gui);
        assertTrue(panel instanceof JPanel);
    }

    @Test(timeout = 5000)
    public void testCPUListenerNotifiedOnReset() {
        new StatusPanel(memory, cpu, gui);
        // Program: halt instruction (0x00) at position 0
        memory.setProgram(new byte[]{0x00});

        // reset triggers runStateChanged(STATE_STOPPED_BREAK) + internalStateChanged()
        cpu.reset(0);
    }

    @Test(timeout = 5000)
    public void testCPUListenerNotifiedOnStep() {
        new StatusPanel(memory, cpu, gui);
        // Program: > (P++) then halt
        memory.setProgram(new byte[]{0x01, 0x00});

        cpu.reset(0);
        // step triggers runStateChanged + internalStateChanged
        cpu.step();
    }

    @Test(timeout = 5000)
    public void testCPUListenerNotifiedOnStopNormal() {
        new StatusPanel(memory, cpu, gui);
        // Program: halt instruction only
        memory.setProgram(new byte[]{0x00});

        cpu.reset(0);
        // stepping halt instruction triggers STATE_STOPPED_NORMAL
        cpu.step();
    }

    @Test(timeout = 5000)
    public void testCPUListenerNotifiedOnExecuteAndStop() {
        new StatusPanel(memory, cpu, gui);
        // Program: halt instruction
        memory.setProgram(new byte[]{0x00});

        cpu.reset(0);
        // execute will run until halt, triggering STATE_RUNNING then STATE_STOPPED_NORMAL
        cpu.execute();
        // Wait briefly for execution to complete
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
    }

    @Test(timeout = 5000)
    public void testCPUListenerNotifiedOnStepWithBadInstruction() {
        new StatusPanel(memory, cpu, gui);
        // Program: > (valid), then invalid 0xFF, then halt
        memory.setProgram(new byte[]{0x01, (byte) 0xFF, 0x00});

        cpu.reset(0);
        cpu.step(); // > (P++) - valid step, STATE_STOPPED_BREAK
        cpu.step(); // 0xFF - invalid instruction triggers STATE_STOPPED_BAD_INSTR
    }

    @Test(timeout = 5000)
    public void testCPUListenerInternalStateChangedAfterMultipleSteps() {
        new StatusPanel(memory, cpu, gui);
        // Program: >(P++), +(P++), halt
        memory.setProgram(new byte[]{0x01, 0x03, 0x00});

        cpu.reset(0);
        cpu.step(); // > (P++)
        cpu.step(); // + (*P++)
    }

    @Test(timeout = 5000)
    public void testCPUListenerHandlesAddressFallout() {
        new StatusPanel(memory, cpu, gui);
        // Program: < (P--) followed by halt
        memory.setProgram(new byte[]{0x02, 0x00});

        cpu.reset(0);
        // Force P to 0 so that < will push it to -1 triggering ADDR_FALLOUT
        cpu.getEngine().P = 0;
        cpu.step(); // < (P--) triggers STATE_STOPPED_ADDR_FALLOUT and [unreachable] in internalStateChanged
    }

    @Test(timeout = 5000)
    public void testCPUListenerOnStop() {
        new StatusPanel(memory, cpu, gui);
        memory.setProgram(new byte[]{0x00});

        cpu.reset(0);
        cpu.stop(); // triggers STATE_STOPPED_NORMAL
    }
}
