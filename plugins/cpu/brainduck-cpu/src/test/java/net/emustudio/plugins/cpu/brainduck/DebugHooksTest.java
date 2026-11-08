/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.debugger.Debugging;
import net.emustudio.emulib.plugins.debugger.StepView;
import org.junit.Test;
import java.util.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DebugHooksTest {
    @Test public void recordingDisablesGroupingAndCapturesRawDataWrites() {
        MemoryStub memory = new MemoryStub();
        memory.setProgram(new byte[] {3, 3, 0}); // ++;
        BrainCPUContextImpl context = createNiceMock(BrainCPUContextImpl.class);
        replay(context);
        Profiler profiler = new Profiler(memory);
        EmulatorEngine engine = new EmulatorEngine(memory, context, profiler);
        engine.reset(0);
        int data = engine.P;
        assertFalse(engine.debugger.isActive());
        List<StepView> events = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow();
                events.add(event);
                return false;
            }

            @Override public boolean beforeStep() { return false; }
}, true);
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step(true));
        assertEquals(1, engine.IP); // Optimizer otherwise groups both increments.
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step(true));
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.step(true));
        assertEquals(3, events.size());
        assertEquals(0, events.get(0).programLocation.orElseThrow());
        assertEquals(1, events.get(1).programLocation.orElseThrow());
        assertFalse(events.get(0).summary.isBlank());
        assertFalse(encoding(events.get(0)).isBlank());
        assertTrue(events.get(0).rows.stream().filter(row -> row.group.equals("Memory")).anyMatch(a -> a.label.equals(String.format(Locale.ROOT, "Write @%04X", data)) && Long.parseLong(a.after) == 1));
        assertTrue(events.get(1).rows.stream().filter(row -> row.group.equals("Memory")).anyMatch(a -> a.label.equals(String.format(Locale.ROOT, "Write @%04X", data)) && Long.parseLong(a.after) == 2));
        assertEquals(2, memory.read(data).intValue());
    }

    @Test public void listenerCanStopBeforeAndAfterWithoutLosingNextInstruction() throws Exception {
        MemoryStub memory = new MemoryStub();
        memory.setProgram(new byte[] {3, 0});
        BrainCPUContextImpl context = createNiceMock(BrainCPUContextImpl.class);
        replay(context);
        EmulatorEngine engine = new EmulatorEngine(memory, context, new Profiler(memory));
        engine.reset(0);
        Debugging.Observer before = new Debugging.Observer() {
            @Override public boolean beforeStep() { return true; }

            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) { return false; }
};
        AutoCloseable observation = engine.debugger.observe(before, true);
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step(true));
        assertEquals(0, engine.IP);
        assertTrue(engine.debugStopped);
        observation.close();
        assertFalse(engine.debugger.isActive());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step(false));
        assertEquals(1, engine.IP);
        assertFalse(engine.debugStopped);
    }
    @Test public void repeatedAddressUsesCurrentCodeAfterMutationAndReset() {
        MemoryStub memory = new MemoryStub();
        memory.setProgram(new byte[] {3, 0}); // +;
        BrainCPUContextImpl context = createNiceMock(BrainCPUContextImpl.class);
        replay(context);
        EmulatorEngine engine = new EmulatorEngine(memory, context, new Profiler(memory));
        List<StepView> events = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow();
                events.add(event);
                return false;
            }

            @Override public boolean beforeStep() { return false; }
}, true);
        engine.reset(0);
        engine.step(false);
        memory.write(0, (byte) 1); // Replace + with > at same address.
        engine.reset(0);
        engine.step(false);
        assertNotEquals(events.get(0).summary, events.get(1).summary);
        assertNotEquals(encoding(events.get(0)), encoding(events.get(1)));
        assertEquals(0, events.get(1).programLocation.orElseThrow());
    }
    @Test public void selectedBankDoesNotRelabelRawBankZeroExecution() {
        MemoryStub memory = new MemoryStub() {
            { memory = new Byte[2][1000]; java.util.Arrays.fill(memory[0], (byte) 0); java.util.Arrays.fill(memory[1], (byte) 0); }
            @Override public int getBanksCount() { return 2; }
            @Override public int getSelectedBank() { return 1; }
            @Override public int getCommonBoundary() { return 1000; }
            @Override public Byte read(int address) { return memory[1][address]; }
            @Override public Byte[] read(int address, int count) {
                return java.util.Arrays.copyOfRange(memory[1], address, address + count);
            }
        };
        memory.getRawMemory()[0][0] = 3; // + executes from raw bank zero.
        memory.getRawMemory()[1][0] = 1; // > is visible through selected-bank reads.
        BrainCPUContextImpl context = createNiceMock(BrainCPUContextImpl.class);
        replay(context);
        EmulatorEngine engine = new EmulatorEngine(memory, context, new Profiler(memory));
        engine.reset(0);
        List<StepView> events = new ArrayList<>();
        List<Integer> beforeBanks = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean beforeStep() { int address = engine.IP; beforeBanks.add(address); return false; }
            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow(); events.add(event); return false; }
        }, true);
        engine.step(false);
        assertEquals(List.of(0), beforeBanks);

        assertEquals("03", encoding(events.get(0)).trim());
        assertEquals("3", engine.debugger.compile("mem[0]").display().text);
        assertEquals(1, memory.getRawMemory()[0][engine.P].intValue());
    }

    @Test
    public void haltResultSurvivesDebuggerStopAfterInstruction() {
        MemoryStub memory = new MemoryStub();
        memory.setProgram(new byte[] {0});
        BrainCPUContextImpl context = createNiceMock(BrainCPUContextImpl.class);
        replay(context);
        EmulatorEngine engine = new EmulatorEngine(memory, context, new Profiler(memory));
        engine.reset(0);
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow(); return true; }

            @Override public boolean beforeStep() { return false; }
}, true);

        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.step(false));
        assertFalse(engine.debugStopped);
    }

    @Test
    public void invalidInstructionSurvivesDebuggerStopAfterInstruction() {
        MemoryStub memory = new MemoryStub();
        memory.setProgram(new byte[] {0x7f});
        BrainCPUContextImpl context = createNiceMock(BrainCPUContextImpl.class);
        replay(context);
        EmulatorEngine engine = new EmulatorEngine(memory, context, new Profiler(memory));
        engine.reset(0);
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow(); return true; }

            @Override public boolean beforeStep() { return false; }
}, true);

        assertEquals(CPU.RunState.STATE_STOPPED_BAD_INSTR, engine.step(false));
        assertFalse(engine.debugStopped);
    }

    private static String encoding(net.emustudio.emulib.plugins.debugger.StepView event) {
        return event.rows.stream().filter(row -> row.group.equals("Instruction") && row.label.equals("Encoding")).findFirst().orElseThrow().after;
    }

}
