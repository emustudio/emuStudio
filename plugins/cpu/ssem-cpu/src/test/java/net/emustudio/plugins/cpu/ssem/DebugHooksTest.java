/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.debugger.Debugging;
import net.emustudio.emulib.plugin.debugger.StepView;
import net.emustudio.emulib.Numbers;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class DebugHooksTest {
    @Test
    public void runChecksApplicationBreakpointAtNextInstructionAddress() {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.REVERSE_BITS);
        memory.setMemory(new short[128]);
        memory.write(1, (byte) 3);
        memory.write(5, (byte) 3);
        EmulatorEngine engine = new EmulatorEngine(memory);
        engine.reset(-4);
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean beforeStep() { int address = engine.CI.get() + 4; return address == 8; }

            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) { return false; }
}, true);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.run());

        assertEquals(4, engine.CI.get());
        assertEquals(0, engine.Acc.get());
    }

    @Test public void recordsActualFetchAddressAndRegistersDuringRun() {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.REVERSE_BITS);
        memory.setMemory(new short[128]);
        memory.write(1, (byte) 3); // CMP skips the following instruction when ACC is negative.
        memory.write(9, (byte) 7); // STP at byte address 8.
        EmulatorEngine engine = new EmulatorEngine(memory);
        engine.reset(-4);
        engine.Acc.set(-1);
        List<StepView> events = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow();
                events.add(event);
                return false;
            }

            @Override public boolean beforeStep() { return false; }
}, true);
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.run());
        assertEquals(2, events.size());
        assertEquals(0, events.get(0).programLocation.orElseThrow());
        assertEquals(8, events.get(1).programLocation.orElseThrow());
        assertTrue(events.get(0).summary.toLowerCase().contains("cmp"));
        assertFalse(encoding(events.get(0)).isBlank());
        assertEquals(Long.valueOf(0xffffffffL), state(events.get(0), "ACC", true));
        assertEquals(Long.valueOf(8), state(events.get(0), "PC", false));
        assertTrue(events.get(0).rows.stream().filter(row -> row.group.equals("Memory")).anyMatch(a -> !a.label.equals("Write @0000")));
    }

    @Test public void stopBeforeInstructionLeavesCiAndAccumulatorUntouched() {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.REVERSE_BITS);
        memory.setMemory(new short[128]);
        EmulatorEngine engine = new EmulatorEngine(memory);
        engine.reset(-4);
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean beforeStep() { return true; }

            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) { return false; }
}, true);
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(-4, engine.CI.get());
        assertEquals(0, engine.Acc.get());
    }
    @Test public void repeatedAddressUsesCurrentCodeAfterMutationAndReset() {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.REVERSE_BITS);
        memory.setMemory(new short[128]);
        memory.write(1, (byte) 3); // CMP.
        EmulatorEngine engine = new EmulatorEngine(memory);
        List<StepView> events = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow();
                events.add(event);
                return false;
            }

            @Override public boolean beforeStep() { return false; }
}, true);
        engine.reset(-4);
        engine.step();
        memory.write(1, (byte) 7); // Replace same address with STP.
        engine.reset(-4);
        engine.step();
        assertTrue(events.get(0).summary.toLowerCase().contains("cmp"));
        assertTrue(events.get(1).summary.toLowerCase().contains("stp"));
        assertNotEquals(encoding(events.get(0)), encoding(events.get(1)));
    }

    @Test
    public void haltResultSurvivesDebuggerStopAfterInstruction() {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.REVERSE_BITS);
        memory.setMemory(new short[128]);
        memory.write(1, (byte) 7);
        EmulatorEngine engine = new EmulatorEngine(memory);
        engine.reset(-4);
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow(); return true; }

            @Override public boolean beforeStep() { return false; }
}, true);

        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.run());
        assertEquals(0, engine.CI.get());
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
