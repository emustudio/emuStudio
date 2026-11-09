/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram;

import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.emustudio.emulib.plugins.debugger.Debugging;
import net.emustudio.emulib.plugins.debugger.StepView;

import javax.swing.*;
import java.util.Collections;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class CpuImplTest {
    private CpuImpl cpu;
    private RamMemoryContext memory;
    private AbstractTapeContext inputTape;
    private AbstractTapeContext outputTape;
    private AbstractTapeContext storageTape;
    private ContextPool contextPool;
    private ApplicationApi applicationApi;
    private CPUContext execution;
    private boolean initialized = false;

    @Before
    public void setup() throws Exception {
        contextPool = createNiceMock(ContextPool.class);
        memory = createNiceMock(RamMemoryContext.class);
        inputTape = createNiceMock(AbstractTapeContext.class);
        outputTape = createNiceMock(AbstractTapeContext.class);
        storageTape = createNiceMock(AbstractTapeContext.class);
        expect(inputTape.getSymbolAt(anyInt())).andStubReturn(java.util.Optional.empty());
        expect(outputTape.getSymbolAt(anyInt())).andStubReturn(java.util.Optional.empty());
        expect(storageTape.getSymbolAt(anyInt())).andStubReturn(java.util.Optional.empty());

        expect(contextPool.get(eq(0L), eq(RamMemoryContext.class))).andReturn(memory).anyTimes();
        expect(contextPool.get(eq(0L), eq(AbstractTapeContext.class), eq(0))).andReturn(storageTape).anyTimes();
        expect(contextPool.get(eq(0L), eq(AbstractTapeContext.class), eq(1))).andReturn(inputTape).anyTimes();
        expect(contextPool.get(eq(0L), eq(AbstractTapeContext.class), eq(2))).andReturn(outputTape).anyTimes();

        replay(contextPool);

        execution = createNiceMock(CPUContext.class);
        replay(execution);
        applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        this.cpu = new CpuImpl(0, applicationApi, PluginSettings.UNAVAILABLE);
    }

    private void initCpu() throws Exception {
        if (!initialized) {
            expect(memory.getInputs()).andReturn(Collections.emptyList()).anyTimes();
            replay(memory, inputTape, outputTape, storageTape);
            cpu.initialize();
            initialized = true;
        }
    }

    @After
    public void tearDown() {
        cpu.destroy();
    }

    @Test
    public void testAddressSpaceDoesNotDependOnLoadedProgram() {
        assertEquals(Integer.MAX_VALUE, cpu.getAddressSpaceSize());
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
    public void testGetR0WhenStorageIsNull() {
        // Before initialization, storageTape in context is null
        // getR0 should return TapeSymbol.EMPTY
        TapeSymbol r0 = cpu.getR0();
        assertEquals(TapeSymbol.EMPTY, r0);
    }

    @Test
    public void testGetR0ReturnsValue() throws Exception {
        reset(storageTape);
        storageTape.setLeftBounded(true);
        storageTape.setEditable(true);
        storageTape.setHighlightHeadPosition(false);
        storageTape.setClearAtReset(true);
        storageTape.setTitle("Storage");
        storageTape.setShowPositions(true);
        storageTape.clear();
        expectLastCall().anyTimes();
        expect(storageTape.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(42))).anyTimes();
        replay(storageTape);

        expect(memory.getInputs()).andReturn(Collections.emptyList()).anyTimes();
        replay(memory, inputTape, outputTape);

        cpu.initialize();
        initialized = true;

        cpu.reset(0);
        TapeSymbol r0 = cpu.getR0();
        assertEquals(42, r0.number);
    }

    @Test
    public void testStepCallsEngine() throws Exception {
        reset(memory, storageTape, inputTape, outputTape);

        RamInstruction haltInstr = createNiceMock(RamInstruction.class);
        expect(haltInstr.getOpcode()).andReturn(RamInstruction.Opcode.HALT).anyTimes();
        expect(haltInstr.getDirection()).andReturn(RamInstruction.Direction.DIRECT).anyTimes();
        expect(haltInstr.getOperand()).andReturn(null).anyTimes();
        expect(haltInstr.getLabel()).andReturn(null).anyTimes();
        replay(haltInstr);

        expect(memory.read(0)).andReturn(haltInstr).anyTimes();
        expect(memory.getInputs()).andReturn(Collections.emptyList()).anyTimes();
        replay(memory);

        storageTape.setLeftBounded(true);
        storageTape.setEditable(true);
        storageTape.setHighlightHeadPosition(false);
        storageTape.setClearAtReset(true);
        storageTape.setTitle("Storage");
        storageTape.setShowPositions(true);
        storageTape.clear();
        expectLastCall().anyTimes();
        replay(storageTape);

        inputTape.setLeftBounded(true);
        inputTape.setEditable(true);
        inputTape.setHighlightHeadPosition(true);
        inputTape.setClearAtReset(false);
        inputTape.setTitle("Input tape");
        inputTape.setShowPositions(true);
        inputTape.clear();
        expectLastCall().anyTimes();
        replay(inputTape);

        outputTape.setLeftBounded(true);
        outputTape.setEditable(false);
        outputTape.setHighlightHeadPosition(true);
        outputTape.setClearAtReset(true);
        outputTape.setTitle("Output tape");
        outputTape.setShowPositions(true);
        outputTape.clear();
        expectLastCall().anyTimes();
        replay(outputTape);

        cpu.initialize();
        initialized = true;
        cpu.reset(0);

        // step() is void - it triggers listeners. Just verify it doesn't throw.
        cpu.step();
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
    public void testDestroyDoesNotThrow() throws Exception {
        initCpu();
        cpu.destroy();
    }

    @Test(timeout = 5000)
    public void testExecuteWithHaltInstruction() throws Exception {
        reset(memory, storageTape, inputTape, outputTape);

        RamInstruction haltInstr = createNiceMock(RamInstruction.class);
        expect(haltInstr.getOpcode()).andReturn(RamInstruction.Opcode.HALT).anyTimes();
        expect(haltInstr.getDirection()).andReturn(RamInstruction.Direction.DIRECT).anyTimes();
        expect(haltInstr.getOperand()).andReturn(null).anyTimes();
        expect(haltInstr.getLabel()).andReturn(null).anyTimes();
        replay(haltInstr);

        expect(memory.read(anyInt())).andReturn(haltInstr).anyTimes();
        expect(memory.getInputs()).andReturn(Collections.emptyList()).anyTimes();
        replay(memory);

        storageTape.setLeftBounded(anyBoolean());
        expectLastCall().anyTimes();
        storageTape.setEditable(anyBoolean());
        expectLastCall().anyTimes();
        storageTape.setHighlightHeadPosition(anyBoolean());
        expectLastCall().anyTimes();
        storageTape.setClearAtReset(anyBoolean());
        expectLastCall().anyTimes();
        storageTape.setTitle(anyString());
        expectLastCall().anyTimes();
        storageTape.setShowPositions(anyBoolean());
        expectLastCall().anyTimes();
        storageTape.clear();
        expectLastCall().anyTimes();
        replay(storageTape);

        inputTape.setLeftBounded(anyBoolean());
        expectLastCall().anyTimes();
        inputTape.setEditable(anyBoolean());
        expectLastCall().anyTimes();
        inputTape.setHighlightHeadPosition(anyBoolean());
        expectLastCall().anyTimes();
        inputTape.setClearAtReset(anyBoolean());
        expectLastCall().anyTimes();
        inputTape.setTitle(anyString());
        expectLastCall().anyTimes();
        inputTape.setShowPositions(anyBoolean());
        expectLastCall().anyTimes();
        inputTape.clear();
        expectLastCall().anyTimes();
        replay(inputTape);

        outputTape.setLeftBounded(anyBoolean());
        expectLastCall().anyTimes();
        outputTape.setEditable(anyBoolean());
        expectLastCall().anyTimes();
        outputTape.setHighlightHeadPosition(anyBoolean());
        expectLastCall().anyTimes();
        outputTape.setClearAtReset(anyBoolean());
        expectLastCall().anyTimes();
        outputTape.setTitle(anyString());
        expectLastCall().anyTimes();
        outputTape.setShowPositions(anyBoolean());
        expectLastCall().anyTimes();
        outputTape.clear();
        expectLastCall().anyTimes();
        replay(outputTape);

        cpu.initialize();
        initialized = true;
        cpu.reset(0);
        cpu.execute();
        Thread.sleep(300);
    }

    @Test
    public void testDebuggerColumns() throws Exception {
        initCpu();
        java.util.List<DebuggerColumn<?>> columns = cpu.getDebuggerColumns();
        assertEquals(2, columns.size());
        assertEquals("label", columns.get(0).getTitle());
        assertEquals("instruction", columns.get(1).getTitle());
    }

    @Test
    public void testGetStatusPanelWithGui() throws Exception {
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
    @Test(timeout = 3000)
    public void recordingStopDoesNotOverrideContinuousHaltResult() throws Exception {
        RamInstruction instruction = createNiceMock(RamInstruction.class);
        expect(instruction.getOpcode()).andReturn(RamInstruction.Opcode.HALT).anyTimes();
        expect(instruction.getDirection()).andReturn(RamInstruction.Direction.DIRECT).anyTimes();
        replay(instruction);
        expect(memory.read(0)).andReturn(instruction).anyTimes();
        initCpu();
        java.util.List<StepView> events = new java.util.ArrayList<>();
        cpu.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow();
                events.add(event);
                return true;
            }

            @Override public boolean beforeStep() { return false; }
}, true);
        assertEquals(net.emustudio.emulib.plugins.cpu.CPU.RunState.STATE_STOPPED_NORMAL, cpu.execute());
        assertEquals(1, events.size());
        assertEquals(1, cpu.getInstructionLocation());
    }

    @Test
    public void interruptedExecutionReturnsContinuingStateForHostToResolve() throws Exception {
        initCpu();
        Thread.currentThread().interrupt();
        try {
            assertEquals(net.emustudio.emulib.plugins.cpu.CPU.RunState.STATE_RUNNING, cpu.execute());
        } finally {
            Thread.interrupted();
        }
    }
}
