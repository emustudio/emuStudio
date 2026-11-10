/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram;


import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.debugger.Debugging;
import net.emustudio.emulib.plugin.debugger.StepView;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import org.junit.Test;
import java.util.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DebugHooksTest extends AbstractEngineTest {
    @Test
    public void lightObservationCapturesTypedInstructionAndActualNextAddress() {
        setProgram(instr(RamInstruction.Opcode.HALT, RamInstruction.Direction.DIRECT));
        replay(input, storage, output);
        List<Integer> nextAddresses = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean beforeStep() { return false; }
            @Override public boolean afterStep(boolean completed,
                    Optional<net.emustudio.emulib.plugin.cpu.DecodedInstruction> instruction,
                    OptionalInt nextAddress, Optional<StepView> capture) {
                assertTrue(completed);
                assertEquals(0, instruction.orElseThrow().address());
                assertEquals(1, instruction.orElseThrow().length());
                assertTrue(instruction.orElseThrow().mnemonic().contains("halt"));
                assertTrue(capture.isEmpty());
                nextAddresses.add(nextAddress.orElseThrow());
                return false;
            }
        }, Debugging.Capture.INSTRUCTION);

        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.step());

        assertEquals(List.of(1), nextAddresses);
    }

    @Test public void recordsTypedInstructionAndStringOperandWithoutByteMemory() {
        setProgram(instr(RamInstruction.Opcode.WRITE, RamInstruction.Direction.CONSTANT, "hello"),
                instr(RamInstruction.Opcode.HALT, RamInstruction.Direction.DIRECT));
        output.writeData(eq(new TapeSymbol("hello")));
        expectLastCall().once();
        output.moveRight();
        expectLastCall().once();
        replay(input, storage, output);
        List<StepView> events = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugin.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow();
                events.add(event);
                return false;
            }

            @Override public boolean beforeStep() { return false; }
}, Debugging.Capture.FULL);
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.step());
        assertEquals(2, events.size());
        assertEquals(0, events.get(0).programLocation.orElseThrow());
        assertEquals(Long.valueOf(1), state(events.get(0), "IP", false));
        assertTrue(events.get(0).summary.contains("HELLO"));
        assertEquals("WRITE CONSTANT hello", encoding(events.get(0)));
        assertTrue(events.get(0).rows.stream().noneMatch(row -> row.group.equals("Memory"))); // Typed program/tapes are not byte memory.
        verify(output);
    }

    @Test public void refusedInstructionDoesNotAdvanceTypedProgramCounter() {
        setProgram(instr(RamInstruction.Opcode.HALT, RamInstruction.Direction.DIRECT));
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean beforeStep() { return true; }

            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugin.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) { return false; }
}, Debugging.Capture.FULL);
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(0, engine.IP.get());
        assertTrue(engine.debugStopped);
    }

    @Test
    public void haltResultSurvivesDebuggerStopAfterInstruction() {
        setProgram(instr(RamInstruction.Opcode.HALT, RamInstruction.Direction.DIRECT));
        replay(input, storage, output);
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugin.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow(); return true; }

            @Override public boolean beforeStep() { return false; }
}, Debugging.Capture.FULL);

        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.step());
        assertFalse(engine.debugStopped);
    }

    @Test
    public void missingInstructionSurvivesDebuggerStopAfterInstruction() {
        setProgram();
        replay(input, storage, output);
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugin.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow(); return true; }

            @Override public boolean beforeStep() { return false; }
}, Debugging.Capture.FULL);

        assertEquals(CPU.RunState.STATE_STOPPED_BAD_INSTR, engine.step());
        assertFalse(engine.debugStopped);
    }

    @Test
    public void symbolConditionsAndCapturePreserveStringStorage() throws Exception {
        java.util.concurrent.atomic.AtomicReference<TapeSymbol> r0 = new java.util.concurrent.atomic.AtomicReference<>(TapeSymbol.EMPTY);
        expect(storage.getSymbolAt(0)).andAnswer(() -> Optional.of(r0.get())).anyTimes();
        storage.setSymbolAt(eq(0), anyObject(TapeSymbol.class));
        expectLastCall().andAnswer(() -> { r0.set(getCurrentArgument(1)); return null; }).once();
        setProgram(instr(RamInstruction.Opcode.LOAD, RamInstruction.Direction.CONSTANT, "hello"));
        replay(input, storage, output);
        List<StepView> events = new ArrayList<>();
        Debugging.Expression condition = engine.debugger.compile("R0 == \"hello\"");
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean beforeStep() { return false; }
            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugin.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, Optional<StepView> capture) {
                events.add(capture.orElseThrow());
                return false;
            }
        }, Debugging.Capture.FULL);

        engine.step();

        assertTrue(condition.test());
        assertEquals("hello", engine.debugger.compile("storage(0)").display().text);
        assertTrue(events.get(0).rows.stream().anyMatch(row -> row.label.equals("Write storage[0]") && row.after.equals("hello")));
        assertTrue(events.get(0).rows.stream().anyMatch(row -> row.group.equals("State") && row.label.equals("R0") && row.changed
                && row.before.isEmpty() && row.after.equals("hello")));
        verify(storage);
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
