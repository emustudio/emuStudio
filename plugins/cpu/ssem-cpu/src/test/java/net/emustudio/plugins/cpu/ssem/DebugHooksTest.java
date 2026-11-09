/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.debugger.Debugging;
import net.emustudio.emulib.plugins.debugger.StepView;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class DebugHooksTest {
    @Test
    public void lightObservationReportsActualConditionalSkipWithoutFullCapture() {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[128]);
        memory.write(1, (byte) 3);
        EmulatorEngine engine = new EmulatorEngine(memory);
        List<Integer> nextAddresses = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean beforeStep() { return false; }
            @Override public boolean afterStep(boolean completed,
                    Optional<net.emustudio.emulib.plugins.cpu.DecodedInstruction> instruction,
                    OptionalInt nextAddress, Optional<StepView> capture) {
                assertTrue(completed);
                assertEquals(0, instruction.orElseThrow().address());
                assertEquals(4, instruction.orElseThrow().length());
                assertTrue(capture.isEmpty());
                nextAddresses.add(nextAddress.orElseThrow());
                return false;
            }
        }, Debugging.Capture.INSTRUCTION);
        engine.reset(-4);
        engine.Acc.set(-1);

        engine.step();
        engine.reset(-4);
        engine.step();

        assertEquals(List.of(8, 4), nextAddresses);
    }

    @Test
    public void runChecksApplicationBreakpointAtNextInstructionAddress() {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[128]);
        memory.write(1, (byte) 3);
        memory.write(5, (byte) 3);
        EmulatorEngine engine = new EmulatorEngine(memory);
        engine.reset(-4);
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean beforeStep() { int address = engine.CI.get() + 4; return address == 8; }

            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugins.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) { return false; }
}, Debugging.Capture.FULL);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.run());

        assertEquals(4, engine.CI.get());
        assertEquals(0, engine.Acc.get());
    }

    @Test public void recordsActualFetchAddressAndRegistersDuringRun() {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[128]);
        memory.write(1, (byte) 3); // CMP skips the following instruction when ACC is negative.
        memory.write(9, (byte) 7); // STP at byte address 8.
        EmulatorEngine engine = new EmulatorEngine(memory);
        engine.reset(-4);
        engine.Acc.set(-1);
        List<StepView> events = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugins.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow();
                events.add(event);
                return false;
            }

            @Override public boolean beforeStep() { return false; }
}, Debugging.Capture.FULL);
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
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[128]);
        EmulatorEngine engine = new EmulatorEngine(memory);
        engine.reset(-4);
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean beforeStep() { return true; }

            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugins.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) { return false; }
}, Debugging.Capture.FULL);
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(-4, engine.CI.get());
        assertEquals(0, engine.Acc.get());
    }
    @Test public void repeatedAddressUsesCurrentCodeAfterMutationAndReset() {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[128]);
        memory.write(1, (byte) 3); // CMP.
        EmulatorEngine engine = new EmulatorEngine(memory);
        List<StepView> events = new ArrayList<>();
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugins.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow();
                events.add(event);
                return false;
            }

            @Override public boolean beforeStep() { return false; }
}, Debugging.Capture.FULL);
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
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[128]);
        memory.write(1, (byte) 7);
        EmulatorEngine engine = new EmulatorEngine(memory);
        engine.reset(-4);
        engine.debugger.observe(new Debugging.Observer() {
            @Override public boolean afterStep(boolean completed, java.util.Optional<net.emustudio.emulib.plugins.cpu.DecodedInstruction> instruction, java.util.OptionalInt nextAddress, java.util.Optional<StepView> capture) {
                StepView event = capture.orElseThrow(); return true; }

            @Override public boolean beforeStep() { return false; }
}, Debugging.Capture.FULL);

        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.run());
        assertEquals(0, engine.CI.get());
    }

    private static String encoding(net.emustudio.emulib.plugins.debugger.StepView event) {
        return event.rows.stream().filter(row -> row.group.equals("Instruction") && row.label.equals("Encoding")).findFirst().orElseThrow().after;
    }

    private static Long state(net.emustudio.emulib.plugins.debugger.StepView event, String name, boolean before) {
        net.emustudio.emulib.plugins.debugger.ViewRow row = event.rows.stream()
                .filter(value -> value.group.equals("State") && value.label.equals(name)).findFirst().orElseThrow();
        return Long.valueOf(before ? row.before : row.after);
    }
}
