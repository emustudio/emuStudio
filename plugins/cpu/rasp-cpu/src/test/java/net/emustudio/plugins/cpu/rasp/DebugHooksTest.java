/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugin.cpu.CPUContext;

import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.debugger.Debugging;
import net.emustudio.emulib.plugin.debugger.StepView;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import org.junit.Test;
import java.util.*;
import static net.emustudio.plugins.memory.rasp.gui.Disassembler.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DebugHooksTest {
    @Test public void recordsIntegerWordsAndPreservesNegativeValues() throws Exception {
        MemoryStub memory = new MemoryStub();
        memory.write(0, 0);
        memory.write(5, 4); // LOAD constant
        memory.write(6, -300);
        memory.write(7, HALT);
        AbstractTapeContext input = createNiceMock(AbstractTapeContext.class);
        AbstractTapeContext output = createNiceMock(AbstractTapeContext.class);
        expect(input.getSymbolAt(anyInt())).andStubReturn(java.util.Optional.of(new net.emustudio.plugins.device.abstracttape.api.TapeSymbol(-12)));
        expect(output.getSymbolAt(anyInt())).andStubReturn(java.util.Optional.empty());
        replay(input, output);
        EmulatorEngine engine = new EmulatorEngine(memory, input, output);
        engine.IP.set(5);
        List<StepView> events = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow();
                events.add(event);
                return false;
            }

            @Override public boolean beforeStep() { return false; }
}, true);
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.step());
        assertEquals(2, events.size());
        assertTrue(engine.debugger.compile("INPUT == -12").test());
        assertTrue(events.get(0).rows.stream().anyMatch(row -> row.label.equals("Read @0006") && row.after.equals("-300")));
        assertTrue(events.get(0).rows.stream().anyMatch(row -> row.label.equals("Write @0000") && row.after.equals("-300")));
        assertEquals(5, events.get(0).programLocation.orElseThrow());
        assertTrue(encoding(events.get(0)).contains(Integer.toHexString(-300)));
        assertTrue(events.get(0).summary.contains("-300"));
        assertEquals(Long.valueOf(-300), state(events.get(0), "ACC", false));
        assertTrue(events.get(0).rows.stream().filter(row -> row.group.equals("Memory")).anyMatch(a -> a.label.equals("Write @0000") && Long.parseLong(a.after) == -300));
        assertTrue(events.get(0).rows.stream().filter(row -> row.group.equals("Memory")).anyMatch(a -> a.label.equals("Read @0006") && Long.parseLong(a.after) == -300));
    }

    @Test public void refusedInstructionDoesNotFetchOrAdvance() throws Exception {
        MemoryStub memory = new MemoryStub();
        memory.write(0, HALT);
        AbstractTapeContext input = createNiceMock(AbstractTapeContext.class);
        AbstractTapeContext output = createNiceMock(AbstractTapeContext.class);
        expect(input.getSymbolAt(anyInt())).andStubReturn(java.util.Optional.empty());
        expect(output.getSymbolAt(anyInt())).andStubReturn(java.util.Optional.empty());
        replay(input, output);
        EmulatorEngine engine = new EmulatorEngine(memory, input, output);
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean beforeStep() { return true; }

            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) { return false; }
}, true);
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(0, engine.IP.get());
        assertTrue(engine.debugStopped);
    }

    @Test
    public void haltResultSurvivesDebuggerStopAfterInstruction() throws Exception {
        MemoryStub memory = new MemoryStub();
        memory.write(0, HALT);
        AbstractTapeContext input = createNiceMock(AbstractTapeContext.class);
        AbstractTapeContext output = createNiceMock(AbstractTapeContext.class);
        expect(input.getSymbolAt(anyInt())).andStubReturn(java.util.Optional.empty());
        expect(output.getSymbolAt(anyInt())).andStubReturn(java.util.Optional.empty());
        replay(input, output);
        EmulatorEngine engine = new EmulatorEngine(memory, input, output);
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow(); return true; }

            @Override public boolean beforeStep() { return false; }
}, true);

        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.step());
        assertFalse(engine.debugStopped);
    }

    @Test
    public void invalidInstructionSurvivesDebuggerStopAfterInstruction() throws Exception {
        MemoryStub memory = new MemoryStub();
        memory.write(0, 99);
        AbstractTapeContext input = createNiceMock(AbstractTapeContext.class);
        AbstractTapeContext output = createNiceMock(AbstractTapeContext.class);
        expect(input.getSymbolAt(anyInt())).andStubReturn(java.util.Optional.empty());
        expect(output.getSymbolAt(anyInt())).andStubReturn(java.util.Optional.empty());
        replay(input, output);
        EmulatorEngine engine = new EmulatorEngine(memory, input, output);
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow(); return true; }

            @Override public boolean beforeStep() { return false; }
}, true);

        assertEquals(CPU.RunState.STATE_STOPPED_BAD_INSTR, engine.step());
        assertFalse(engine.debugStopped);
    }

    @Test
    public void accumulatorEditorPreservesSignedHexWord() throws Exception {
        MemoryStub memory = new MemoryStub();
        AbstractTapeContext input = createNiceMock(AbstractTapeContext.class);
        AbstractTapeContext output = createNiceMock(AbstractTapeContext.class);
        replay(input, output);
        EmulatorEngine engine = new EmulatorEngine(memory, input, output);
        CPUContext context = org.easymock.EasyMock.createNiceMock(CPUContext.class);
        org.easymock.EasyMock.replay(context);
        javax.swing.JPanel editor = engine.debugger.editor(() -> {});

        javax.swing.SwingUtilities.invokeAndWait(() -> {
            ((javax.swing.JComboBox<?>) editor.getComponent(0)).setSelectedItem("ACC");
            ((javax.swing.JTextField) editor.getComponent(1)).setText("0xffffffff");
            ((javax.swing.JButton) editor.getComponent(2)).doClick();
        });

        assertEquals(-1, memory.read(0).intValue());
        assertTrue(engine.debugger.compile("ACC == -1").test());
    }

    private static String encoding(net.emustudio.emulib.plugin.debugger.StepView event) {
        return event.rows.stream().filter(row -> row.group.equals("Instruction") && row.label.equals("Encoding")).findFirst().orElseThrow().after;
    }

    private static Long state(net.emustudio.emulib.plugin.debugger.StepView event, String name, boolean before) {
        net.emustudio.emulib.plugin.debugger.ViewRow row = event.rows.stream()
                .filter(value -> value.group.equals("State") && value.label.equals(name)).findFirst().orElseThrow();
        return Long.valueOf(before ? row.before : row.after);
    }
}
