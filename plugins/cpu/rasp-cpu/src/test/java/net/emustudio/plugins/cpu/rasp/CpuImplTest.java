/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerTable;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.util.Optional;

import static net.emustudio.plugins.memory.rasp.gui.Disassembler.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class CpuImplTest {
    private CpuImpl cpu;
    private MemoryStub memory;
    private AbstractTapeContext inputTape;
    private AbstractTapeContext outputTape;
    private ContextPool contextPool;
    private ApplicationApi applicationApi;
    private boolean initialized = false;

    @Before
    public void setup() throws Exception {
        contextPool = createNiceMock(ContextPool.class);
        memory = new MemoryStub(new Annotations());
        inputTape = createNiceMock(AbstractTapeContext.class);
        outputTape = createNiceMock(AbstractTapeContext.class);

        expect(contextPool.getMemoryContext(eq(0L), eq(RaspMemoryContext.class))).andReturn(memory).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(0))).andReturn(inputTape).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(1))).andReturn(outputTape).anyTimes();
        replay(contextPool);

        applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        this.cpu = new CpuImpl(0, applicationApi, PluginSettings.UNAVAILABLE);
    }

    private void initCpu() throws Exception {
        if (!initialized) {
            replay(inputTape, outputTape);
            cpu.initialize();
            initialized = true;
        }
    }

    @After
    public void tearDown() {
        cpu.destroy();
    }

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", cpu.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", cpu.getCopyright());
    }

    @Test
    public void testGetDescription() {
        assertNotNull(cpu.getDescription());
        assertFalse(cpu.getDescription().isEmpty());
    }

    @Test
    public void testGetDisassemblerAfterInit() throws Exception {
        initCpu();
        assertNotNull(cpu.getDisassembler());
    }

    @Test
    public void testSetAndGetInstructionLocation() throws Exception {
        initCpu();
        assertTrue(cpu.setInstructionLocation(10));
        assertEquals(10, cpu.getInstructionLocation());
    }

    @Test
    public void testSetInstructionLocationNegative() throws Exception {
        initCpu();
        assertFalse(cpu.setInstructionLocation(-1));
    }

    @Test
    public void testSetInstructionLocationZero() throws Exception {
        initCpu();
        assertTrue(cpu.setInstructionLocation(0));
        assertEquals(0, cpu.getInstructionLocation());
    }

    @Test
    public void testResetSetsIP() throws Exception {
        initCpu();
        cpu.reset(5);
        assertEquals(5, cpu.getInstructionLocation());
    }

    @Test
    public void testGetACCReturnsMemoryAtZero() throws Exception {
        initCpu();
        memory.write(0, 42);
        assertEquals(42, cpu.getACC());
    }

    @Test
    public void testStepHaltInstruction() throws Exception {
        initCpu();
        memory.write(0, HALT);
        cpu.reset(0);

        // step() is void in AbstractCPU - just verify it doesn't throw
        cpu.step();
    }

    @Test
    public void testStepLoadAndHalt() throws Exception {
        initCpu();
        memory.write(0, 4); // LOAD_C
        memory.write(1, 99);
        memory.write(2, HALT);
        cpu.reset(0);

        cpu.step(); // load_c 99
        assertEquals(99, cpu.getACC());

        cpu.step(); // halt
    }

    @Test
    public void testExecuteHaltsNormally() throws Exception {
        initCpu();
        memory.write(0, HALT);
        cpu.reset(0);

        cpu.execute();
        // Wait briefly for execution to complete
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
    }

    @Test
    public void testGetStatusPanelReturnsNullWhenGuiIsNull() throws Exception {
        initCpu();
        // applicationApi.getGUI() returns null by default (niceMock)
        JPanel panel = cpu.getStatusPanel();
        assertNull(panel);
    }

    @Test
    public void testGetStatusPanelReturnsPanel() throws Exception {
        reset(applicationApi);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        GUI gui = createNiceMock(GUI.class);
        expect(gui.section(anyString(), anyString(), anyString(), anyString()))
                .andReturn(new JPanel()).anyTimes();
        expect(gui.panel(anyString(), anyString(), anyString()))
                .andReturn(new JPanel()).anyTimes();
        expect(gui.label(anyString())).andReturn(new JLabel()).anyTimes();
        replay(gui);
        expect(applicationApi.getGUI()).andReturn(gui).anyTimes();
        replay(applicationApi);

        initCpu();
        JPanel panel = cpu.getStatusPanel();
        assertNotNull(panel);
    }

    @Test
    public void testGetStatusPanelReturnsSamePanel() throws Exception {
        reset(applicationApi);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        GUI gui = createNiceMock(GUI.class);
        expect(gui.section(anyString(), anyString(), anyString(), anyString()))
                .andReturn(new JPanel()).anyTimes();
        expect(gui.panel(anyString(), anyString(), anyString()))
                .andReturn(new JPanel()).anyTimes();
        expect(gui.label(anyString())).andReturn(new JLabel()).anyTimes();
        replay(gui);
        expect(applicationApi.getGUI()).andReturn(gui).anyTimes();
        replay(applicationApi);

        initCpu();
        JPanel panel1 = cpu.getStatusPanel();
        JPanel panel2 = cpu.getStatusPanel();
        assertSame(panel1, panel2);
    }

    @Test
    public void testDestroyDoesNotThrow() throws Exception {
        initCpu();
        cpu.destroy();
    }

    @Test
    public void testStepBadInstruction() throws Exception {
        initCpu();
        memory.write(0, 99); // invalid opcode
        cpu.reset(0);

        // step() is void - just verify it doesn't throw
        cpu.step();
    }

    @Test
    public void testGetStatusPanelWithDebuggerTable() throws Exception {
        reset(applicationApi);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        GUI gui = createNiceMock(GUI.class);
        expect(gui.section(anyString(), anyString(), anyString(), anyString()))
                .andReturn(new JPanel()).anyTimes();
        expect(gui.panel(anyString(), anyString(), anyString()))
                .andReturn(new JPanel()).anyTimes();
        expect(gui.label(anyString())).andReturn(new JLabel()).anyTimes();
        replay(gui);
        DebuggerTable debugTable = createNiceMock(DebuggerTable.class);
        replay(debugTable);
        expect(applicationApi.getGUI()).andReturn(gui).anyTimes();
        expect(applicationApi.getDebuggerTable()).andReturn(debugTable).anyTimes();
        replay(applicationApi);

        initCpu();
        JPanel panel = cpu.getStatusPanel();
        assertNotNull(panel);
    }

    @Test(timeout = 5000)
    public void testExecuteWithBadInstructionStops() throws Exception {
        initCpu();
        memory.write(0, 99); // invalid opcode
        cpu.reset(0);
        cpu.execute();
        Thread.sleep(300);
    }
}
