/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080;


import static org.junit.Assert.*;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugin.debugger.Debugging;
import net.emustudio.emulib.plugin.debugger.StepView;
import net.emustudio.emulib.Numbers;

import org.junit.Test;

import java.util.*;

public class DebugHooksTest {
    @Test
    public void runStopsAtApplicationBreakpointBeforeExecutingInstruction() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, new Byte[] {0x3e, 0x42, 0x3c}, 3);
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
        Debugging.Observer listener = new Debugging.Observer() {
            @Override public boolean beforeStep() { int address = engine.PC; return address == 2; }

            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugin.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) { return false; }
};
        AutoCloseable observation = engine.debugger.observe(listener, Debugging.Capture.FULL);

        assertEquals(net.emustudio.emulib.plugin.cpu.CPU.RunState.STATE_STOPPED_BREAK, engine.run());

        assertEquals(2, engine.PC);
        assertEquals(0x42, engine.regs[7]);
        observation.close();
        engine.step();
        assertEquals(3, engine.PC);
        assertEquals(0x43, engine.regs[7]);
    }

    @Test
    public void disabledProbeKeepsOriginalMemoryReference() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
        java.lang.reflect.Field field = EmulatorEngine.class.getDeclaredField("memory");
        field.setAccessible(true);
        assertSame(memory, field.get(engine));
        Debugging.Observer listener = new Debugging.Observer() {
            @Override public boolean beforeStep() { return false; }

            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugin.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) { return false; }
};
        AutoCloseable observation = engine.debugger.observe(listener, Debugging.Capture.FULL);
        assertNotSame(memory, field.get(engine));
        observation.close();
        assertSame(memory, field.get(engine));
        assertFalse(engine.debugger.isActive());
    }

    @Test
    public void realInstructionsReportGuestWritesAndRegisterChanges() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, new Byte[] {0x3e, 0x42, 0x32, 0x00, 0x01}, 5);
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
        List<StepView> events = new ArrayList<>();
        engine.debugger.observe(
                new Debugging.Observer() {
                    @Override
                    public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugin.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) {
                StepView e = capture.orElseThrow();
                        events.add(e);
                        return false;
                    }

            @Override public boolean beforeStep() { return false; }
}, Debugging.Capture.FULL);
        engine.step();
        engine.step();
        assertEquals(2, events.size());
        assertEquals(0, events.get(0).programLocation.orElseThrow());
        assertEquals(2, events.get(1).programLocation.orElseThrow());
        assertEquals("3E 42", encoding(events.get(0)).trim());
        assertEquals("32 00 01", encoding(events.get(1)).trim());
        assertFalse(events.get(0).summary.isBlank());
        assertEquals(Long.valueOf(0x42), state(events.get(0), "A", false));
        assertTrue(
                events.get(1).rows.stream().filter(row -> row.group.equals("Memory"))
                        .anyMatch(a -> a.label.equals("Write @0100") && Long.parseLong(a.after) == 0x42));
        memory.read(0);
        assertEquals(2, events.size());
        assertEquals(0x42, memory.read(0x100) & 255);
    }

    @Test
    public void historyPreservesLoopIterationsAndExecutionTimeCodeAcrossReset() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, new Byte[] {0x3c, (byte) 0xc3, 0x00, 0x00}, 4);
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
        List<StepView> events = collect(engine);
        engine.step();
        engine.step();
        engine.step();
        assertEquals(3, events.size());
        assertEquals(0, events.get(0).programLocation.orElseThrow());
        assertEquals(1, events.get(1).programLocation.orElseThrow());
        assertEquals(0, events.get(2).programLocation.orElseThrow());
        assertEquals(Long.valueOf(1), state(events.get(0), "A", false));
        assertEquals(Long.valueOf(2), state(events.get(2), "A", false));
        assertEquals("3C", encoding(events.get(0)).trim());
        memory.write(0, (byte) 0);
        engine.reset(0);
        engine.step();
        assertEquals("00", encoding(events.get(3)).trim());
        assertEquals("3C", encoding(events.get(0)).trim());
    }

    @Test
    public void interruptRecordsInjectedInstructionInsteadOfOldPcCode() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, (byte) 0x3c);
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
        List<StepView> events = collect(engine);
        engine.SP = 0x100;
        engine.INTE = true;
        engine.interrupt((short) 0xcf, (short) 0, (short) 0);
        engine.step();
        assertEquals(1, events.size());
        assertEquals("INT RST", events.get(0).summary);
        assertTrue(events.get(0).programLocation.isEmpty());
        assertEquals("CF", encoding(events.get(0)));
        assertEquals(Long.valueOf(8), state(events.get(0), "PC", false));
        assertEquals(Long.valueOf(0), state(events.get(0), "A", false));
    }

    private static List<StepView> collect(EmulatorEngine engine) {
        List<StepView> events = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugin.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow();
                events.add(event);
                return false;
            }

            @Override public boolean beforeStep() { return false; }
}, Debugging.Capture.FULL);
        return events;
    }

    @Test
    public void canStopBeforeInstructionWithoutMutatingRegisters() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, (byte) 0x3c);
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
        engine.debugger.observe(
                new Debugging.Observer() {
                    @Override
                    public boolean beforeStep() {
                        return true;
                    }

            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugin.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) { return false; }
}, Debugging.Capture.FULL);
        engine.step();
        assertEquals(0, engine.PC);
        assertEquals(0, engine.regs[7]);
    }

    @Test
    public void haltResultSurvivesDebuggerStopAfterInstruction() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, (byte) 0x76);
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugin.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow(); return true; }

            @Override public boolean beforeStep() { return false; }
}, Debugging.Capture.FULL);

        assertEquals(net.emustudio.emulib.plugin.cpu.CPU.RunState.STATE_STOPPED_NORMAL, engine.step());
    }

    @Test
    public void continuousHaltResultSurvivesDebuggerStopAfterInstruction() {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, (byte) 0x76);
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugin.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow(); return true; }

            @Override public boolean beforeStep() { return false; }
}, Debugging.Capture.FULL);

        assertEquals(net.emustudio.emulib.plugin.cpu.CPU.RunState.STATE_STOPPED_NORMAL, engine.run());
    }

    @Test
    public void addressFailureSurvivesDebuggerStopAfterInstruction() {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN) {
            @Override public Byte read(int address) {
                if (address == 0x100) throw new IndexOutOfBoundsException("Guest read outside memory");
                return super.read(address);
            }
        };
        memory.write(0, new Byte[] {(byte) 0x3a, 0, 1}, 3);
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugin.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow(); return true; }

            @Override public boolean beforeStep() { return false; }
}, Debugging.Capture.FULL);

        assertEquals(net.emustudio.emulib.plugin.cpu.CPU.RunState.STATE_STOPPED_ADDR_FALLOUT, engine.run());
    }

    @Test
    public void accessConditionNeedsNoFullPresentationCapture() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, new Byte[] {0x32, 0x00, 0x01}, 3);
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
        engine.regs[7] = 42;
        Debugging.Expression condition = engine.debugger.compile("write(0x100)");
        List<Boolean> matches = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean beforeStep() { return false; }
            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugin.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, Optional<StepView> capture) {
                assertTrue(capture.isEmpty());
                matches.add(condition.test());
                return false;
            }
        }, Debugging.Capture.ACCESSES);

        engine.step();

        assertEquals(List.of(true), matches);
        assertEquals(42, memory.read(0x100).intValue());
    }

    @Test
    public void captureFailureDisablesCaptureWhileGuestInstructionsContinue() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, new Byte[] {0x3c, 0x3c}, 2);
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
        engine.debugger.register("BROKEN", () -> { throw new IllegalStateException("Cannot format value"); });
        List<String> failures = new ArrayList<>();
        List<Boolean> completions = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean beforeStep() { return false; }
            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugin.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, Optional<StepView> capture) {
                assertTrue(capture.isEmpty());
                completions.add(completed);
                return false;
            }
            @Override public void failed(String message) { failures.add(message); }
        }, Debugging.Capture.FULL);

        engine.step();
        engine.step();

        assertEquals(2, engine.regs[7]);
        assertEquals(List.of(true, true), completions);
        assertEquals(1, failures.size());
        assertTrue(failures.get(0).contains("Cannot format value"));
    }

    @Test
    public void adjacentNopsShareFoldIdentityWhileAccessLabelsShowTheirAddresses() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, new Byte[] {0, 0}, 2);
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
        List<StepView> events = collect(engine);

        engine.step();
        engine.step();

        assertEquals(events.get(0).foldKey, events.get(1).foldKey);
        assertEquals(0, events.get(0).programLocation.orElseThrow());
        assertEquals(1, events.get(1).programLocation.orElseThrow());
        assertTrue(events.get(0).rows.stream().anyMatch(row -> row.label.equals("Read @0000")));
        assertTrue(events.get(1).rows.stream().anyMatch(row -> row.label.equals("Read @0001")));
    }

    @Test
    public void memoryIncrementShowsBothReadAndWriteAtSameVisibleAddress() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, (byte) 0x34);
        memory.write(0x100, (byte) 2);
        EmulatorEngine engine = new EmulatorEngine(memory, new Context8080Impl());
        engine.regs[4] = 1;
        List<StepView> events = collect(engine);

        engine.step();

        assertTrue(events.get(0).rows.stream().anyMatch(row -> row.label.equals("Read @0000")));
        assertTrue(events.get(0).rows.stream().anyMatch(row -> row.label.equals("Read @0100") && row.after.equals("2")));
        assertTrue(events.get(0).rows.stream().anyMatch(row -> row.label.equals("Write @0100") && row.after.equals("3")));
        assertEquals(3, memory.read(0x100).intValue());
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
