/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

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
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        Debugging.Observer listener = new Debugging.Observer() {
            @Override public boolean beforeStep() { int address = engine.PC; return address == 2; }

            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) { return false; }
};
        AutoCloseable observation = engine.debugger.observe(listener, true);

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
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        java.lang.reflect.Field field = EmulatorEngine.class.getDeclaredField("memory");
        field.setAccessible(true);
        assertSame(memory, field.get(engine));
        Debugging.Observer listener = new Debugging.Observer() {
            @Override public boolean beforeStep() { return false; }

            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) { return false; }
};
        AutoCloseable observation = engine.debugger.observe(listener, true);
        assertNotSame(memory, field.get(engine));
        observation.close();
        assertSame(memory, field.get(engine));
        assertFalse(engine.debugger.isActive());
    }

    @Test
    public void realInstructionsReportGuestWritesAndRegisterChanges() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, new Byte[] {0x3e, 0x42, 0x32, 0x00, 0x01}, 5);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        List<StepView> events = new ArrayList<>();
        engine.debugger.observe(
                new Debugging.Observer() {
                    @Override
                    public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView e = capture.orElseThrow();
                        events.add(e);
                        return false;
                    }

            @Override public boolean beforeStep() { return false; }
}, true);
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
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
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
    public void indexedPrefixCapturesWholeOpcodeAndAlternateRegisters() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, new Byte[] {(byte) 0xdd, 0x21, 0x34, 0x12}, 4);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.regs2[7] = 42;
        engine.flags2 = 0x28;
        List<StepView> events = collect(engine);
        engine.step();
        assertEquals(1, events.size());
        assertEquals("DD 21 34 12", encoding(events.get(0)).trim());
        assertEquals(Long.valueOf(0x1234), state(events.get(0), "IX", false));
        assertEquals(Long.valueOf(42), state(events.get(0), "A2", false));
        assertEquals(Long.valueOf(0x28), state(events.get(0), "F2", false));
    }

    @Test
    public void interruptRecordsAcknowledgementAndActualIsrInstruction() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, (byte) 0x3c);
        memory.write(0x38, new Byte[] {0x3e, 0x42}, 2);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.SP = 0x100;
        engine.interruptMode = 1;
        engine.IFF[0] = true;
        requestInterrupt(engine, new byte[] {(byte) 0xff});
        List<StepView> events = collect(engine);
        engine.step();
        assertEquals(1, events.size());
        assertTrue(events.get(0).summary.startsWith("INT acknowledge; "));
        assertEquals("3E 42", encoding(events.get(0)).trim());
        assertEquals(0x38, events.get(0).programLocation.orElseThrow());
        assertEquals(Long.valueOf(0), state(events.get(0), "PC", true));
        assertEquals(Long.valueOf(0x3a), state(events.get(0), "PC", false));
        assertEquals(Long.valueOf(0x42), state(events.get(0), "A", false));
        engine.requestNonMaskableInterrupt();
        engine.step();
        assertEquals("NMI acknowledge", events.get(1).summary);
        assertTrue(events.get(1).programLocation.isEmpty());
        assertEquals("", encoding(events.get(1)));
        assertEquals(Long.valueOf(0x66), state(events.get(1), "PC", false));
    }

    @Test
    public void im2RecordingDoesNotAddMemoryContentionOrGuestReads() throws Exception {
        long[] ordinary = executeContendedIm2(false);
        long[] recorded = executeContendedIm2(true);
        assertEquals(ordinary[0], recorded[0]);
        assertEquals(ordinary[1], recorded[1]);
        assertTrue(ordinary[0] > 0);
    }

    private static long[] executeContendedIm2(boolean record) throws Exception {
        ContendedMemory memory = new ContendedMemory();
        memory.write(0, (byte) 0);
        memory.write(0x100, new Byte[] {0x00, 0x02}, 2);
        memory.write(0x200, new Byte[] {0x3e, 0x42}, 2);
        ContextZ80Impl context = new ContextZ80Impl();
        long[] cycles = {0};
        context.addPassedCyclesListener(delta -> cycles[0] += delta);
        EmulatorEngine engine = new EmulatorEngine(memory, context);
        memory.engine = engine;
        engine.SP = 0x300;
        engine.I = 1;
        engine.IFF[0] = true;
        engine.interruptMode = 2;
        List<StepView> events = record ? collect(engine) : List.of();
        requestInterrupt(engine, new byte[] {0});
        engine.step();
        assertEquals(0x202, engine.PC);
        assertEquals(0x42, engine.regs[7]);
        if (record) {
            assertEquals(1, events.size());
            assertEquals(0x200, events.get(0).programLocation.orElseThrow());

            assertEquals("3E 42", encoding(events.get(0)).trim());
            assertEquals(Long.valueOf(0), state(events.get(0), "PC", true));
        }
        return new long[] {cycles[0], memory.guestReads};
    }

    private static void requestInterrupt(EmulatorEngine engine, byte[] data) throws Exception {
        java.lang.reflect.Field state = EmulatorEngine.class.getDeclaredField("currentRunState");
        state.setAccessible(true);
        state.set(engine, net.emustudio.emulib.plugin.cpu.CPU.RunState.STATE_RUNNING);
        engine.requestMaskableInterrupt(data);
    }

    /** Like the Spectrum bus, guest reads add cycles and inspection reads bypass contention. */
    private static final class ContendedMemory extends ByteMemoryStub {
        EmulatorEngine engine;
        int guestReads;
        ContendedMemory() { super(Numbers.Strategy.LITTLE_ENDIAN); }
        @Override public Byte peek(int at) { return super.read(at); }
        @Override public Byte read(int at) {
            guestReads++;
            if (engine != null) engine.addExecutedCyclesPerTimeSlice(2);
            return super.read(at);
        }
        @Override public Byte[] read(int at, int count) {
            Byte[] values = new Byte[count];
            for (int i = 0; i < count; i++) values[i] = read(at + i);
            return values;
        }
    }

    private static List<StepView> collect(EmulatorEngine engine) {
        List<StepView> events = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow();
                events.add(event);
                return false;
            }

            @Override public boolean beforeStep() { return false; }
}, true);
        return events;
    }

    @Test
    public void canStopBeforeInstructionWithoutMutatingRegisters() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, (byte) 0x3c);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.debugger.observe(
                new Debugging.Observer() {
                    @Override
                    public boolean beforeStep() {
                        return true;
                    }

            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) { return false; }
}, true);
        engine.step();
        assertEquals(0, engine.PC);
        assertEquals(0, engine.regs[7]);
    }

    @Test
    public void haltResultSurvivesDebuggerStopAfterInstruction() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, (byte) 0x76);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow(); return true; }

            @Override public boolean beforeStep() { return false; }
}, true);

        assertEquals(net.emustudio.emulib.plugin.cpu.CPU.RunState.STATE_STOPPED_NORMAL, engine.step());
    }

    @Test
    public void continuousHaltResultSurvivesDebuggerStopAfterInstruction() {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);
        memory.write(0, (byte) 0x76);
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow(); return true; }

            @Override public boolean beforeStep() { return false; }
}, true);

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
        EmulatorEngine engine = new EmulatorEngine(memory, new ContextZ80Impl());
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow(); return true; }

            @Override public boolean beforeStep() { return false; }
}, true);

        assertEquals(net.emustudio.emulib.plugin.cpu.CPU.RunState.STATE_STOPPED_ADDR_FALLOUT, engine.run());
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
